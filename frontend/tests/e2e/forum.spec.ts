/**
 * TC-FORUM-001: Authenticated user creates a forum post and sees it in the list.
 * TC-FORUM-002: Another user upvotes, toggles, downvotes, and verifies persistence.
 *
 * Tests run serially so postUrl set in TC-FORUM-001 is available to TC-FORUM-002.
 */

import { test, expect } from '@playwright/test';
import { createUser, loginAs, uniqueEmail, uniqueTitle, UserData } from './helpers/users';

let std: UserData;
let exp: UserData;
let postUrl: string;
let postTitle: string;

test.describe.serial('Forum flows', () => {

  test.beforeAll(async ({ request }) => {
    std = { email: uniqueEmail('forum-std'), password: 'TestPass123!', full_name: 'Forum Std User', role: 'STANDARD' };
    exp = { email: uniqueEmail('forum-exp'), password: 'TestPass123!', full_name: 'Forum Exp User', role: 'EXPERT', category_id: 1 };
    postTitle = uniqueTitle('Earthquake safety tips');
    await createUser(request, std);
    await createUser(request, exp);
  });

  // ── TC-FORUM-001 ──────────────────────────────────────────────────────────────

  test('TC-FORUM-001 — create a forum post and verify it appears in the list', async ({ page }) => {
    await loginAs(page, std.email, std.password);
    await page.goto('/forum');

    // hub-selector-bar is position:fixed z-index:1000 and intercepts pointer events over this button;
    // use JS .click() to fire the React handler directly, bypassing pointer routing
    await page.locator('button:has-text("+ New Post")').evaluate(el => (el as HTMLElement).click());
    await page.waitForURL('**/forum/new');

    await page.fill('#title', postTitle);
    await page.fill('#content', 'Store 3 days of water per person.');

    await page.click('button:has-text("Create Post")');

    // PostCreatePage navigates to /forum?tab=GLOBAL on success, not the post detail
    await page.waitForURL(/\/forum\?tab=/);
    await expect(page.getByText(postTitle)).toBeVisible();

    // Open the post detail to capture the URL for TC-FORUM-002
    await page.locator('.post-card-title').filter({ hasText: postTitle }).click();
    await page.waitForURL(/\/forum\/posts\/\d+/);
    postUrl = page.url();
  });

  // ── TC-FORUM-002 ──────────────────────────────────────────────────────────────

  test('TC-FORUM-002 — upvote, toggle, downvote, and verify persistence', async ({ page }) => {
    await loginAs(page, exp.email, exp.password);
    await page.goto(postUrl);

    const upvoteBtn = page.locator('[title="Upvote"]');
    const downvoteBtn = page.locator('[title="Downvote"]');

    // Vote buttons render as "▲ N" / "▼ N" — extract the trailing number
    const parseCount = async (btn: typeof upvoteBtn): Promise<number> => {
      const text = await btn.innerText();
      return parseInt(text.replace(/\D+/g, ''), 10) || 0;
    };

    const initialUp = await parseCount(upvoteBtn);
    const initialDown = await parseCount(downvoteBtn);

    // Upvote — count +1, button becomes active
    await upvoteBtn.click();
    await expect(upvoteBtn).toContainText(String(initialUp + 1));
    await expect(upvoteBtn).toHaveClass(/vote-active/);

    // Toggle off — wait for active class to clear (confirms API round-trip), then check count
    await upvoteBtn.click();
    await expect(upvoteBtn).not.toHaveClass(/vote-active/);
    await expect(upvoteBtn).toContainText(String(initialUp));

    // Downvote — downvote count +1
    await downvoteBtn.click();
    await expect(downvoteBtn).toContainText(String(initialDown + 1));

    // Reload — state persisted
    await page.reload();
    await expect(downvoteBtn).toContainText(String(initialDown + 1));
    await expect(downvoteBtn).toHaveClass(/vote-active/);
  });

}); // end describe.serial
