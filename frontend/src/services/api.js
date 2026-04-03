const API_BASE = 'http://localhost:8000';

/**
 * Resolves an image URL so relative paths served by the backend are loadable.
 * - Relative paths (e.g. "/media/uploads/abc.png") get the API base prepended.
 * - Absolute URLs are returned as-is.
 */
export function resolveImageUrl(url) {
  if (url.startsWith('/')) return `${API_BASE}${url}`;
  return url;
}

/**
 * Generic fetch wrapper that handles JSON and auth headers.
 */
async function request(endpoint, options = {}) {
  const token = localStorage.getItem('token');
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  });

  const data = await response.json();
  return { ok: response.ok, status: response.status, data };
}

/**
 * POST /register
 * Request body matches AUTH_IMPLEMENTATION_GUIDE Section 8.1:
 *   full_name, email, password, confirm_password, role,
 *   neighborhood_address (optional), expertise_field (required if EXPERT)
 *
 * Success  → { message, user: { id, full_name, email, role, neighborhood_address, expertise_field } }
 * Failure  → { message, errors: { ... } }
 */
export function register(payload) {
  return request('/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/**
 * POST /login
 * Request body: { email, password }
 *
 * Success → { message, token, user: { id, full_name, email, role, neighborhood_address, expertise_field } }
 * Failure → { message: "Invalid email or password" }
 */
export function login(payload) {
  return request('/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/**
 * POST /logout
 * Requires Authorization header.
 *
 * Success → { message: "Logged out successfully" }
 */
export function logout() {
  return request('/logout', {
    method: 'POST',
  });
}

/**
 * GET /me
 * Requires Authorization header.
 *
 * Success → { id, full_name, email, role, neighborhood_address, expertise_field }
 */
export function getMe() {
  return request('/me', {
    method: 'GET',
  });
}

/**
 * GET /hubs/
 * Public — returns list of all hubs.
 */
export function getHubs() {
  return request('/hubs/', {
    method: 'GET',
  });
}

// ── Forum ─────────────────────────────────────────────────────────────────────

export function getPosts({ hub, forumType } = {}) {
  const params = new URLSearchParams();
  if (hub) params.set('hub', hub);
  if (forumType) params.set('forum_type', forumType);
  const qs = params.toString();
  return request(`/forum/posts/${qs ? `?${qs}` : ''}`, { method: 'GET' });
}

export function getPost(id) {
  return request(`/forum/posts/${id}/`, { method: 'GET' });
}

export function createPost(payload) {
  return request('/forum/posts/', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updatePost(id, payload) {
  return request(`/forum/posts/${id}/`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export function deletePost(id) {
  return request(`/forum/posts/${id}/`, { method: 'DELETE' });
}

export function getComments(postId) {
  return request(`/forum/posts/${postId}/comments/`, { method: 'GET' });
}

export function createComment(postId, content) {
  return request(`/forum/posts/${postId}/comments/`, {
    method: 'POST',
    body: JSON.stringify({ content }),
  });
}

export function deleteComment(commentId) {
  return request(`/forum/comments/${commentId}/`, { method: 'DELETE' });
}

export function vote(postId, voteType) {
  return request(`/forum/posts/${postId}/vote/`, {
    method: 'POST',
    body: JSON.stringify({ vote_type: voteType }),
  });
}

export function reportPost(postId, reason) {
  return request(`/forum/posts/${postId}/report/`, {
    method: 'POST',
    body: JSON.stringify({ reason }),
  });
}

export function repost(postId, hubId) {
  return request(`/forum/posts/${postId}/repost/`, {
    method: 'POST',
    body: JSON.stringify(hubId ? { hub: hubId } : {}),
  });
}

export async function uploadImages(files) {
  const token = localStorage.getItem('token');
  const formData = new FormData();
  files.forEach((f) => formData.append('images', f));

  const response = await fetch(`${API_BASE}/forum/upload/`, {
    method: 'POST',
    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: formData,
  });

  const data = await response.json();
  return { ok: response.ok, status: response.status, data };
}
