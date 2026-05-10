import { test, expect } from '@playwright/test';

test('homepage loads without error', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveTitle('Neighborhood Emergency Hub');
  await expect(page.locator('h1').first()).toBeVisible();
});
