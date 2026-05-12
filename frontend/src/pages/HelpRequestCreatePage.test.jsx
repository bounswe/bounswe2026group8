/**
 * Integration tests for HelpRequestCreatePage.
 *
 * Covers: form rendering, client-side validation (empty title, empty description),
 * successful submission payload, navigation on success, and global error display.
 */

import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import HelpRequestCreatePage from './HelpRequestCreatePage';
import { AuthProvider } from '../context/AuthContext';

jest.mock('react-i18next', () => {
  const en = require('../locales/en.json');
  function t(key, opts) {
    const parts = key.split('.');
    let val = en;
    for (const p of parts) {
      if (val == null || typeof val !== 'object') return key;
      val = val[p];
    }
    if (typeof val !== 'string') return key;
    if (!opts) return val;
    return val.replace(/\{\{(\w+)\}\}/g, (_, k) =>
      opts[k] !== undefined ? String(opts[k]) : `{{${k}}}`
    );
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

const STANDARD_USER = {
  id: 1,
  email: 'u@example.com',
  full_name: 'Alice',
  role: 'STANDARD',
  hub: { id: 2, name: 'Istanbul', slug: 'istanbul' },
};

/**
 * Renders HelpRequestCreatePage as an authenticated user.
 * Waits for the submit button since the page returns null until auth resolves.
 */
async function renderCreate(userData = STANDARD_USER) {
  localStorage.setItem('token', 'test-token');
  api.getMe.mockResolvedValue({ ok: true, status: 200, data: userData });
  await act(async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <HelpRequestCreatePage />
        </AuthProvider>
      </MemoryRouter>
    );
  });
  await waitFor(() => {
    expect(screen.getByRole('button', { name: /submit request/i })).toBeInTheDocument();
  });
}

describe('HelpRequestCreatePage', () => {
  beforeEach(() => {
    localStorage.clear();
    mockNavigate.mockClear();
    api.getMe.mockResolvedValue({ ok: false, status: 401, data: null });
    api.getHubs.mockResolvedValue({ ok: true, data: [] });
    api.createHelpRequest.mockClear();
  });

  // ── Rendering ────────────────────────────────────────────────────────────────

  it('renders form fields', async () => {
    await renderCreate();
    expect(screen.getByLabelText(/^title$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^description$/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /submit request/i })).toBeInTheDocument();
  });

  // ── Validation ───────────────────────────────────────────────────────────────

  it('shows error when title is empty', async () => {
    await renderCreate();

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /submit request/i }));

    expect(screen.getByText('Title is required.')).toBeInTheDocument();
    expect(api.createHelpRequest).not.toHaveBeenCalled();
  });

  it('shows error when description is empty', async () => {
    await renderCreate();

    const user = userEvent.setup();
    await user.type(screen.getByLabelText(/^title$/i), 'Need help');
    await user.click(screen.getByRole('button', { name: /submit request/i }));

    expect(screen.getByText('Description is required.')).toBeInTheDocument();
    expect(api.createHelpRequest).not.toHaveBeenCalled();
  });

  // ── Submission ───────────────────────────────────────────────────────────────

  it('calls createHelpRequest with correct payload', async () => {
    api.createHelpRequest.mockResolvedValue({ ok: true, data: { id: 5 } });
    await renderCreate();

    const user = userEvent.setup();
    await user.type(screen.getByLabelText(/^title$/i), 'Need help');
    await user.type(screen.getByLabelText(/^description$/i), 'Urgent');
    await user.click(screen.getByRole('button', { name: /submit request/i }));

    await waitFor(() => {
      expect(api.createHelpRequest).toHaveBeenCalledWith({
        title: 'Need help',
        description: 'Urgent',
        category: 'MEDICAL',
        urgency: 'MEDIUM',
      });
    });
  });

  it('navigates to detail page on successful creation', async () => {
    api.createHelpRequest.mockResolvedValue({ ok: true, data: { id: 5 } });
    await renderCreate();

    const user = userEvent.setup();
    await user.type(screen.getByLabelText(/^title$/i), 'Need help');
    await user.type(screen.getByLabelText(/^description$/i), 'Urgent');
    await user.click(screen.getByRole('button', { name: /submit request/i }));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/help-requests/5');
    });
  });

  it('shows global error on failed creation', async () => {
    api.createHelpRequest.mockResolvedValue({
      ok: false,
      data: { detail: 'Server error' },
    });
    await renderCreate();

    const user = userEvent.setup();
    await user.type(screen.getByLabelText(/^title$/i), 'Need help');
    await user.type(screen.getByLabelText(/^description$/i), 'Urgent');
    await user.click(screen.getByRole('button', { name: /submit request/i }));

    await waitFor(() => {
      expect(screen.getByText('Server error')).toBeInTheDocument();
    });
  });
});
