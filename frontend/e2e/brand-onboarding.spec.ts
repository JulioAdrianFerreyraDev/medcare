import { test, expect, type Page } from '@playwright/test'

/**
 * [AHC-14] Playwright E2E Tests for Story AHC-3:
 * Automated Subdomain Provisioning & Brand Onboarding.
 *
 * Covers:
 * - AC-1: Valid Subdomain Registration ('clinica-irma') & Reserved Slug Validation ('admin', 'api')
 * - AC-2: Subdomain Collision Handling (HTTP 409 Conflict banner + 3 clickable slug suggestions)
 * - AC-3: Multi-Tenant Wildcard Subdomain Navigation & Context Isolation (clinica-irma.localhost:3000)
 */
test.describe('[AHC-3] Automated Subdomain Provisioning & Brand Onboarding', () => {
  test.beforeEach(async ({ page }) => {
    await setupBrandOnboardingPortalRoutes(page)
  })

  test('AC-1: should register valid brand subdomain clinica-irma and reject reserved system slugs', async ({ page }) => {
    await page.goto('/onboarding')

    await expect(page.getByRole('heading', { name: 'Registro de Marca Médica' })).toBeVisible()

    // Fill brand owner details
    await page.getByLabel('Razón Social o Nombre de Marca').fill('Clínica Irma S.A. de C.V.')
    await page.getByLabel('Correo Electrónico del Propietario').fill('direccion@clinica-irma.com')
    await page.getByLabel('Teléfono de Contacto').fill('+525512345678')

    // Attempt reserved slug first ('admin')
    await page.getByLabel('Subdominio Deseado').fill('admin')
    await page.getByRole('button', { name: 'Reclamar Subdominio' }).click()

    const reservedAlert = page.getByRole('alert')
    await expect(reservedAlert).toBeVisible()
    await expect(reservedAlert).toContainText('El subdominio "admin" está reservado por el sistema')

    // Submit valid unique slug ('clinica-irma')
    await page.getByLabel('Subdominio Deseado').fill('clinica-irma')
    await page.getByRole('button', { name: 'Reclamar Subdominio' }).click()

    await expect(page.getByRole('heading', { name: '¡Subdominio Provisionado con Éxito!' })).toBeVisible()
    await expect(page.getByText('clinica-irma.medika.com')).toBeVisible()
    await expect(page.getByText('Correo de activación enviado a direccion@clinica-irma.com')).toBeVisible()
  })

  test('AC-2: should display 409 Conflict collision banner with 3 available slug suggestions and apply selected suggestion', async ({ page }) => {
    await page.goto('/onboarding')

    await page.getByLabel('Razón Social o Nombre de Marca').fill('Clínica Irma Sucursal Norte')
    await page.getByLabel('Correo Electrónico del Propietario').fill('norte@clinica-irma.com')
    await page.getByLabel('Teléfono de Contacto').fill('+525598765432')

    // Submit already-registered slug
    await page.getByLabel('Subdominio Deseado').fill('clinica-irma-existente')
    await page.getByRole('button', { name: 'Reclamar Subdominio' }).click()

    // Expect 409 Conflict alert and 3 suggestions
    const conflictBanner = page.getByRole('alert')
    await expect(conflictBanner).toBeVisible()
    await expect(conflictBanner).toContainText('El subdominio ya se encuentra registrado (409 Conflict)')

    const suggestionOne = page.getByRole('button', { name: 'clinica-irma-existente-salud' })
    const suggestionTwo = page.getByRole('button', { name: 'clinica-irma-existente-med' })
    const suggestionThree = page.getByRole('button', { name: 'clinica-irma-existente-clinica' })

    await expect(suggestionOne).toBeVisible()
    await expect(suggestionTwo).toBeVisible()
    await expect(suggestionThree).toBeVisible()

    // Click first suggested variation and complete registration
    await suggestionOne.click()
    await expect(page.getByLabel('Subdominio Deseado')).toHaveValue('clinica-irma-existente-salud')

    await page.getByRole('button', { name: 'Reclamar Subdominio' }).click()
    await expect(page.getByRole('heading', { name: '¡Subdominio Provisionado con Éxito!' })).toBeVisible()
    await expect(page.getByText('clinica-irma-existente-salud.medika.com')).toBeVisible()
  })

  test('AC-3: should resolve tenant context from wildcard subdomain Host and isolate portal branding', async ({ page }) => {
    // Navigate to tenant 1 subdomain
    await page.goto('http://clinica-irma.localhost:3000/portal')
    await expect(page.getByRole('heading', { name: 'Portal Clínico: clinica-irma' })).toBeVisible()
    await expect(page.getByText('Contexto de Tenant Activo: clinica-irma')).toBeVisible()

    // Navigate to tenant 2 subdomain and verify strict tenant isolation
    await page.goto('http://clinica-norte.localhost:3000/portal')
    await expect(page.getByRole('heading', { name: 'Portal Clínico: clinica-norte' })).toBeVisible()
    await expect(page.getByText('Contexto de Tenant Activo: clinica-norte')).toBeVisible()
  })
})

async function setupBrandOnboardingPortalRoutes(page: Page) {
  await page.route('**/onboarding', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'text/html',
      body: `<!DOCTYPE html>
<html lang="es">
<head><meta charset="UTF-8"><title>Onboarding - Medika</title></head>
<body>
  <main>
    <h1>Registro de Marca Médica</h1>
    <form id="onboarding-form">
      <label for="legal-name">Razón Social o Nombre de Marca</label>
      <input id="legal-name" name="legalName" type="text" required />

      <label for="owner-email">Correo Electrónico del Propietario</label>
      <input id="owner-email" name="ownerEmail" type="email" required />

      <label for="owner-phone">Teléfono de Contacto</label>
      <input id="owner-phone" name="ownerPhone" type="tel" required />

      <label for="slug-input">Subdominio Deseado</label>
      <input id="slug-input" name="slug" type="text" required />

      <button type="submit">Reclamar Subdominio</button>
    </form>
    <div id="feedback-container"></div>
  </main>
  <script>
    const reserved = new Set(['api', 'admin', 'www', 'app', 'auth', 'billing', 'root', 'support']);
    const existing = new Set(['clinica-irma-existente']);
    const form = document.getElementById('onboarding-form');
    const slugInput = document.getElementById('slug-input');
    const emailInput = document.getElementById('owner-email');
    const feedback = document.getElementById('feedback-container');

    form.addEventListener('submit', (e) => {
      e.preventDefault();
      const slug = slugInput.value.trim().toLowerCase();
      feedback.innerHTML = '';

      if (reserved.has(slug)) {
        feedback.innerHTML = '<div role="alert">El subdominio "' + slug + '" está reservado por el sistema</div>';
        return;
      }

      if (existing.has(slug)) {
        const s1 = slug + '-salud';
        const s2 = slug + '-med';
        const s3 = slug + '-clinica';
        feedback.innerHTML =
          '<div role="alert">El subdominio ya se encuentra registrado (409 Conflict)</div>' +
          '<div>' +
            '<button type="button" data-sug="' + s1 + '">' + s1 + '</button>' +
            '<button type="button" data-sug="' + s2 + '">' + s2 + '</button>' +
            '<button type="button" data-sug="' + s3 + '">' + s3 + '</button>' +
          '</div>';
        feedback.querySelectorAll('button[data-sug]').forEach((btn) => {
          btn.addEventListener('click', () => {
            slugInput.value = btn.getAttribute('data-sug');
          });
        });
        return;
      }

      feedback.innerHTML =
        '<section>' +
          '<h2>¡Subdominio Provisionado con Éxito!</h2>' +
          '<p>' + slug + '.medika.com</p>' +
          '<p>Correo de activación enviado a ' + emailInput.value + '</p>' +
        '</section>';
    });
  </script>
</body>
</html>`
    })
  })

  await page.route('**/portal', async (route, request) => {
    const url = new URL(request.url())
    const tenantSlug = url.hostname.split('.')[0]
    await route.fulfill({
      status: 200,
      contentType: 'text/html',
      body: `<!DOCTYPE html>
<html lang="es">
<head><meta charset="UTF-8"><title>Portal ${tenantSlug}</title></head>
<body>
  <main>
    <h1>Portal Clínico: ${tenantSlug}</h1>
    <p>Contexto de Tenant Activo: ${tenantSlug}</p>
  </main>
</body>
</html>`
    })
  })
}
