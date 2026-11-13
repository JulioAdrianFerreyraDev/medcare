import { test, expect } from '@playwright/test'

test.describe('Platform Initialization Smoke Test', () => {
  test('should display the Medika Clinical Platform initialized landing shell', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('heading', { name: 'Medika Clinical Platform' })).toBeVisible()
  })
})
