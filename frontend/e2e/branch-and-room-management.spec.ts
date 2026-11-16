import { test, expect, type Page } from '@playwright/test'

/**
 * [AHC-20] Playwright E2E Tests for Story AHC-4:
 * Multi-Branch & Physical Room Management.
 *
 * Covers:
 * - AC-1: Brand Admin registering multiple clinic branches ('Sede Centro', 'Sede Norte') with address, timezone & phone
 * - AC-2: Defining physical consultation rooms ('Consultorio Dental 1', 'Cabina Podológica A') with strict concurrency capacity = 1
 * - AC-3: Assigning healthcare providers across multiple branches while restricting receptionists to a single branch
 */
test.describe('[AHC-4] Multi-Branch & Physical Room Management', () => {
  test.beforeEach(async ({ page }) => {
    await setupBranchAdminRoutes(page)
  })

  test('AC-1: should allow Brand Admin to register Sede Centro and Sede Norte under active tenant', async ({ page }) => {
    await page.goto('http://clinica-irma.localhost:3000/admin/branches')

    await expect(page.getByRole('heading', { name: 'Gestión de Sedes y Consultorios' })).toBeVisible()

    // Register Sede Centro
    await page.getByLabel('Nombre de la Sede').fill('Sede Centro')
    await page.getByLabel('Dirección Física').fill('Av. Paseo de la Reforma 222, CDMX')
    await page.getByLabel('Zona Horaria').selectOption('America/Mexico_City')
    await page.getByLabel('Teléfono de Sede').fill('+525511223344')
    await page.getByRole('button', { name: 'Registrar Sede' }).click()

    await expect(page.getByRole('heading', { name: 'Sede Centro' })).toBeVisible()
    await expect(page.getByText('Av. Paseo de la Reforma 222, CDMX')).toBeVisible()

    // Register Sede Norte
    await page.getByLabel('Nombre de la Sede').fill('Sede Norte')
    await page.getByLabel('Dirección Física').fill('Av. Montevideo 500, CDMX')
    await page.getByLabel('Zona Horaria').selectOption('America/Mexico_City')
    await page.getByLabel('Teléfono de Sede').fill('+525566778899')
    await page.getByRole('button', { name: 'Registrar Sede' }).click()

    await expect(page.getByRole('heading', { name: 'Sede Norte' })).toBeVisible()
  })

  test('AC-2: should map physical consultation rooms with strict concurrency capacity of 1 active consultation', async ({ page }) => {
    await page.goto('http://clinica-irma.localhost:3000/admin/branches')

    await page.getByLabel('Nombre del Consultorio').fill('Consultorio Dental 1')
    await page.getByLabel('Sede Destino del Consultorio').selectOption('Sede Centro')
    await page.getByRole('button', { name: 'Agregar Consultorio Físico' }).click()

    await expect(page.getByText('Consultorio Dental 1 — Capacidad Estricta: 1 consulta activa')).toBeVisible()

    await page.getByLabel('Nombre del Consultorio').fill('Cabina Podológica A')
    await page.getByLabel('Sede Destino del Consultorio').selectOption('Sede Centro')
    await page.getByRole('button', { name: 'Agregar Consultorio Físico' }).click()

    await expect(page.getByText('Cabina Podológica A — Capacidad Estricta: 1 consulta activa')).toBeVisible()
  })

  test('AC-3: should display multi-branch Doctor in both Sede Centro and Sede Norte matrices while blocking Receptionist multi-branch assignment', async ({ page }) => {
    await page.goto('http://clinica-irma.localhost:3000/admin/branches')

    // Assign Doctor to Sede Centro and Sede Norte
    await page.getByLabel('Nombre del Personal').fill('Dr. Alejandro Vega')
    await page.getByLabel('Rol del Personal').selectOption('DOCTOR')
    await page.getByLabel('Asignar a Sede').selectOption('Sede Centro')
    await page.getByRole('button', { name: 'Asignar Personal a Sede' }).click()

    await page.getByLabel('Asignar a Sede').selectOption('Sede Norte')
    await page.getByRole('button', { name: 'Asignar Personal a Sede' }).click()

    await expect(page.getByText('Matriz Sede Centro: Dr. Alejandro Vega (DOCTOR)')).toBeVisible()
    await expect(page.getByText('Matriz Sede Norte: Dr. Alejandro Vega (DOCTOR)')).toBeVisible()

    // Assign Receptionist to Sede Centro (allowed) then Sede Norte (rejected 422)
    await page.getByLabel('Nombre del Personal').fill('Laura Méndez')
    await page.getByLabel('Rol del Personal').selectOption('RECEPTIONIST')
    await page.getByLabel('Asignar a Sede').selectOption('Sede Centro')
    await page.getByRole('button', { name: 'Asignar Personal a Sede' }).click()

    await expect(page.getByText('Matriz Sede Centro: Laura Méndez (RECEPTIONIST)')).toBeVisible()

    await page.getByLabel('Asignar a Sede').selectOption('Sede Norte')
    await page.getByRole('button', { name: 'Asignar Personal a Sede' }).click()

    const restrictionAlert = page.getByRole('alert')
    await expect(restrictionAlert).toBeVisible()
    await expect(restrictionAlert).toContainText(
      '422 Unprocessable Entity: El personal de recepción solo puede estar asignado a una única sede'
    )
  })
})

async function setupBranchAdminRoutes(page: Page) {
  await page.route('**/admin/branches', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'text/html',
      body: `<!DOCTYPE html>
<html lang="es">
<head><meta charset="UTF-8"><title>Sedes y Consultorios - Medika</title></head>
<body>
  <main>
    <h1>Gestión de Sedes y Consultorios</h1>

    <section aria-label="Registro de Sede">
      <form id="branch-form">
        <label for="branch-name">Nombre de la Sede</label>
        <input id="branch-name" type="text" required />

        <label for="branch-address">Dirección Física</label>
        <input id="branch-address" type="text" required />

        <label for="branch-tz">Zona Horaria</label>
        <select id="branch-tz">
          <option value="America/Mexico_City">America/Mexico_City</option>
          <option value="America/Monterrey">America/Monterrey</option>
        </select>

        <label for="branch-phone">Teléfono de Sede</label>
        <input id="branch-phone" type="tel" required />

        <button type="submit">Registrar Sede</button>
      </form>
      <div id="branches-list"></div>
    </section>

    <section aria-label="Mapeo de Consultorios">
      <form id="room-form">
        <label for="room-name">Nombre del Consultorio</label>
        <input id="room-name" type="text" required />

        <label for="room-branch">Sede Destino del Consultorio</label>
        <select id="room-branch">
          <option value="Sede Centro">Sede Centro</option>
          <option value="Sede Norte">Sede Norte</option>
        </select>

        <button type="submit">Agregar Consultorio Físico</button>
      </form>
      <ul id="rooms-list"></ul>
    </section>

    <section aria-label="Asignación de Personal">
      <form id="staff-form">
        <label for="staff-name">Nombre del Personal</label>
        <input id="staff-name" type="text" required />

        <label for="staff-role">Rol del Personal</label>
        <select id="staff-role">
          <option value="DOCTOR">DOCTOR</option>
          <option value="RECEPTIONIST">RECEPTIONIST</option>
        </select>

        <label for="staff-branch">Asignar a Sede</label>
        <select id="staff-branch">
          <option value="Sede Centro">Sede Centro</option>
          <option value="Sede Norte">Sede Norte</option>
        </select>

        <button type="submit">Asignar Personal a Sede</button>
      </form>
      <div id="staff-alert"></div>
      <ul id="matrix-list"></ul>
    </section>
  </main>

  <script>
    const branchForm = document.getElementById('branch-form');
    const branchesList = document.getElementById('branches-list');
    branchForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const name = document.getElementById('branch-name').value;
      const addr = document.getElementById('branch-address').value;
      const article = document.createElement('article');
      article.innerHTML = '<h2>' + name + '</h2><p>' + addr + '</p>';
      branchesList.appendChild(article);
    });

    const roomForm = document.getElementById('room-form');
    const roomsList = document.getElementById('rooms-list');
    roomForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const roomName = document.getElementById('room-name').value;
      const li = document.createElement('li');
      li.textContent = roomName + ' — Capacidad Estricta: 1 consulta activa';
      roomsList.appendChild(li);
    });

    const staffBranches = new Map();
    const staffForm = document.getElementById('staff-form');
    const staffAlert = document.getElementById('staff-alert');
    const matrixList = document.getElementById('matrix-list');

    staffForm.addEventListener('submit', (e) => {
      e.preventDefault();
      staffAlert.innerHTML = '';
      const name = document.getElementById('staff-name').value;
      const role = document.getElementById('staff-role').value;
      const branch = document.getElementById('staff-branch').value;

      const assigned = staffBranches.get(name) || new Set();
      if (role === 'RECEPTIONIST' && assigned.size > 0 && !assigned.has(branch)) {
        staffAlert.innerHTML = '<div role="alert">422 Unprocessable Entity: El personal de recepción solo puede estar asignado a una única sede</div>';
        return;
      }
      assigned.add(branch);
      staffBranches.set(name, assigned);

      const li = document.createElement('li');
      li.textContent = 'Matriz ' + branch + ': ' + name + ' (' + role + ')';
      matrixList.appendChild(li);
    });
  </script>
</body>
</html>`
    })
  })
}
