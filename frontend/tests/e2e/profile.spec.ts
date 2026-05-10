/**
 * Profile page: rendering, resource management, role-conditional expertise section,
 * and authorization guard (other users cannot edit/delete your content).
 */

import { test, expect } from '@playwright/test';
import { createUser, loginAs, logout, uniqueEmail, uniqueTitle, UserData } from './helpers/users';

let std: UserData;
let exp: UserData;
let std2: UserData;

test.beforeAll(async ({ request }) => {
  std  = { email: uniqueEmail('profile-std'),  password: 'TestPass123!', full_name: 'Profile Std User',  role: 'STANDARD' };
  exp  = { email: uniqueEmail('profile-exp'),  password: 'TestPass123!', full_name: 'Profile Exp User',  role: 'EXPERT', category_id: 1 };
  std2 = { email: uniqueEmail('profile-std2'), password: 'TestPass123!', full_name: 'Profile Std2 User', role: 'STANDARD' };
  await createUser(request, std);
  await createUser(request, exp);
  await createUser(request, std2);
});

// ── Profile rendering ─────────────────────────────────────────────────────────

test('profile shows user name, email, and Personal Information section', async ({ page }) => {
  await loginAs(page, std.email, std.password);
  await page.goto('/profile');

  await expect(page.getByText(std.full_name)).toBeVisible();
  await expect(page.getByText(std.email)).toBeVisible();
  await expect(page.getByText('Personal Information')).toBeVisible();
});

// ── Resource management ───────────────────────────────────────────────────────

test('resource can be added and appears in the list', async ({ page }) => {
  await loginAs(page, std.email, std.password);
  await page.goto('/profile');

  // Open the Resources accordion
  await page.click('button:has-text("Resources")');
  await expect(page.getByText('No resources added yet.')).toBeVisible();

  // Open add form
  await page.click('button:has-text("+ Add Resource")');

  await page.fill('input[placeholder="e.g. Generator"]', 'Generator');
  await page.fill('input[placeholder="e.g. Power"]', 'Power');

  const qtyInput = page.locator('input[type="number"]');
  await qtyInput.fill('2');

  await page.click('button:has-text("Save")');

  await expect(page.getByText('Generator')).toBeVisible();
});

// ── Role restriction — expertise section ──────────────────────────────────────

test('expertise accordion is hidden for standard user', async ({ page }) => {
  await loginAs(page, std.email, std.password);
  await page.goto('/profile');
  await expect(page.locator('button:has-text("Expertise Fields")')).not.toBeVisible();
});

test('expertise accordion is visible for expert user', async ({ page }) => {
  await loginAs(page, exp.email, exp.password);
  await page.goto('/profile');
  await expect(page.locator('button:has-text("Expertise Fields")')).toBeVisible();
});

// ── Authorization guard ───────────────────────────────────────────────────────

test('another user cannot edit or delete a post they did not create', async ({ page }) => {
  const postTitle = uniqueTitle('Auth guard test post');
  // std creates a forum post
  await loginAs(page, std.email, std.password);
  await page.goto('/forum/new');
  await page.fill('#title', postTitle);
  await page.fill('#content', 'This post belongs to std.');
  await page.click('button:has-text("Create Post")');
  // PostCreatePage navigates to /forum?tab=GLOBAL on success, not the post detail
  await page.waitForURL(/\/forum/);
  await page.locator('.post-card-title').filter({ hasText: postTitle }).click();
  await page.waitForURL(/\/forum\/posts\/\d+/);
  const postUrl = page.url();

  // std2 opens the same post — Edit and Delete must not be visible
  await logout(page);
  await loginAs(page, std2.email, std2.password);
  await page.goto(postUrl);

  await expect(page.locator('button:has-text("Edit")')).not.toBeVisible();
  await expect(page.locator('button:has-text("Delete")')).not.toBeVisible();
});
