import { Page, APIRequestContext } from '@playwright/test';

const API_BASE = process.env.VITE_API_BASE ?? 'http://127.0.0.1:8000';

export interface UserData {
  email: string;
  password: string;
  full_name: string;
  role?: 'STANDARD' | 'EXPERT';
  category_id?: number;
}

/** Returns a unique email to prevent cross-run state pollution. */
export function uniqueEmail(prefix: string): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2)}@e2e.test`;
}

/**
 * Creates a user via the Django register API.
 * hub_id is omitted — optional in the backend, keeps tests hub-independent.
 * For EXPERT users pass category_id=1 (First Aid/Medical, seeded by migration 0006).
 */
export async function createUser(request: APIRequestContext, data: UserData): Promise<void> {
  const payload: Record<string, unknown> = {
    full_name: data.full_name,
    email: data.email,
    password: data.password,
    confirm_password: data.password,
    role: data.role ?? 'STANDARD',
  };
  if (data.category_id !== undefined) payload.category_id = data.category_id;

  const res = await request.post(`${API_BASE}/register`, { data: payload });
  if (!res.ok()) {
    const body = await res.text();
    throw new Error(`createUser failed (${res.status()}): ${body}`);
  }
}

/**
 * Logs in via the UI sign-in form.
 * Waits for redirect to /dashboard before returning.
 */
export async function loginAs(page: Page, email: string, password: string): Promise<void> {
  await page.goto('/signin');
  await page.fill('#email', email);
  await page.fill('#password', password);
  await page.click('button:has-text("Sign In")');
  await page.waitForURL('**/dashboard');
}

/**
 * Clears the auth token from localStorage and navigates to /signin.
 * Use this between user switches in multi-user test flows.
 */
export async function logout(page: Page): Promise<void> {
  await page.evaluate(() => localStorage.removeItem('token'));
  await page.goto('/signin');
}
