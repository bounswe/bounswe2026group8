/**
 * Integration tests for AuthContext.
 *
 * Tests authentication state management: login, logout, token persistence,
 * and session restoration from localStorage (section 2.2 of the test plan).
 */

import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AuthProvider, useAuth } from './AuthContext';

jest.mock('../services/api');
import * as api from '../services/api';

/** Minimal component that exposes auth state for assertions. */
function AuthTestComponent() {
  const { isAuthenticated, user, loginUser, logoutUser, hubs } = useAuth();
  return (
    <div>
      <div data-testid="auth-status">{isAuthenticated ? 'authenticated' : 'unauthenticated'}</div>
      {user && <div data-testid="user-email">{user.email}</div>}
      {user && <div data-testid="user-role">{user.role}</div>}
      <div data-testid="hub-count">{hubs.length}</div>
      <button
        onClick={() =>
          loginUser('token-abc', { email: 'test@example.com', role: 'STANDARD' })
        }
      >
        Login
      </button>
      <button onClick={logoutUser}>Logout</button>
    </div>
  );
}

async function renderAuth() {
  await act(async () => {
    render(
      <AuthProvider>
        <AuthTestComponent />
      </AuthProvider>
    );
  });
}

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear();
    // Default: no token → skip getMe. Hub list returns empty.
    api.getMe.mockResolvedValue({ ok: false, status: 401, data: null });
    api.getHubs.mockResolvedValue({ ok: true, data: [] });
  });

  // ── Initial state ────────────────────────────────────────────────────────────

  it('starts unauthenticated when no token is stored', async () => {
    await renderAuth();
    expect(screen.getByTestId('auth-status')).toHaveTextContent('unauthenticated');
  });

  // ── Login ────────────────────────────────────────────────────────────────────

  it('loginUser sets authenticated state and stores token', async () => {
    // After loginUser sets the token, the useEffect re-runs and calls getMe.
    // Return success so the session is not immediately cleared.
    api.getMe.mockResolvedValue({
      ok: true,
      status: 200,
      data: { email: 'test@example.com', role: 'STANDARD' },
    });

    const user = userEvent.setup();
    await renderAuth();

    await user.click(screen.getByRole('button', { name: 'Login' }));

    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('authenticated');
    });
    expect(screen.getByTestId('user-email')).toHaveTextContent('test@example.com');
    expect(localStorage.getItem('token')).toBe('token-abc');
  });

  it('loginUser stores user role correctly', async () => {
    api.getMe.mockResolvedValue({
      ok: true,
      status: 200,
      data: { email: 'test@example.com', role: 'STANDARD' },
    });

    const user = userEvent.setup();
    await renderAuth();
    await user.click(screen.getByRole('button', { name: 'Login' }));
    await waitFor(() => {
      expect(screen.getByTestId('user-role')).toHaveTextContent('STANDARD');
    });
  });

  // ── Logout ───────────────────────────────────────────────────────────────────

  it('logoutUser clears authenticated state and removes token', async () => {
    // Session is restored from localStorage on mount
    localStorage.setItem('token', 'existing-token');
    api.getMe.mockResolvedValue({
      ok: true,
      status: 200,
      data: { email: 'user@example.com', role: 'STANDARD' },
    });

    const user = userEvent.setup();
    await renderAuth();

    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('authenticated');
    });

    await user.click(screen.getByRole('button', { name: 'Logout' }));
    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('unauthenticated');
    });
    expect(localStorage.getItem('token')).toBeNull();
  });

  // ── Session restoration ──────────────────────────────────────────────────────

  it('restores session when valid token exists in localStorage', async () => {
    localStorage.setItem('token', 'existing-token');
    api.getMe.mockResolvedValueOnce({
      ok: true,
      status: 200,
      data: { email: 'saved@example.com', role: 'EXPERT' },
    });

    await renderAuth();

    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('authenticated');
    });
    expect(screen.getByTestId('user-email')).toHaveTextContent('saved@example.com');
  });

  it('clears invalid token and stays unauthenticated on 401', async () => {
    localStorage.setItem('token', 'expired-token');
    api.getMe.mockResolvedValueOnce({ ok: false, status: 401, data: null });

    await renderAuth();

    await waitFor(() => {
      expect(screen.getByTestId('auth-status')).toHaveTextContent('unauthenticated');
    });
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('keeps token but shows unauthenticated on network error (5xx)', async () => {
    localStorage.setItem('token', 'valid-token');
    api.getMe.mockResolvedValueOnce({ ok: false, status: 502, data: null });

    await renderAuth();

    await waitFor(() => {
      // Loading resolves after the failed getMe call
      expect(screen.getByTestId('auth-status')).toHaveTextContent('unauthenticated');
    });
    // Token is preserved — don't log user out on transient server errors
    expect(localStorage.getItem('token')).toBe('valid-token');
  });

  // ── Hubs ─────────────────────────────────────────────────────────────────────

  it('loads hubs on mount', async () => {
    api.getHubs.mockResolvedValueOnce({
      ok: true,
      data: [
        { id: 1, name: 'Istanbul', slug: 'istanbul' },
        { id: 2, name: 'Ankara', slug: 'ankara' },
      ],
    });

    await renderAuth();

    await waitFor(() => {
      expect(screen.getByTestId('hub-count')).toHaveTextContent('2');
    });
  });
});
