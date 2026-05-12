/**
 * TC-AUTH-001: Register a new Expert user via the UI form.
 * TC-AUTH-002: Login with valid/invalid credentials.
 */

import { test, expect } from '@playwright/test';
import { createUser, loginAs, uniqueEmail, UserData } from './helpers/users';

// ── TC-AUTH-001 ───────────────────────────────────────────────────────────────

test.describe('TC-AUTH-001 — Expert registration via UI', () => {
  test('registers a new Expert user and logs in successfully', async ({ page }) => {
    const email = uniqueEmail('auth001');
    const password = 'TestPass123!';

    await page.goto('/signup');

    await page.fill('#full_name', 'Expert User Test');
    await page.fill('#email', email);
    await page.fill('#password', password);
    await page.fill('#confirm_password', password);

    // Select Expert role — expertise category dropdown should appear
    await page.selectOption('#role', 'EXPERT');
    await expect(page.locator('#category_id')).toBeVisible();
    await page.selectOption('#category_id', { index: 1 });

    // Select first available hub if one exists
    const hubSelect = page.locator('#hub_id');
    const hubOptions = await hubSelect.locator('option').count();
    if (hubOptions > 1) {
      await hubSelect.selectOption({ index: 1 });
    }

    await page.click('button:has-text("Sign Up")');

    // SignUpPage shows a success message then redirects to /signin after ~1500ms
    await expect(page.getByText(/account created/i)).toBeVisible({ timeout: 15000 });
    await page.waitForURL('**/signin', { timeout: 10000 });

    // Login with new credentials
    await loginAs(page, email, password);
    await expect(page).toHaveURL(/\/dashboard/);
  });
});

// ── TC-AUTH-002 ───────────────────────────────────────────────────────────────

test.describe('TC-AUTH-002 — Login with valid and invalid credentials', () => {
  let user: UserData;

  test.beforeAll(async ({ request }) => {
    user = {
      email: uniqueEmail('auth002'),
      password: 'TestPass123!',
      full_name: 'Auth Test User',
      role: 'STANDARD',
    };
    await createUser(request, user);
  });

  test('valid login reaches dashboard and allows access to protected route', async ({ page }) => {
    await loginAs(page, user.email, user.password);
    await expect(page).toHaveURL(/\/dashboard/);

    // Protected route loads without redirect back to /signin
    await page.goto('/profile');
    await expect(page).not.toHaveURL(/\/signin/);
    await expect(page.getByText('Your Profile')).toBeVisible();
  });

  test('invalid password shows error message', async ({ page }) => {
    await page.goto('/signin');
    await page.fill('#email', user.email);
    await page.fill('#password', 'wrongpassword');
    await page.click('button:has-text("Sign In")');

    await expect(page.locator('.alert-error')).toBeVisible();
    await expect(page.locator('.alert-error')).toContainText(/invalid/i);
  });
});
