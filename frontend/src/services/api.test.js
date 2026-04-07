/**
 * Unit tests for the API service layer.
 *
 * Mocks the global fetch so no real network requests are made.
 * Tests cover resolveImageUrl logic and request/response handling
 * for auth, forum, and help request endpoints.
 */

import {
  resolveImageUrl,
  login,
  register,
  logout,
  getMe,
  getHubs,
  getPosts,
  createPost,
  vote,
  reportPost,
  repost,
  getHelpRequests,
  createHelpRequest,
  getHelpOffers,
  createHelpOffer,
  getResources,
  createResource,
  getExpertiseFields,
  createExpertiseField,
} from './api';

// ── Helpers ────────────────────────────────────────────────────────────────────

/** Create a mock fetch response with json body. */
function mockResponse(body, { ok = true, status = 200 } = {}) {
  return Promise.resolve({
    ok,
    status,
    json: () => Promise.resolve(body),
  });
}

/** Create a 204 No Content mock response. */
function mock204() {
  return Promise.resolve({ ok: true, status: 204 });
}

// ── resolveImageUrl ────────────────────────────────────────────────────────────

describe('resolveImageUrl', () => {
  it('prepends origin to relative paths starting with /', () => {
    const result = resolveImageUrl('/media/uploads/photo.png');
    // API_BASE is 'http://localhost:8000' (from babel transform + default fallback)
    expect(result).toBe('http://localhost:8000/media/uploads/photo.png');
  });

  it('returns absolute https urls unchanged', () => {
    expect(resolveImageUrl('https://cdn.example.com/img.png')).toBe('https://cdn.example.com/img.png');
  });

  it('returns absolute http urls unchanged', () => {
    expect(resolveImageUrl('http://example.com/img.png')).toBe('http://example.com/img.png');
  });

  it('returns null unchanged', () => {
    expect(resolveImageUrl(null)).toBeNull();
  });

  it('returns undefined unchanged', () => {
    expect(resolveImageUrl(undefined)).toBeUndefined();
  });
});

// ── Auth ───────────────────────────────────────────────────────────────────────

describe('login', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  it('sends POST to /login with credentials', async () => {
    global.fetch.mockReturnValueOnce(
      mockResponse({ message: 'Login successful', token: 'tok', refresh: 'ref', user: {} })
    );

    await login({ email: 'a@b.com', password: 'pass' });

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/login'),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ email: 'a@b.com', password: 'pass' }),
      })
    );
  });

  it('returns ok true on success', async () => {
    global.fetch.mockReturnValueOnce(
      mockResponse({ token: 'tok', user: {} }, { ok: true, status: 200 })
    );
    const result = await login({ email: 'a@b.com', password: 'pass' });
    expect(result.ok).toBe(true);
  });

  it('returns ok false on invalid credentials', async () => {
    global.fetch.mockReturnValueOnce(
      mockResponse({ message: 'Invalid email or password' }, { ok: false, status: 400 })
    );
    const result = await login({ email: 'a@b.com', password: 'wrong' });
    expect(result.ok).toBe(false);
    expect(result.data.message).toBe('Invalid email or password');
  });
});

describe('register', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  it('sends POST to /register', async () => {
    global.fetch.mockReturnValueOnce(
      mockResponse({ message: 'Account created successfully', user: {} }, { status: 201 })
    );

    const payload = {
      full_name: 'Test User',
      email: 'test@example.com',
      password: 'StrongPass123!',
      confirm_password: 'StrongPass123!',
      role: 'STANDARD',
    };
    await register(payload);

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/register'),
      expect.objectContaining({ method: 'POST' })
    );
  });

  it('returns 400 for missing required fields', async () => {
    global.fetch.mockReturnValueOnce(
      mockResponse({ errors: { email: ['Required'] } }, { ok: false, status: 400 })
    );
    const result = await register({});
    expect(result.ok).toBe(false);
    expect(result.status).toBe(400);
  });
});

describe('logout', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
    localStorage.setItem('token', 'test-token');
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('sends POST to /logout with auth header', async () => {
    global.fetch.mockReturnValueOnce(
      mockResponse({ message: 'Logged out successfully' })
    );

    await logout();

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/logout'),
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({ Authorization: 'Bearer test-token' }),
      })
    );
  });
});

// ── Forum ──────────────────────────────────────────────────────────────────────

describe('getPosts', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  it('sends GET to /forum/posts/', async () => {
    global.fetch.mockReturnValueOnce(mockResponse([]));
    await getPosts();
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/forum/posts/'),
      expect.objectContaining({ method: 'GET' })
    );
  });

  it('appends forum_type query param when provided', async () => {
    global.fetch.mockReturnValueOnce(mockResponse([]));
    await getPosts({ forumType: 'URGENT' });
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('forum_type=URGENT'),
      expect.anything()
    );
  });

  it('appends hub query param when provided', async () => {
    global.fetch.mockReturnValueOnce(mockResponse([]));
    await getPosts({ hub: 5 });
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('hub=5'),
      expect.anything()
    );
  });
});

describe('vote', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
    localStorage.setItem('token', 'test-token');
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('sends POST to vote endpoint with vote_type UP', async () => {
    global.fetch.mockReturnValueOnce(mockResponse({ vote: 'UP' }));
    await vote(42, 'UP');
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/forum/posts/42/vote/'),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ vote_type: 'UP' }),
      })
    );
  });

  it('sends POST with vote_type DOWN', async () => {
    global.fetch.mockReturnValueOnce(mockResponse({ vote: 'DOWN' }));
    await vote(42, 'DOWN');
    expect(fetch).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ body: JSON.stringify({ vote_type: 'DOWN' }) })
    );
  });
});

describe('reportPost', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
    localStorage.setItem('token', 'test-token');
  });

  it('sends POST to report endpoint with reason', async () => {
    global.fetch.mockReturnValueOnce(mockResponse({ detail: 'Reported' }));
    await reportPost(7, 'SPAM');
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/forum/posts/7/report/'),
      expect.objectContaining({ body: JSON.stringify({ reason: 'SPAM' }) })
    );
  });
});

describe('repost', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
    localStorage.setItem('token', 'test-token');
  });

  it('sends POST to repost endpoint with hub id', async () => {
    global.fetch.mockReturnValueOnce(mockResponse({ id: 99 }));
    await repost(5, 3);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/forum/posts/5/repost/'),
      expect.objectContaining({ body: JSON.stringify({ hub: 3 }) })
    );
  });

  it('sends empty body when no hub specified', async () => {
    global.fetch.mockReturnValueOnce(mockResponse({ id: 99 }));
    await repost(5, null);
    expect(fetch).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ body: JSON.stringify({}) })
    );
  });
});

// ── Help Requests ──────────────────────────────────────────────────────────────

describe('getHelpRequests', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  it('sends GET to /help-requests/', async () => {
    global.fetch.mockReturnValueOnce(mockResponse([]));
    await getHelpRequests();
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/help-requests/'),
      expect.objectContaining({ method: 'GET' })
    );
  });

  it('filters by category when provided', async () => {
    global.fetch.mockReturnValueOnce(mockResponse([]));
    await getHelpRequests({ category: 'MEDICAL' });
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('category=MEDICAL'),
      expect.anything()
    );
  });

  it('filters by hub_id when provided', async () => {
    global.fetch.mockReturnValueOnce(mockResponse([]));
    await getHelpRequests({ hub_id: 2 });
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('hub_id=2'),
      expect.anything()
    );
  });
});

describe('createHelpRequest', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
    localStorage.setItem('token', 'test-token');
  });

  it('sends POST to /help-requests/ with payload', async () => {
    global.fetch.mockReturnValueOnce(mockResponse({ id: 1 }, { status: 201 }));

    const payload = {
      category: 'MEDICAL',
      urgency: 'HIGH',
      title: 'Need help',
      description: 'Please respond',
    };
    await createHelpRequest(payload);

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/help-requests/'),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify(payload),
      })
    );
  });
});

// ── Profile — Resources & Expertise ───────────────────────────────────────────

describe('getResources', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
    localStorage.setItem('token', 'test-token');
  });

  it('sends GET to /resources', async () => {
    global.fetch.mockReturnValueOnce(mockResponse([]));
    await getResources();
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/resources'),
      expect.objectContaining({ method: 'GET' })
    );
  });
});

describe('createResource', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
    localStorage.setItem('token', 'test-token');
  });

  it('sends POST to /resources with payload', async () => {
    global.fetch.mockReturnValueOnce(mockResponse({ id: 1 }, { status: 201 }));
    await createResource({ name: 'First Aid Kit', category: 'MEDICAL', quantity: 1 });
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/resources'),
      expect.objectContaining({ method: 'POST' })
    );
  });
});

describe('getExpertiseFields', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
    localStorage.setItem('token', 'test-token');
  });

  it('sends GET to /expertise', async () => {
    global.fetch.mockReturnValueOnce(mockResponse([]));
    await getExpertiseFields();
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/expertise'),
      expect.objectContaining({ method: 'GET' })
    );
  });
});

describe('createExpertiseField', () => {
  beforeEach(() => {
    global.fetch = jest.fn();
    localStorage.setItem('token', 'test-token');
  });

  it('sends POST to /expertise with payload', async () => {
    global.fetch.mockReturnValueOnce(mockResponse({ id: 1 }, { status: 201 }));
    await createExpertiseField({ field: 'Medical Doctor', certification_level: 'ADVANCED' });
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/expertise'),
      expect.objectContaining({ method: 'POST' })
    );
  });
});
