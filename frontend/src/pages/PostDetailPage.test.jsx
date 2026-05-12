/**
 * Integration tests for PostDetailPage.
 *
 * Covers: post and comment rendering, vote toggle behavior,
 * report modal flow, and comment submission.
 */

import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import PostDetailPage from './PostDetailPage';
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
  useParams: () => ({ id: '1' }),
}));

/** Authenticated viewer — different id from the post author (99). */
const VIEWER = {
  id: 1,
  email: 'viewer@example.com',
  role: 'STANDARD',
  hub: { id: 2, name: 'Istanbul', slug: 'istanbul' },
};

const makePost = (overrides = {}) => ({
  id: 1,
  title: 'Forum Post',
  content: 'Post body text.',
  forum_type: 'GLOBAL',
  hub_name: 'Istanbul',
  upvote_count: 5,
  downvote_count: 2,
  comment_count: 0,
  user_vote: null,
  user_has_reposted: false,
  repost_count: 0,
  reposted_from: null,
  image_urls: [],
  created_at: '2026-01-01T00:00:00Z',
  updated_at: '2026-01-01T00:00:00Z',
  author: { id: 99, full_name: 'Author Name', role: 'STANDARD', email: 'author@example.com', profile: null },
  ...overrides,
});

const makeComment = (overrides = {}) => ({
  id: 10,
  content: 'Nice post!',
  created_at: '2026-01-02T00:00:00Z',
  author: { id: 99, full_name: 'Commenter', role: 'STANDARD', profile: null },
  ...overrides,
});

async function renderPage(userData = VIEWER) {
  localStorage.setItem('token', 'test-token');
  api.getMe.mockResolvedValue({ ok: true, status: 200, data: userData });
  await act(async () => {
    render(
      <MemoryRouter>
        <AuthProvider>
          <PostDetailPage />
        </AuthProvider>
      </MemoryRouter>
    );
  });
  await waitFor(() => {
    expect(screen.queryByText(/loading/i)).not.toBeInTheDocument();
  });
}

describe('PostDetailPage — rendering', () => {
  beforeEach(() => {
    localStorage.clear();
    mockNavigate.mockClear();
    api.getMe.mockResolvedValue({ ok: false, status: 401, data: null });
    api.getHubs.mockResolvedValue({ ok: true, data: [] });
    api.getPost.mockResolvedValue({ ok: true, data: makePost() });
    api.getComments.mockResolvedValue({ ok: true, data: [] });
    api.vote.mockResolvedValue({ ok: true });
    api.reportPost.mockResolvedValue({ ok: true, data: {} });
    api.createComment.mockResolvedValue({
      ok: true,
      data: makeComment({ id: 20, content: 'My comment' }),
    });
  });

  it('renders post title and content', async () => {
    await renderPage();
    expect(screen.getByText('Forum Post')).toBeInTheDocument();
    expect(screen.getByText('Post body text.')).toBeInTheDocument();
  });

  it('shows empty comment state when no comments', async () => {
    await renderPage();
    expect(screen.getByText(/no comments yet/i)).toBeInTheDocument();
  });

  it('renders comment list when comments exist', async () => {
    api.getComments.mockResolvedValue({ ok: true, data: [makeComment()] });
    await renderPage();
    expect(screen.getByText('Nice post!')).toBeInTheDocument();
  });
});

describe('PostDetailPage — voting', () => {
  beforeEach(() => {
    localStorage.clear();
    mockNavigate.mockClear();
    api.getMe.mockResolvedValue({ ok: false, status: 401, data: null });
    api.getHubs.mockResolvedValue({ ok: true, data: [] });
    api.getPost.mockResolvedValue({ ok: true, data: makePost() });
    api.getComments.mockResolvedValue({ ok: true, data: [] });
    api.vote.mockResolvedValue({ ok: true });
    api.reportPost.mockResolvedValue({ ok: true, data: {} });
    api.createComment.mockResolvedValue({
      ok: true,
      data: makeComment({ id: 20, content: 'My comment' }),
    });
  });

  it('upvote increments count and adds vote-active class', async () => {
    await renderPage();

    const user = userEvent.setup();
    await user.click(screen.getByTitle('Upvote'));

    await waitFor(() => {
      expect(screen.getByTitle('Upvote')).toHaveTextContent('6');
      expect(screen.getByTitle('Upvote')).toHaveClass('vote-active');
    });
    expect(api.vote).toHaveBeenCalledWith('1', 'UP');
  });

  it('toggling same vote twice removes it', async () => {
    await renderPage();

    const user = userEvent.setup();
    await user.click(screen.getByTitle('Upvote'));
    await waitFor(() => expect(screen.getByTitle('Upvote')).toHaveTextContent('6'));

    await user.click(screen.getByTitle('Upvote'));
    await waitFor(() => {
      expect(screen.getByTitle('Upvote')).toHaveTextContent('5');
      expect(screen.getByTitle('Upvote')).not.toHaveClass('vote-active');
    });
  });

  it('switching UP to DOWN adjusts both counts', async () => {
    api.getPost.mockResolvedValue({
      ok: true,
      data: makePost({ upvote_count: 5, downvote_count: 2, user_vote: 'UP' }),
    });
    await renderPage();

    const user = userEvent.setup();
    await user.click(screen.getByTitle('Downvote'));

    await waitFor(() => {
      expect(screen.getByTitle('Upvote')).toHaveTextContent('4');
      expect(screen.getByTitle('Downvote')).toHaveTextContent('3');
    });
  });
});

describe('PostDetailPage — report modal', () => {
  beforeEach(() => {
    localStorage.clear();
    mockNavigate.mockClear();
    api.getMe.mockResolvedValue({ ok: false, status: 401, data: null });
    api.getHubs.mockResolvedValue({ ok: true, data: [] });
    api.getPost.mockResolvedValue({ ok: true, data: makePost() });
    api.getComments.mockResolvedValue({ ok: true, data: [] });
    api.vote.mockResolvedValue({ ok: true });
    api.reportPost.mockResolvedValue({ ok: true, data: {} });
    api.createComment.mockResolvedValue({
      ok: true,
      data: makeComment({ id: 20, content: 'My comment' }),
    });
  });

  it('report button not visible when user is the post author', async () => {
    // Author id is 99; render as user id=99 (same as author)
    await renderPage({ ...VIEWER, id: 99 });
    expect(screen.queryByRole('button', { name: /report/i })).not.toBeInTheDocument();
  });

  it('report modal opens on Report button click', async () => {
    await renderPage();

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /report/i }));

    await waitFor(() => {
      expect(screen.getByText(/report this post/i)).toBeInTheDocument();
    });
  });

  it('report modal submits with selected reason', async () => {
    await renderPage();

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /report/i }));
    await waitFor(() => expect(screen.getByLabelText(/reason/i)).toBeInTheDocument());

    await user.selectOptions(screen.getByLabelText(/reason/i), 'ABUSE');
    await user.click(screen.getByRole('button', { name: /submit/i }));

    await waitFor(() => {
      expect(api.reportPost).toHaveBeenCalledWith('1', 'ABUSE');
    });
  });
});

describe('PostDetailPage — comments', () => {
  beforeEach(() => {
    localStorage.clear();
    mockNavigate.mockClear();
    api.getMe.mockResolvedValue({ ok: false, status: 401, data: null });
    api.getHubs.mockResolvedValue({ ok: true, data: [] });
    api.getPost.mockResolvedValue({ ok: true, data: makePost() });
    api.getComments.mockResolvedValue({ ok: true, data: [] });
    api.vote.mockResolvedValue({ ok: true });
    api.reportPost.mockResolvedValue({ ok: true, data: {} });
    api.createComment.mockResolvedValue({
      ok: true,
      data: makeComment({ id: 20, content: 'My comment' }),
    });
  });

  it('comment form is shown for authenticated user', async () => {
    await renderPage();
    expect(screen.getByPlaceholderText(/write a comment/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /post comment/i })).toBeInTheDocument();
  });

  it('submitting a comment calls createComment and appends the text', async () => {
    await renderPage();

    const user = userEvent.setup();
    await user.type(screen.getByPlaceholderText(/write a comment/i), 'My comment');
    await user.click(screen.getByRole('button', { name: /post comment/i }));

    await waitFor(() => {
      expect(api.createComment).toHaveBeenCalledWith('1', 'My comment');
    });
    expect(screen.getByText('My comment')).toBeInTheDocument();
  });
});
