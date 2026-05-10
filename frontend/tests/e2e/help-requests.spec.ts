/**
 * TC-HELP-001: Standard user creates a help request; status shows OPEN.
 * TC-HELP-002: Expert takes on request; status changes to Expert Responding.
 * TC-HELP-003: Author marks request as resolved; button disappears for others.
 *
 * Tests run serially so requestUrl set in TC-HELP-001 is available to later tests.
 */

import { test, expect } from '@playwright/test';
import { createUser, loginAs, logout, uniqueEmail, uniqueTitle, UserData } from './helpers/users';

let std: UserData;
let exp: UserData;
let requestUrl: string;
let requestTitle: string;

test.describe.serial('Help Request flows', () => {

  test.beforeAll(async ({ request }) => {
    std = { email: uniqueEmail('help-std'), password: 'TestPass123!', full_name: 'Help Std User', role: 'STANDARD' };
    exp = { email: uniqueEmail('help-exp'), password: 'TestPass123!', full_name: 'Help Exp User', role: 'EXPERT', category_id: 1 };
    requestTitle = uniqueTitle('Need medical assistance');
    await createUser(request, std);
    await createUser(request, exp);
  });

  // ── TC-HELP-001 ───────────────────────────────────────────────────────────────

  test('TC-HELP-001 — create a help request; status shows Open', async ({ page }) => {
    await loginAs(page, std.email, std.password);
    await page.goto('/help-requests');

    await page.click('button:has-text("+ New Request")');
    await page.waitForURL('**/help-requests/new');

    await page.fill('#title', requestTitle);
    await page.fill('#description', 'I need help with medication delivery');
    await page.selectOption('#category', 'MEDICAL');
    await page.selectOption('#urgency', 'HIGH');

    await page.click('button:has-text("Submit Request")');
    await page.waitForURL(/\/help-requests\/\d+/);
    requestUrl = page.url();

    // Detail page shows correct content and Open status
    await expect(page.getByText(requestTitle)).toBeVisible();
    await expect(page.locator('.badge').filter({ hasText: /open/i })).toBeVisible();

    // Also appears in the list
    await page.goto('/help-requests');
    await expect(page.getByText(requestTitle)).toBeVisible();
  });

  // ── TC-HELP-002 ───────────────────────────────────────────────────────────────

  test('TC-HELP-002 — expert takes on request; status becomes Expert Responding', async ({ page }) => {
    await loginAs(page, exp.email, exp.password);
    await page.goto(requestUrl);

    await expect(page.locator('.badge').filter({ hasText: /open/i })).toBeVisible();

    // Expert posts a comment
    await page.fill('.help-detail-textarea', 'I can help — contact me at 0555-xxx');
    await page.click('button:has-text("Post Comment")');
    await expect(page.getByText('I can help — contact me at 0555-xxx')).toBeVisible();

    // Expert takes on the request — this sets is_expert_responding = true
    await page.click('button:has-text("Take On Request")');
    await expect(page.locator('.badge').filter({ hasText: /expert responding/i })).toBeVisible();

    // Standard user sees the same status and the comment
    await logout(page);
    await loginAs(page, std.email, std.password);
    await page.goto(requestUrl);
    await expect(page.locator('.badge').filter({ hasText: /expert responding/i })).toBeVisible();
    await expect(page.getByText('I can help — contact me at 0555-xxx')).toBeVisible();
  });

  // ── TC-HELP-003 ───────────────────────────────────────────────────────────────

  test('TC-HELP-003 — author marks as resolved; button hidden for others', async ({ page }) => {
    await loginAs(page, std.email, std.password);
    await page.goto(requestUrl);

    const resolveBtn = page.locator('button:has-text("Mark as Resolved")');
    await expect(resolveBtn).toBeVisible();
    await resolveBtn.click();

    // Status changes to Resolved
    await expect(page.locator('.badge').filter({ hasText: /resolved/i })).toBeVisible();

    // Resolve button is no longer shown
    await expect(page.locator('button:has-text("Mark as Resolved")')).not.toBeVisible();

    // Expert (non-author) also cannot see the resolve button
    await logout(page);
    await loginAs(page, exp.email, exp.password);
    await page.goto(requestUrl);
    await expect(page.locator('button:has-text("Mark as Resolved")')).not.toBeVisible();
  });

}); // end describe.serial
