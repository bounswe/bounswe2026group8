/**
 * Integration tests for HelpRequestsPage.
 *
 * Covers: help request listing, category filtering, help offers tab,
 * empty states, and authorization guards (section 6.5 of the test plan).
 *
 * Note: HelpRequestsPage renders null until the auth context resolves the user.
 * Tests set a token in localStorage and mock getMe so the component renders.
 */

import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import HelpRequestsPage from './HelpRequestsPage';
import { AuthProvider } from '../context/AuthContext';

jest.mock('../services/api');
import * as api from '../services/api';

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => jest.fn(),
}));

/** Standard user fixture with a hub — used in most tests. */
const STANDARD_USER = {
  id: 1,
  email: 'user@example.com',
  role: 'STANDARD',
  hub: { id: 2, name: 'Istanbul', slug: 'istanbul' },
};

/**
 * Renders HelpRequestsPage with a fully authenticated user.
 * Sets a token in localStorage so AuthContext calls getMe and populates the user.
 */
async function renderPage(userData = STANDARD_USER) {
  localStorage.setItem('token', 'test-token');
  api.getMe.mockResolvedValue({ ok: true, status: 200, data: userData });

  await act(async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <HelpRequestsPage />
        </AuthProvider>
      </MemoryRouter>
    );
  });

  // Wait for auth context to load and page to become visible
  await waitFor(() => {
    expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
  });
}

const makeRequest = (overrides = {}) => ({
  id: 1,
  title: 'Need medical help',
  category: 'MEDICAL',
  urgency: 'HIGH',
  status: 'OPEN',
  comment_count: 0,
  created_at: '2026-01-01T00:00:00Z',
  hub: 2,
  hub_name: 'Istanbul',
  author: {
    id: 99,
    full_name: 'Other User',
    email: 'other@example.com',
    role: 'STANDARD',
    hub: null,
    neighborhood_address: null,
    expertise_field: null,
  },
  ...overrides,
});

const makeOffer = (overrides = {}) => ({
  id: 1,
  category: 'FOOD',
  skill_or_resource: 'Canned goods',
  description: 'I have extra food',
  availability: 'Available now',
  hub: 2,
  hub_name: 'Istanbul',
  author: { id: 99, full_name: 'Helper', email: 'helper@example.com', role: 'STANDARD', hub: null },
  created_at: '2026-01-01T00:00:00Z',
  ...overrides,
});

describe('HelpRequestsPage — Requests tab', () => {
  beforeEach(() => {
    localStorage.clear();
    api.getHubs.mockResolvedValue({ ok: true, data: [] });
    api.getHelpOffers.mockResolvedValue({ ok: true, data: [] });
  });

  it('shows help requests loaded from API', async () => {
    api.getHelpRequests.mockResolvedValue({
      ok: true,
      data: [makeRequest({ title: 'Need medical help' })],
    });

    await renderPage();

    await waitFor(() => {
      expect(screen.getByText('Need medical help')).toBeInTheDocument();
    });
  });

  it('shows Requests and Offers tab buttons', async () => {
    api.getHelpRequests.mockResolvedValue({ ok: true, data: [] });

    await renderPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Requests' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Offers' })).toBeInTheDocument();
    });
  });

  it('fetches requests filtered by category when filter button is clicked', async () => {
    api.getHelpRequests.mockResolvedValue({ ok: true, data: [] });
    const user = userEvent.setup();

    await renderPage();

    // Click the "Medical" category filter button
    const medicalBtn = await screen.findByRole('button', { name: /medical/i });
    await user.click(medicalBtn);

    await waitFor(() => {
      expect(api.getHelpRequests).toHaveBeenCalledWith(
        expect.objectContaining({ category: 'MEDICAL' })
      );
    });
  });

  it('shows OPEN status label on open request', async () => {
    api.getHelpRequests.mockResolvedValue({
      ok: true,
      data: [makeRequest({ status: 'OPEN' })],
    });

    await renderPage();

    await waitFor(() => {
      expect(screen.getByText(/open/i)).toBeInTheDocument();
    });
  });

  it('shows EXPERT_RESPONDING status label', async () => {
    api.getHelpRequests.mockResolvedValue({
      ok: true,
      data: [makeRequest({ status: 'EXPERT_RESPONDING', title: 'Expert test' })],
    });

    await renderPage();

    await waitFor(() => {
      expect(screen.getByText(/expert responding/i)).toBeInTheDocument();
    });
  });

  it('shows urgency label on requests', async () => {
    api.getHelpRequests.mockResolvedValue({
      ok: true,
      data: [makeRequest({ urgency: 'HIGH' })],
    });

    await renderPage();

    await waitFor(() => {
      expect(screen.getByText(/high/i)).toBeInTheDocument();
    });
  });
});

describe('HelpRequestsPage — Offers tab', () => {
  beforeEach(() => {
    localStorage.clear();
    api.getHubs.mockResolvedValue({ ok: true, data: [] });
    api.getHelpRequests.mockResolvedValue({ ok: true, data: [] });
  });

  it('shows offers when switching to offers tab', async () => {
    api.getHelpOffers.mockResolvedValue({
      ok: true,
      data: [makeOffer({ skill_or_resource: 'Canned goods' })],
    });

    const user = userEvent.setup();
    await renderPage();

    const offersTab = await screen.findByRole('button', { name: 'Offers' });
    await user.click(offersTab);

    await waitFor(() => {
      expect(screen.getByText(/canned goods/i)).toBeInTheDocument();
    });
  });
});

describe('HelpRequestsPage — Authorization', () => {
  beforeEach(() => {
    localStorage.clear();
    api.getHubs.mockResolvedValue({ ok: true, data: [] });
    api.getHelpRequests.mockResolvedValue({ ok: true, data: [] });
    api.getHelpOffers.mockResolvedValue({ ok: true, data: [] });
  });

  it('shows + New Request button for authenticated user', async () => {
    await renderPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /new request/i })).toBeInTheDocument();
    });
  });
});
