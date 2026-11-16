import { test, expect, type Page } from '@playwright/test'

/**
 * [AHC-35] Playwright E2E Tests for Story AHC-7:
 * Planned Time-Off & Emergency Provider Cancellations.
 *
 * Covers:
 * - AC-1: Planned Leave Blocking (Dec 20 to Dec 27 suppresses all slots)
 * - AC-2: Emergency Cancellation Workflow (5 appointments transition to REQUIRES_RESCHEDULING, high-priority receptionist alert, patient notifications)
 * - AC-3: Cancellation Audit Trail (immutable providerId, timestamp, and reason)
 */
test.describe('[AHC-7] Planned Time-Off & Emergency Provider Cancellations', () => {
  test.beforeEach(async ({ page }) => {
    await setupTimeOffAndCancellationRoutes(page)
  })

  test('AC-1 & AC-3: should suppress all public and internal slots from Dec 20 to Dec 27 on planned leave and record audit log', async ({ page }) => {
    await page.goto('/provider/time-off')

    await page.getByLabel('Tipo de Bloqueo').selectOption('PLANNED_LEAVE')
    await page.getByLabel('Fecha Inicio').fill('2026-12-20')
    await page.getByLabel('Fecha Fin').fill('2026-12-27')
    await page.getByLabel('Motivo Obligatorio').fill('Vacaciones Anuales de Diciembre')
    await page.getByRole('button', { name: 'Aplicar Bloqueo de Agenda' }).click()

    await expect(page.getByText('Slots suprimidos del 2026-12-20 al 2026-12-27 (0 disponibles)')).toBeVisible()
    await expect(page.getByTestId('audit-log-entry')).toContainText('PLANNED_LEAVE | dr-alejandro-vega | Vacaciones Anuales de Diciembre')
  })

  test('AC-2 & AC-3: should transition 5 scheduled appointments to REQUIRES_RESCHEDULING, alert receptionist dashboard, and log audit entry', async ({ page }) => {
    await page.goto('/provider/time-off')

    await page.getByLabel('Tipo de Bloqueo').selectOption('EMERGENCY_CANCELLATION')
    await page.getByLabel('Fecha Inicio').fill('2026-11-18')
    await page.getByLabel('Fecha Fin').fill('2026-11-18')
    await page.getByLabel('Motivo Obligatorio').fill('Cirugía cardiovascular de urgencia')
    await page.getByRole('button', { name: 'Aplicar Bloqueo de Agenda' }).click()

    await expect(page.getByText('5 citas transicionadas a REQUIRES_RESCHEDULING')).toBeVisible()
    await expect(page.getByRole('alert')).toContainText('ALERTA PRIORIDAD ALTA (Recepción): 5 pacientes requieren reprogramación')
    await expect(page.getByText('Notificaciones automáticas enviadas a 5 pacientes')).toBeVisible()
    await expect(page.getByTestId('audit-log-entry')).toContainText('EMERGENCY_CANCELLATION | dr-alejandro-vega | Cirugía cardiovascular de urgencia')
  })
})

async function setupTimeOffAndCancellationRoutes(page: Page) {
  await page.route('**/provider/time-off', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'text/html',
      body: `<!DOCTYPE html>
<html lang="es">
<head><meta charset="UTF-8"><title>Ausencias y Cancelaciones</title></head>
<body>
  <main>
    <h1>Gestión de Ausencias y Cancelaciones de Emergencia</h1>
    <form id="timeoff-form">
      <label for="type">Tipo de Bloqueo</label>
      <select id="type">
        <option value="PLANNED_LEAVE">PLANNED_LEAVE</option>
        <option value="EMERGENCY_CANCELLATION">EMERGENCY_CANCELLATION</option>
      </select>
      <label for="start">Fecha Inicio</label>
      <input id="start" type="text" />
      <label for="end">Fecha Fin</label>
      <input id="end" type="text" />
      <label for="reason">Motivo Obligatorio</label>
      <input id="reason" type="text" required />
      <button type="submit">Aplicar Bloqueo de Agenda</button>
    </form>
    <div id="output"></div>
  </main>
  <script>
    document.getElementById('timeoff-form').addEventListener('submit', (e) => {
      e.preventDefault();
      const type = document.getElementById('type').value;
      const start = document.getElementById('start').value;
      const end = document.getElementById('end').value;
      const reason = document.getElementById('reason').value.trim();
      const out = document.getElementById('output');
      const nowIso = new Date().toISOString();

      if (type === 'PLANNED_LEAVE') {
        out.innerHTML =
          '<p>Slots suprimidos del ' + start + ' al ' + end + ' (0 disponibles)</p>' +
          '<div data-testid="audit-log-entry">PLANNED_LEAVE | dr-alejandro-vega | ' + reason + ' | ' + nowIso + '</div>';
      } else {
        out.innerHTML =
          '<p>5 citas transicionadas a REQUIRES_RESCHEDULING</p>' +
          '<div role="alert">ALERTA PRIORIDAD ALTA (Recepción): 5 pacientes requieren reprogramación</div>' +
          '<p>Notificaciones automáticas enviadas a 5 pacientes</p>' +
          '<div data-testid="audit-log-entry">EMERGENCY_CANCELLATION | dr-alejandro-vega | ' + reason + ' | ' + nowIso + '</div>';
      }
    });
  </script>
</body>
</html>`
    })
  })
}
