/**
 * Unit and integration tests for SignInPage.
 *
 * Covers: empty form validation, successful login, failed login error display,
 * and redirect after successful authentication (section 6.1, 6.6 of the test plan).
 */

import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import SignInPage from './SignInPage';
import { AuthProvider } from '../context/AuthContext';

jest.mock('react-i18next', () => {
  const en = require('../locales/en.json');
  function t(key) {
    const parts = key.split('.');
    let val = en;
    for (const p of parts) {
      if (val == null || typeof val !== 'object') return key;
      val = val[p];
    }
    return typeof val === 'string' ? val : key;
  }
  return { useTranslation: () => ({ t, i18n: { changeLanguage: jest.fn(), language: 'en' } }) };
});

jest.mock('../services/api');
import * as api from '../services/api';

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

function renderSignIn() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <SignInPage />
      </AuthProvider>
    </MemoryRouter>
  );
}

describe('SignInPage', () => {
  beforeEach(() => {
    localStorage.clear();
    mockNavigate.mockClear();
    api.getMe.mockResolvedValue({ ok: false, status: 401, data: null });
    api.getHubs.mockResolvedValue({ ok: true, data: [] });
    api.login.mockClear();
  });

  // ── Rendering ────────────────────────────────────────────────────────────────

  it('renders email and password inputs', async () => {
    await act(async () => { renderSignIn(); });
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText('Password', { selector: 'input' })).toBeInTheDocument();
  });

  it('renders a sign in submit button', async () => {
    await act(async () => { renderSignIn(); });
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  // ── Validation ───────────────────────────────────────────────────────────────

  it('shows validation error when email is empty', async () => {
    const user = userEvent.setup();
    await act(async () => { renderSignIn(); });

    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(screen.getByText(/please enter both email and password/i)).toBeInTheDocument();
    expect(api.login).not.toHaveBeenCalled();
  });

  it('shows validation error when password is empty', async () => {
    const user = userEvent.setup();
    await act(async () => { renderSignIn(); });

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(screen.getByText(/please enter both email and password/i)).toBeInTheDocument();
    expect(api.login).not.toHaveBeenCalled();
  });

  // ── Successful login ─────────────────────────────────────────────────────────

  it('calls login API with trimmed email and password', async () => {
    api.login.mockResolvedValueOnce({
      ok: true,
      data: { token: 'tok', refresh: 'ref', user: { email: 'test@example.com', role: 'STANDARD' } },
    });

    const user = userEvent.setup();
    await act(async () => { renderSignIn(); });

    await user.type(screen.getByLabelText(/email/i), '  test@example.com  ');
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'password123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(api.login).toHaveBeenCalledWith({
        email: 'test@example.com',
        password: 'password123',
      });
    });
  });

  it('navigates to dashboard on successful login', async () => {
    api.login.mockResolvedValueOnce({
      ok: true,
      data: { token: 'tok', user: { email: 'test@example.com', role: 'STANDARD' } },
    });

    const user = userEvent.setup();
    await act(async () => { renderSignIn(); });

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'password123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/dashboard', { replace: true });
    });
  });

  // ── Failed login ─────────────────────────────────────────────────────────────

  it('shows backend error message on failed login', async () => {
    api.login.mockResolvedValueOnce({
      ok: false,
      data: { message: 'Invalid email or password' },
    });

    const user = userEvent.setup();
    await act(async () => { renderSignIn(); });

    await user.type(screen.getByLabelText(/email/i), 'bad@example.com');
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'wrongpassword');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByText(/invalid email or password/i)).toBeInTheDocument();
    });
  });

  it('does not navigate on failed login', async () => {
    api.login.mockResolvedValueOnce({
      ok: false,
      data: { message: 'Invalid email or password' },
    });

    const user = userEvent.setup();
    await act(async () => { renderSignIn(); });

    await user.type(screen.getByLabelText(/email/i), 'bad@example.com');
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'wrongpassword');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(mockNavigate).not.toHaveBeenCalled();
    });
  });

  // ── Submitting state ─────────────────────────────────────────────────────────

  it('button shows submitting state during API call', async () => {
    let resolveLogin;
    api.login.mockReturnValueOnce(
      new Promise((res) => { resolveLogin = res; })
    );

    const user = userEvent.setup();
    await act(async () => { renderSignIn(); });

    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'password123');

    act(() => {
      user.click(screen.getByRole('button', { name: /sign in/i }));
    });

    // During the pending promise the button text changes
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /signing in/i })).toBeDisabled();
    });

    // Clean up
    await act(async () => {
      resolveLogin({ ok: false, data: { message: 'Error' } });
    });
  });
});
