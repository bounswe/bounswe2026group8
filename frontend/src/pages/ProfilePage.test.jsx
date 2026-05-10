/**
 * Integration tests for ProfilePage.
 *
 * Covers: user identity rendering, personal information section, bio placeholder
 * and populated state, resources accordion (empty/populated/add form), and
 * role-conditional expertise accordion (STANDARD vs EXPERT).
 */

import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import ProfilePage from './ProfilePage';
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

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => jest.fn(),
}));

const STANDARD_USER = {
  id: 1,
  email: 'user@example.com',
  full_name: 'Alice Smith',
  role: 'STANDARD',
  hub: { id: 2, name: 'Istanbul', slug: 'istanbul' },
};
const EXPERT_USER = { ...STANDARD_USER, role: 'EXPERT' };

const EMPTY_PROFILE = {
  phone_number: '',
  blood_type: '',
  emergency_contact: '',
  emergency_contact_phone: '',
  preferred_language: '',
  has_disability: false,
  availability_status: 'SAFE',
  special_needs: '',
  bio: '',
};

const makeResource = (overrides = {}) => ({
  id: 1,
  name: 'Generator',
  category: 'Power',
  quantity: 2,
  condition: true,
  ...overrides,
});

/**
 * Renders ProfilePage as an authenticated user.
 * Waits for both the page title and the user's full name to confirm auth resolved.
 */
async function renderProfile(userData = STANDARD_USER) {
  localStorage.setItem('token', 'test-token');
  api.getMe.mockResolvedValue({ ok: true, status: 200, data: userData });
  await act(async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <ProfilePage />
        </AuthProvider>
      </MemoryRouter>
    );
  });
  await waitFor(() => {
    expect(screen.getByText('Your Profile')).toBeInTheDocument();
    expect(screen.getByText(userData.full_name)).toBeInTheDocument();
  });
}

describe('ProfilePage', () => {
  beforeEach(() => {
    localStorage.clear();
    api.getMe.mockResolvedValue({ ok: false, status: 401, data: null });
    api.getHubs.mockResolvedValue({ ok: true, data: [] });
    api.getProfile.mockResolvedValue({ ok: true, data: EMPTY_PROFILE });
    api.getResources.mockResolvedValue({ ok: true, data: [] });
    api.getExpertiseFields.mockResolvedValue({ ok: true, data: [] });
    api.getExpertiseCategories.mockResolvedValue({ ok: true, data: [] });
    api.getMyBadges.mockResolvedValue({ ok: true, data: [] });
  });

  // ── Identity ─────────────────────────────────────────────────────────────────

  it('renders user name and email', async () => {
    await renderProfile();
    expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    expect(screen.getByText('user@example.com')).toBeInTheDocument();
  });

  // ── Personal Information ──────────────────────────────────────────────────────

  it('renders Personal Information section', async () => {
    await renderProfile();
    expect(screen.getByText('Personal Information')).toBeInTheDocument();
  });

  it('shows bio placeholder when bio is empty', async () => {
    await renderProfile();
    expect(screen.getByText('Tell others about yourself…')).toBeInTheDocument();
  });

  it('shows bio text when profile returns a bio', async () => {
    api.getProfile.mockResolvedValue({ ok: true, data: { ...EMPTY_PROFILE, bio: 'Software engineer' } });
    await renderProfile();
    await waitFor(() => {
      expect(screen.getByText('Software engineer')).toBeInTheDocument();
    });
  });

  // ── Resources accordion ───────────────────────────────────────────────────────

  it('Resources accordion header is always visible', async () => {
    await renderProfile();
    expect(screen.getByRole('button', { name: /resources/i })).toBeInTheDocument();
  });

  it('shows empty resources message after opening accordion', async () => {
    await renderProfile();

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /^📦 Resources$/i }));

    await waitFor(() => {
      expect(screen.getByText('No resources added yet.')).toBeInTheDocument();
    });
  });

  it('shows resource name after opening accordion', async () => {
    api.getResources.mockResolvedValue({ ok: true, data: [makeResource()] });
    await renderProfile();

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /^📦 Resources/i }));

    await waitFor(() => {
      expect(screen.getByText('Generator')).toBeInTheDocument();
    });
  });

  // ── Expertise accordion (role-conditional) ────────────────────────────────────

  it('expertise accordion is NOT rendered for standard user', async () => {
    await renderProfile(STANDARD_USER);
    expect(
      screen.queryByRole('button', { name: /expertise fields/i })
    ).not.toBeInTheDocument();
  });

  it('expertise accordion IS rendered for expert user', async () => {
    await renderProfile(EXPERT_USER);
    expect(
      screen.getByRole('button', { name: /expertise fields/i })
    ).toBeInTheDocument();
  });

  // ── Resource add form ─────────────────────────────────────────────────────────

  it('resource add form appears after opening accordion and clicking Add Resource', async () => {
    await renderProfile();

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /^📦 Resources/i }));
    await user.click(await screen.findByRole('button', { name: /add resource/i }));

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/e\.g\. Generator/i)).toBeInTheDocument();
      expect(screen.getByPlaceholderText(/e\.g\. Power/i)).toBeInTheDocument();
      expect(screen.getByRole('spinbutton')).toBeInTheDocument();
    });
  });
});
