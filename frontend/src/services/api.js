const API_BASE = 'http://localhost:8000';

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

  // 204 No Content — no body to parse.
  const data = response.status === 204 ? null : await response.json();
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
 * Success → { message, token, refresh, user: { ... } }
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

// ── Help Requests ────────────────────────────────────────────────────────────

/**
 * GET /help-requests/
 * Fetches help requests, optionally filtered by hub and/or category.
 *
 * @param {Object} params
 * @param {number} [params.hub_id]   — filter by hub ID
 * @param {string} [params.category] — filter by category (MEDICAL|FOOD|SHELTER|TRANSPORT)
 *
 * Success → array of help request objects
 */
export function getHelpRequests(params = {}) {
  const query = new URLSearchParams();
  if (params.hub_id) query.append('hub_id', params.hub_id);
  if (params.category) query.append('category', params.category);
  const qs = query.toString();
  return request(`/help-requests/${qs ? `?${qs}` : ''}`, {
    method: 'GET',
  });
}

/** POST /help-requests/ — create a new help request. */
export function createHelpRequest(payload) {
  return request('/help-requests/', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/** PATCH /help-requests/{id}/status/ — update the status of a help request. */
export function updateHelpRequestStatus(id, newStatus) {
  return request(`/help-requests/${id}/status/`, {
    method: 'PATCH',
    body: JSON.stringify({ status: newStatus }),
  });
}

/** GET /help-requests/{id}/ — full detail of a single help request. */
export function getHelpRequest(id) {
  return request(`/help-requests/${id}/`, { method: 'GET' });
}

/** GET /help-requests/{id}/comments/ — list comments on a help request. */
export function getHelpComments(requestId) {
  return request(`/help-requests/${requestId}/comments/`, { method: 'GET' });
}

/** POST /help-requests/{id}/comments/ — add a comment to a help request. */
export function createHelpComment(requestId, content) {
  return request(`/help-requests/${requestId}/comments/`, {
    method: 'POST',
    body: JSON.stringify({ content }),
  });
}

/**
 * GET /help-offers/
 * Fetches help offers, optionally filtered by hub and/or category.
 */
export function getHelpOffers(params = {}) {
  const query = new URLSearchParams();
  if (params.hub_id) query.append('hub_id', params.hub_id);
  if (params.category) query.append('category', params.category);
  const qs = query.toString();
  return request(`/help-offers/${qs ? `?${qs}` : ''}`, { method: 'GET' });
}

/** POST /help-offers/ — create a new help offer. */
export function createHelpOffer(payload) {
  return request('/help-offers/', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/** DELETE /help-offers/{id}/ — delete a help offer (author only). */
export function deleteHelpOffer(id) {
  return request(`/help-offers/${id}/`, { method: 'DELETE' });
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
