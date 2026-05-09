/**
 * TC-FORUM-001: Authenticated user creates a forum post and sees it in the list.
 * TC-FORUM-002: Another user upvotes, toggles, downvotes, and verifies persistence.
 */

import { test, expect } from '@playwright/test';
import { createUser, loginAs, uniqueEmail, UserData } from './helpers/users';

let std: UserData;
let exp: UserData;
let postUrl: string;

test.beforeAll(async ({ request }) => {
  std = { email: uniqueEmail('forum-std'), password: 'TestPass123!', full_name: 'Forum Std User', role: 'STANDARD' };
  exp = { email: uniqueEmail('forum-exp'), password: 'TestPass123!', full_name: 'Forum Exp User', role: 'EXPERT', category_id: 1 };
  await createUser(request, std);
  await createUser(request, exp);
});

// ── TC-FORUM-001 ──────────────────────────────────────────────────────────────

test('TC-FORUM-001 — create a forum post and verify it appears in the list', async ({ page }) => {
  await loginAs(page, std.email, std.password);
  await page.goto('/forum');

  await page.click('button:has-text("+ New Post")');
  await page.waitForURL('**/forum/new');

  await page.fill('#title', 'Earthquake safety tips');
  await page.fill('#content', 'Store 3 days of water per person.');

  await page.click('button:has-text("Create Post")');

  // Redirects to the new post's detail page
  await page.waitForURL(/\/forum\/posts\/\d+/);
  postUrl = page.url();

  await expect(page.getByText('Earthquake safety tips')).toBeVisible();

  // Post also appears in the forum list
  await page.goto('/forum');
  await expect(page.getByText('Earthquake safety tips')).toBeVisible();
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
  await expect(upvoteBtn).toHaveClass(/vote-active/);
  await expect(upvoteBtn).toContainText(String(initialUp + 1));

  // Toggle off — count returns to original
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
