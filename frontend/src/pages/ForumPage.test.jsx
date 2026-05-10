/**
 * Integration tests for ForumPage.
 *
 * Covers: post list rendering, forum type tab filters, empty states,
 * vote toggle behavior, and new post button visibility.
 */

import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import ForumPage from './ForumPage';
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
  role: 'STANDARD',
  hub: { id: 2, name: 'Istanbul', slug: 'istanbul' },
};
const USER_NO_HUB = { ...STANDARD_USER, hub: null };

const makePost = (overrides = {}) => ({
  id: 1,
  title: 'Test Post',
  content: 'Content here',
  forum_type: 'GLOBAL',
  hub_name: 'Istanbul',
  upvote_count: 0,
  downvote_count: 0,
  comment_count: 0,
  user_vote: null,
  user_has_reposted: false,
  repost_count: 0,
  reposted_from: null,
  image_urls: [],
  created_at: '2026-01-01T00:00:00Z',
  author: { id: 99, full_name: 'Other User', role: 'STANDARD', email: 'o@example.com', profile: null },
  ...overrides,
});

/**
 * Renders ForumPage inside MemoryRouter + AuthProvider.
 * Pass null for userData to render as unauthenticated.
 */
async function renderForum(userData = STANDARD_USER, initialTab = null) {
  if (userData) {
    localStorage.setItem('token', 'test-token');
    api.getMe.mockResolvedValue({ ok: true, status: 200, data: userData });
  }
  const entry = initialTab ? `/?tab=${initialTab}` : '/';
  await act(async () => {
    render(
      <MemoryRouter initialEntries={[entry]}>
        <AuthProvider>
          <ForumPage />
        </AuthProvider>
      </MemoryRouter>
    );
  });
  await waitFor(() => {
    expect(screen.queryByText(/loading posts/i)).not.toBeInTheDocument();
  });
}

describe('ForumPage', () => {
  beforeEach(() => {
    localStorage.clear();
    mockNavigate.mockClear();
    api.getMe.mockResolvedValue({ ok: false, status: 401, data: null });
    api.getHubs.mockResolvedValue({ ok: true, data: [] });
    api.vote.mockResolvedValue({ ok: true });
    api.getPosts.mockResolvedValue({ ok: true, data: [] });
  });

  // ── Rendering ────────────────────────────────────────────────────────────────

  it('renders post titles from API', async () => {
    api.getPosts.mockResolvedValue({ ok: true, data: [makePost()] });
    await renderForum();
    expect(screen.getByText('Test Post')).toBeInTheDocument();
  });

  it('shows no-global empty state when GLOBAL tab has no posts', async () => {
    await renderForum();
    expect(screen.getByText(/no global posts yet/i)).toBeInTheDocument();
  });

  it('shows select-hub empty state for STANDARD tab when user has no hub', async () => {
    await renderForum(USER_NO_HUB, 'STANDARD');
    expect(screen.getByText(/select a hub to see/i)).toBeInTheDocument();
    expect(api.getPosts).not.toHaveBeenCalledWith(
      expect.objectContaining({ forumType: 'STANDARD' })
    );
  });

  // ── Filters ──────────────────────────────────────────────────────────────────

  it('calls getPosts with STANDARD forumType and hub id after tab switch', async () => {
    await renderForum();

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /standard/i }));

    await waitFor(() => {
      expect(api.getPosts).toHaveBeenCalledWith({ hub: 2, forumType: 'STANDARD' });
    });
  });

  // ── Voting ───────────────────────────────────────────────────────────────────

  it('upvote increments count and adds vote-active class', async () => {
    api.getPosts.mockResolvedValue({ ok: true, data: [makePost({ upvote_count: 3 })] });
    await renderForum();

    const user = userEvent.setup();
    await user.click(screen.getByTitle('Upvote'));

    await waitFor(() => {
      expect(screen.getByTitle('Upvote')).toHaveTextContent('4');
      expect(screen.getByTitle('Upvote')).toHaveClass('vote-active');
    });
    expect(api.vote).toHaveBeenCalledWith(1, 'UP');
  });

  it('toggling same vote twice removes it', async () => {
    api.getPosts.mockResolvedValue({ ok: true, data: [makePost({ upvote_count: 3 })] });
    await renderForum();

    const user = userEvent.setup();
    await user.click(screen.getByTitle('Upvote'));
    await waitFor(() => expect(screen.getByTitle('Upvote')).toHaveTextContent('4'));

    await user.click(screen.getByTitle('Upvote'));
    await waitFor(() => {
      expect(screen.getByTitle('Upvote')).toHaveTextContent('3');
      expect(screen.getByTitle('Upvote')).not.toHaveClass('vote-active');
    });
  });

  it('switching UP to DOWN adjusts both counts', async () => {
    api.getPosts.mockResolvedValue({
      ok: true,
      data: [makePost({ upvote_count: 3, downvote_count: 1, user_vote: 'UP' })],
    });
    await renderForum();

    const user = userEvent.setup();
    await user.click(screen.getByTitle('Downvote'));

    await waitFor(() => {
      expect(screen.getByTitle('Upvote')).toHaveTextContent('2');
      expect(screen.getByTitle('Downvote')).toHaveTextContent('2');
    });
  });

  // ── Authorization ─────────────────────────────────────────────────────────────

  it('shows + New Post button for authenticated user', async () => {
    await renderForum();
    expect(screen.getByRole('button', { name: /new post/i })).toBeInTheDocument();
  });

  it('hides + New Post button for unauthenticated user', async () => {
    await renderForum(null);
    expect(screen.queryByRole('button', { name: /new post/i })).not.toBeInTheDocument();
  });
});
