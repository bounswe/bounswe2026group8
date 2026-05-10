/**
 * Unit and integration tests for SignUpPage.
 *
 * Covers: form rendering, client-side validation (passwords mismatch, too short,
 * missing full name), successful registration, and backend error display
 * (section 6.2 of the test plan).
 */

import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import SignUpPage from './SignUpPage';
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

function renderSignUp() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <SignUpPage />
      </AuthProvider>
    </MemoryRouter>
  );
}

describe('SignUpPage', () => {
  beforeEach(() => {
    localStorage.clear();
    mockNavigate.mockClear();
    api.getMe.mockResolvedValue({ ok: false, status: 401, data: null });
    api.getHubs.mockResolvedValue({ ok: true, data: [] });
    api.getExpertiseCategories.mockResolvedValue({ ok: true, data: [] });
    api.register.mockClear();
  });

  // ── Rendering ────────────────────────────────────────────────────────────────

  it('renders the registration form fields', async () => {
    await act(async () => { renderSignUp(); });
    expect(screen.getByLabelText(/full name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText('Password', { selector: 'input' })).toBeInTheDocument();
    expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
  });

  // ── Validation ───────────────────────────────────────────────────────────────

  it('shows error when passwords do not match', async () => {
    const user = userEvent.setup();
    await act(async () => { renderSignUp(); });

    await user.type(screen.getByLabelText(/full name/i), 'Alice Smith');
    await user.type(screen.getByLabelText(/email/i), 'alice@example.com');
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'Abc1234!');
    await user.type(screen.getByLabelText(/confirm password/i), 'Different1!');
    await user.click(screen.getByRole('button', { name: /sign up/i }));

    expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
    expect(api.register).not.toHaveBeenCalled();
  });

  it('shows error when password is too short', async () => {
    const user = userEvent.setup();
    await act(async () => { renderSignUp(); });

    await user.type(screen.getByLabelText(/full name/i), 'Alice Smith');
    await user.type(screen.getByLabelText(/email/i), 'alice@example.com');
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'short');
    await user.type(screen.getByLabelText(/confirm password/i), 'short');
    await user.click(screen.getByRole('button', { name: /sign up/i }));

    expect(screen.getByText(/password must be at least 8 characters/i)).toBeInTheDocument();
    expect(api.register).not.toHaveBeenCalled();
  });

  it('shows error when full name is empty', async () => {
    const user = userEvent.setup();
    await act(async () => { renderSignUp(); });

    await user.type(screen.getByLabelText(/email/i), 'alice@example.com');
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'Password1!');
    await user.type(screen.getByLabelText(/confirm password/i), 'Password1!');
    await user.click(screen.getByRole('button', { name: /sign up/i }));

    expect(screen.getByText(/full name is required/i)).toBeInTheDocument();
    expect(api.register).not.toHaveBeenCalled();
  });

  // ── Successful registration ──────────────────────────────────────────────────

  it('calls register API with correct payload', async () => {
    api.register.mockResolvedValueOnce({ ok: true, data: {} });

    const user = userEvent.setup();
    await act(async () => { renderSignUp(); });

    await user.type(screen.getByLabelText(/full name/i), 'Alice Smith');
    await user.type(screen.getByLabelText(/email/i), 'alice@example.com');
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'Password1!');
    await user.type(screen.getByLabelText(/confirm password/i), 'Password1!');
    await user.click(screen.getByRole('button', { name: /sign up/i }));

    await waitFor(() => {
      expect(api.register).toHaveBeenCalledWith(
        expect.objectContaining({
          full_name: 'Alice Smith',
          email: 'alice@example.com',
          password: 'Password1!',
          confirm_password: 'Password1!',
          role: 'STANDARD',
        })
      );
    });
  });

  it('shows success message on successful registration', async () => {
    api.register.mockResolvedValueOnce({ ok: true, data: {} });

    const user = userEvent.setup();
    await act(async () => { renderSignUp(); });

    await user.type(screen.getByLabelText(/full name/i), 'Alice Smith');
    await user.type(screen.getByLabelText(/email/i), 'alice@example.com');
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'Password1!');
    await user.type(screen.getByLabelText(/confirm password/i), 'Password1!');
    await user.click(screen.getByRole('button', { name: /sign up/i }));

    await waitFor(() => {
      expect(screen.getByText(/account created! redirecting to sign in/i)).toBeInTheDocument();
    });
  });

  // ── Failed registration ──────────────────────────────────────────────────────

  it('shows backend error on failed registration', async () => {
    api.register.mockResolvedValueOnce({
      ok: false,
      data: { message: 'Email already in use.' },
    });

    const user = userEvent.setup();
    await act(async () => { renderSignUp(); });

    await user.type(screen.getByLabelText(/full name/i), 'Alice Smith');
    await user.type(screen.getByLabelText(/email/i), 'existing@example.com');
    await user.type(screen.getByLabelText('Password', { selector: 'input' }), 'Password1!');
    await user.type(screen.getByLabelText(/confirm password/i), 'Password1!');
    await user.click(screen.getByRole('button', { name: /sign up/i }));

    await waitFor(() => {
      expect(screen.getByText(/email already in use/i)).toBeInTheDocument();
    });
    // success banner must not appear — error path stays on the form
    expect(screen.queryByText(/account created/i)).not.toBeInTheDocument();
  });
});
