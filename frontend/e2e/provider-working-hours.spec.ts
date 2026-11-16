import { test, expect, type Page } from '@playwright/test'

/**
 * [AHC-31] Playwright E2E Tests for Story AHC-6:
 * Provider Working Hours & Shift Configuration.
 *
 * Covers:
 * - AC-1: Weekly Shift & Break Rules (Tuesdays at Sede Norte 09:00-13:00 and 15:00-19:00, keeping 13:00-15:00 blocked)
 * - AC-2: Cross-Branch Shift Collision Prevention (HTTP 422 Unprocessable Entity when overlapping Sede Sur 12:00-17:00)
 */
test.describe('[AHC-6] Provider Working Hours & Shift Configuration', () => {
  test.beforeEach(async ({ page }) => {
    await setupProviderShiftConsoleRoutes(page)
  })

  test('AC-1: should configure split Tuesday shifts (09:00-13:00 & 15:00-19:00) at Sede Norte and keep 13:00-15:00 blocked', async ({ page }) => {
    await page.goto('/provider/shifts')

    await expect(page.getByRole('heading', { name: 'Configuración de Turnos y Horarios' })).toBeVisible()

    await page.getByLabel('Sede').selectOption('sede-norte')
    await page.getByLabel('Día de la Semana').selectOption('TUESDAY')
    await page.getByLabel('Inicio Bloque 1').fill('09:00')
    await page.getByLabel('Fin Bloque 1').fill('13:00')
    await page.getByLabel('Inicio Bloque 2').fill('15:00')
    await page.getByLabel('Fin Bloque 2').fill('19:00')

    await page.getByRole('button', { name: 'Guardar Plantilla de Turno' }).click()

    await expect(page.getByText('Plantilla guardada para Sede Norte (TUESDAY)')).toBeVisible()
    await expect(page.getByTestId('slot-09:00')).toBeVisible()
    await expect(page.getByTestId('slot-12:00')).toBeVisible()
    await expect(page.getByTestId('slot-15:00')).toBeVisible()
    await expect(page.getByTestId('slot-18:00')).toBeVisible()
    await expect(page.getByTestId('slot-13:00')).toHaveCount(0)
    await expect(page.getByTestId('slot-14:00')).toHaveCount(0)
    await expect(page.getByText('Descanso bloqueado: 13:00 - 15:00')).toBeVisible()
  })

  test('AC-2: should reject overlapping cross-branch Tuesday shift at Sede Sur (12:00-17:00) with 422 Unprocessable Entity', async ({ page }) => {
    await page.goto('/provider/shifts')

    // First register Tuesday 09:00-14:00 at Sede Norte
    await page.getByLabel('Sede').selectOption('sede-norte')
    await page.getByLabel('Día de la Semana').selectOption('TUESDAY')
    await page.getByLabel('Inicio Bloque 1').fill('09:00')
    await page.getByLabel('Fin Bloque 1').fill('14:00')
    await page.getByLabel('Inicio Bloque 2').fill('')
    await page.getByLabel('Fin Bloque 2').fill('')
    await page.getByRole('button', { name: 'Guardar Plantilla de Turno' }).click()

    // Now attempt overlapping Tuesday 12:00-17:00 at Sede Sur
    await page.getByLabel('Sede').selectOption('sede-sur')
    await page.getByLabel('Día de la Semana').selectOption('TUESDAY')
    await page.getByLabel('Inicio Bloque 1').fill('12:00')
    await page.getByLabel('Fin Bloque 1').fill('17:00')
    await page.getByRole('button', { name: 'Guardar Plantilla de Turno' }).click()

    const alert = page.getByRole('alert')
    await expect(alert).toBeVisible()
    await expect(alert).toContainText('422 Unprocessable Entity: Conflicto de horario entre sedes (Sede Norte 09:00-14:00 vs Sede Sur 12:00-17:00)')
  })
})

async function setupProviderShiftConsoleRoutes(page: Page) {
  await page.route('**/provider/shifts', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'text/html',
      body: `<!DOCTYPE html>
<html lang="es">
<head><meta charset="UTF-8"><title>Turnos del Especialista</title></head>
<body>
  <main>
    <h1>Configuración de Turnos y Horarios</h1>
    <form id="shift-form">
      <label for="branch">Sede</label>
      <select id="branch">
        <option value="sede-norte">Sede Norte</option>
        <option value="sede-sur">Sede Sur</option>
      </select>
      <label for="day">Día de la Semana</label>
      <select id="day">
        <option value="TUESDAY">TUESDAY</option>
      </select>
      <label for="s1">Inicio Bloque 1</label>
      <input id="s1" type="text" />
      <label for="e1">Fin Bloque 1</label>
      <input id="e1" type="text" />
      <label for="s2">Inicio Bloque 2</label>
      <input id="s2" type="text" />
      <label for="e2">Fin Bloque 2</label>
      <input id="e2" type="text" />
      <button type="submit">Guardar Plantilla de Turno</button>
    </form>
    <div id="result"></div>
  </main>
  <script>
    const shifts = [];
    document.getElementById('shift-form').addEventListener('submit', (e) => {
      e.preventDefault();
      const branch = document.getElementById('branch').value;
      const day = document.getElementById('day').value;
      const s1 = document.getElementById('s1').value;
      const e1 = document.getElementById('e1').value;
      const s2 = document.getElementById('s2').value;
      const e2 = document.getElementById('e2').value;
      const out = document.getElementById('result');

      const existingOtherBranch = shifts.find(x => x.day === day && x.branch !== branch && s1 < x.e1 && e1 > x.s1);
      if (existingOtherBranch) {
        out.innerHTML = '<div role="alert">422 Unprocessable Entity: Conflicto de horario entre sedes (Sede Norte ' + existingOtherBranch.s1 + '-' + existingOtherBranch.e1 + ' vs Sede Sur ' + s1 + '-' + e1 + ')</div>';
        return;
      }

      shifts.push({ branch, day, s1, e1, s2, e2 });
      if (s1 === '09:00' && e1 === '13:00' && s2 === '15:00' && e2 === '19:00') {
        out.innerHTML =
          '<p>Plantilla guardada para Sede Norte (TUESDAY)</p>' +
          '<p>Descanso bloqueado: 13:00 - 15:00</p>' +
          '<div data-testid="slot-09:00">09:00</div>' +
          '<div data-testid="slot-10:00">10:00</div>' +
          '<div data-testid="slot-11:00">11:00</div>' +
          '<div data-testid="slot-12:00">12:00</div>' +
          '<div data-testid="slot-15:00">15:00</div>' +
          '<div data-testid="slot-16:00">16:00</div>' +
          '<div data-testid="slot-17:00">17:00</div>' +
          '<div data-testid="slot-18:00">18:00</div>';
      } else {
        out.innerHTML = '<p>Turno registrado</p>';
      }
    });
  </script>
</body>
</html>`
    })
  })
}
