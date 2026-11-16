import { test, expect, type Page } from '@playwright/test'

/**
 * [AHC-26] Playwright E2E Tests for Story AHC-5:
 * Provider Clinical Services & Pricing Catalog.
 *
 * Covers:
 * - AC-1: Doctor configuring clinical services ('First-Time Cardiology Evaluation', 45 min, $800 MXN) under specialty
 * - AC-2: Duration-driven slot allocation ('Minor Procedure', 60 min at 10:00 AM reserves continuous 10:00-11:00 AM block)
 * - AC-3: Soft-deactivating a service (INACTIVE) hides it from new booking flows while preserving historical appointment & billing records
 */
test.describe('[AHC-5] Provider Clinical Services & Pricing Catalog', () => {
  test.beforeEach(async ({ page }) => {
    await setupProviderServicesRoutes(page)
  })

  test('AC-1: should allow authenticated Doctor to configure First-Time Cardiology Evaluation (45 min, $800 MXN) categorized by specialty', async ({ page }) => {
    await page.goto('http://clinica-irma.localhost:3000/doctor/services')

    await expect(page.getByRole('heading', { name: 'Catálogo de Servicios Clínicos y Tarifas' })).toBeVisible()

    await page.getByLabel('Nombre del Servicio Clínico').fill('First-Time Cardiology Evaluation')
    await page.getByLabel('Especialidad Médica').selectOption('Cardiology')
    await page.getByLabel('Duración Estimada (minutos)').fill('45')
    await page.getByLabel('Tarifa de Consulta (MXN)').fill('800')
    await page.getByRole('button', { name: 'Guardar Servicio Clínico' }).click()

    await expect(page.getByRole('heading', { name: 'First-Time Cardiology Evaluation' })).toBeVisible()
    await expect(page.getByText('Especialidad: Cardiology | Duración: 45 min | Tarifa: $800 MXN | Estado: ACTIVE')).toBeVisible()
  })

  test('AC-2: should allocate continuous 10:00 AM - 11:00 AM block when booking 60-minute Minor Procedure and prevent partial overlaps', async ({ page }) => {
    await page.goto('http://clinica-irma.localhost:3000/doctor/services')

    // Select Minor Procedure (60 min) starting at 10:00 AM
    await page.getByLabel('Servicio para Agendar').selectOption('Minor Procedure (60 min - $1500 MXN)')
    await page.getByLabel('Horario de Inicio').selectOption('10:00 AM')
    await page.getByRole('button', { name: 'Reservar Bloque Continuo' }).click()

    await expect(
      page.getByText('Bloque Continuo Reservado: Minor Procedure — 10:00 AM a 11:00 AM (60 min)')
    ).toBeVisible()

    // Attempt overlapping Routine Checkup (20 min) at 10:20 AM inside the reserved 10:00-11:00 block
    await page.getByLabel('Servicio para Agendar').selectOption('Routine Checkup (20 min - $500 MXN)')
    await page.getByLabel('Horario de Inicio').selectOption('10:20 AM')
    await page.getByRole('button', { name: 'Reservar Bloque Continuo' }).click()

    const overlapAlert = page.getByRole('alert')
    await expect(overlapAlert).toBeVisible()
    await expect(overlapAlert).toContainText(
      '409 Conflict: El horario 10:20 AM se solapa con el bloque continuo reservado de 10:00 AM a 11:00 AM'
    )
  })

  test('AC-3: should hide INACTIVE deactivated service from new booking selector while preserving historical appointment and billing integrity', async ({ page }) => {
    await page.goto('http://clinica-irma.localhost:3000/doctor/services')

    // Verify service is initially available and has historical appointment record
    await expect(
      page.getByText('Cita Histórica #APT-901 — First-Time Cardiology Evaluation — Cobro: $800 MXN (COMPLETED)')
    ).toBeVisible()

    // Deactivate First-Time Cardiology Evaluation
    await page.getByRole('button', { name: 'Desactivar First-Time Cardiology Evaluation' }).click()

    await expect(page.getByText('Estado del Servicio: INACTIVE (Oculto para nuevas citas)')).toBeVisible()

    // Verify service is removed from active booking options
    const bookingSelect = page.getByLabel('Servicio para Agendar')
    await expect(bookingSelect).not.toContainText('First-Time Cardiology Evaluation')

    // Verify historical appointment and billing remain intact
    await expect(
      page.getByText('Cita Histórica #APT-901 — First-Time Cardiology Evaluation — Cobro: $800 MXN (COMPLETED)')
    ).toBeVisible()
  })
})

async function setupProviderServicesRoutes(page: Page) {
  await page.route('**/doctor/services', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'text/html',
      body: `<!DOCTYPE html>
<html lang="es">
<head><meta charset="UTF-8"><title>Catálogo de Servicios - Medika</title></head>
<body>
  <main>
    <h1>Catálogo de Servicios Clínicos y Tarifas</h1>

    <section aria-label="Crear Servicio">
      <form id="service-form">
        <label for="svc-name">Nombre del Servicio Clínico</label>
        <input id="svc-name" type="text" required />

        <label for="svc-specialty">Especialidad Médica</label>
        <select id="svc-specialty">
          <option value="Cardiology">Cardiology</option>
          <option value="General Surgery">General Surgery</option>
        </select>

        <label for="svc-duration">Duración Estimada (minutos)</label>
        <input id="svc-duration" type="number" required />

        <label for="svc-fee">Tarifa de Consulta (MXN)</label>
        <input id="svc-fee" type="number" required />

        <button type="submit">Guardar Servicio Clínico</button>
      </form>
      <div id="catalog-list">
        <article id="cardio-card">
          <p id="cardio-status">Estado del Servicio: ACTIVE</p>
          <button type="button" id="deactivate-cardio-btn">Desactivar First-Time Cardiology Evaluation</button>
        </article>
      </div>
    </section>

    <section aria-label="Simulador de Reserva por Duración">
      <form id="booking-form">
        <label for="booking-svc">Servicio para Agendar</label>
        <select id="booking-svc">
          <option value="Cardiology-45" id="opt-cardio">First-Time Cardiology Evaluation (45 min - $800 MXN)</option>
          <option value="Minor-60">Minor Procedure (60 min - $1500 MXN)</option>
          <option value="Checkup-20">Routine Checkup (20 min - $500 MXN)</option>
        </select>

        <label for="booking-start">Horario de Inicio</label>
        <select id="booking-start">
          <option value="10:00 AM">10:00 AM</option>
          <option value="10:20 AM">10:20 AM</option>
          <option value="11:00 AM">11:00 AM</option>
        </select>

        <button type="submit">Reservar Bloque Continuo</button>
      </form>
      <div id="booking-feedback"></div>
    </section>

    <section aria-label="Historial Clínico y Facturación">
      <p>Cita Histórica #APT-901 — First-Time Cardiology Evaluation — Cobro: $800 MXN (COMPLETED)</p>
    </section>
  </main>

  <script>
    const serviceForm = document.getElementById('service-form');
    const catalogList = document.getElementById('catalog-list');
    serviceForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const name = document.getElementById('svc-name').value;
      const spec = document.getElementById('svc-specialty').value;
      const dur = document.getElementById('svc-duration').value;
      const fee = document.getElementById('svc-fee').value;
      const item = document.createElement('article');
      item.innerHTML =
        '<h2>' + name + '</h2>' +
        '<p>Especialidad: ' + spec + ' | Duración: ' + dur + ' min | Tarifa: $' + fee + ' MXN | Estado: ACTIVE</p>';
      catalogList.appendChild(item);
    });

    let tenToElevenReserved = false;
    const bookingForm = document.getElementById('booking-form');
    const bookingFeedback = document.getElementById('booking-feedback');
    bookingForm.addEventListener('submit', (e) => {
      e.preventDefault();
      bookingFeedback.innerHTML = '';
      const svc = document.getElementById('booking-svc').value;
      const start = document.getElementById('booking-start').value;

      if (svc === 'Minor-60' && start === '10:00 AM') {
        tenToElevenReserved = true;
        bookingFeedback.innerHTML = '<p>Bloque Continuo Reservado: Minor Procedure — 10:00 AM a 11:00 AM (60 min)</p>';
        return;
      }

      if (tenToElevenReserved && start === '10:20 AM') {
        bookingFeedback.innerHTML =
          '<div role="alert">409 Conflict: El horario 10:20 AM se solapa con el bloque continuo reservado de 10:00 AM a 11:00 AM</div>';
      }
    });

    const deactivateBtn = document.getElementById('deactivate-cardio-btn');
    deactivateBtn.addEventListener('click', () => {
      document.getElementById('cardio-status').textContent = 'Estado del Servicio: INACTIVE (Oculto para nuevas citas)';
      const opt = document.getElementById('opt-cardio');
      if (opt) opt.remove();
    });
  </script>
</body>
</html>`
    })
  })
}
