const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8000';

/**
 * Resolves an image URL so relative paths served by the backend are loadable.
 * - Relative paths (e.g. "/media/uploads/abc.png") get the API base prepended.
 * - Absolute URLs are returned as-is.
 */
export function resolveImageUrl(url) {
  if (!url) return url;
  if (!url.startsWith('/')) return url;
  // API_BASE may be a relative path like '/api' (production) or an absolute URL
  // like 'http://localhost:8000' (local dev). Media is always served from the
  // same origin, so we must NOT prepend '/api' to '/media/...' paths.
  if (API_BASE.startsWith('http')) {
    return `${new URL(API_BASE).origin}${url}`;
  }
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

  // 204 No Content has no body — skip JSON parsing
  if (response.status === 204) {
    return { ok: true, status: 204, data: null };
  }

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

export function getProfile() {
  return request('/profile', {
    method: 'GET',
  });
}

export function updateProfile(payload) {
  return request('/profile', {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function getSettings() {
  return request('/settings', {
    method: 'GET',
  });
}

export function updateSettings(payload) {
  return request('/settings', {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

// --------------- Resources ---------------

export function getResources() {
  return request('/resources', { method: 'GET' });
}

export function createResource(payload) {
  return request('/resources', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updateResource(id, payload) {
  return request(`/resources/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function deleteResource(id) {
  return request(`/resources/${id}`, { method: 'DELETE' });
}

// --------------- Expertise Categories (public) ---------------

/** GET /expertise-categories/ — no auth needed. */
export function getExpertiseCategories() {
  return request('/expertise-categories/', { method: 'GET' });
}

// --------------- Expertise Fields (EXPERT only) ---------------

export function getExpertiseFields() {
  return request('/expertise', { method: 'GET' });
}

export function createExpertiseField(payload) {
  return request('/expertise', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updateExpertiseField(id, payload) {
  return request(`/expertise/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function deleteExpertiseField(id) {
  return request(`/expertise/${id}`, { method: 'DELETE' });
}

export function getUserProfile(id) {
  return request(`/users/${id}/`, { method: 'GET' });
}

export function updateMe(data) {
  return request('/me', {
    method: 'PATCH',
    body: JSON.stringify(data),
  });
}

/**
 * PATCH /me with a country/city/district payload.
 * The backend resolves (or creates) the matching Hub and assigns it.
 */
export function updateHubLocation({ country, city, district = '' }) {
  return updateMe({ country, city, district });
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
  if (params.author) query.append('author', params.author);
  if (params.expertise_match) query.append('expertise_match', 'true');
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

/** POST /help-requests/{id}/take-on/ — expert takes responsibility. */
export function takeOnHelpRequest(id) {
  return request(`/help-requests/${id}/take-on/`, { method: 'POST' });
}

/** POST /help-requests/{id}/release/ — assigned expert releases responsibility. */
export function releaseHelpRequest(id) {
  return request(`/help-requests/${id}/release/`, { method: 'POST' });
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
  if (params.author) query.append('author', params.author);
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

/** DELETE /help-requests/{id}/ — delete a help request (author only). */
export function deleteHelpRequest(id) {
  return request(`/help-requests/${id}/`, { method: 'DELETE' });
}

/** DELETE /help-requests/comments/{id}/ — delete a help request comment (author only). */
export function deleteHelpComment(commentId) {
  return request(`/help-requests/comments/${commentId}/`, { method: 'DELETE' });
}

// ── Forum ─────────────────────────────────────────────────────────────────────

export function getPosts({ hub, forumType, author, authorRole } = {}) {
  const params = new URLSearchParams();
  if (hub) params.set('hub', hub);
  if (forumType) params.set('forum_type', forumType);
  if (author) params.set('author', author);
  if (authorRole) params.set('author_role', authorRole);
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

export async function uploadHelpRequestImages(files) {
  const token = localStorage.getItem('token');
  const formData = new FormData();
  files.forEach((f) => formData.append('images', f));

  const response = await fetch(`${API_BASE}/help-requests/upload/`, {
    method: 'POST',
    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: formData,
  });

  const data = await response.json();
  return { ok: response.ok, status: response.status, data };
}

// --------------- Mesh (offline messages archive) ---------------

/** GET /mesh-messages/ — list top-level posts uploaded from the offline mesh. */
export function getMeshPosts() {
  return request('/mesh-messages/', { method: 'GET' });
}

/** GET /mesh-messages/{id}/comments/ — list comments on an offline post. */
export function getMeshComments(postId) {
  return request(`/mesh-messages/${postId}/comments/`, { method: 'GET' });
}

// ── Staff: Admin (user / hub / audit) ─────────────────────────────────────────

function buildQuery(params = {}) {
  const usp = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '') return;
    usp.append(key, value);
  });
  const qs = usp.toString();
  return qs ? `?${qs}` : '';
}

export function listStaffUsers(params = {}) {
  return request(`/staff/users/${buildQuery(params)}`, { method: 'GET' });
}

export function updateStaffRole(userId, staffRole, reason = '') {
  return request(`/staff/users/${userId}/staff-role/`, {
    method: 'PATCH',
    body: JSON.stringify({ staff_role: staffRole, reason }),
  });
}

export function updateAccountStatus(userId, isActive, reason) {
  return request(`/staff/users/${userId}/status/`, {
    method: 'PATCH',
    body: JSON.stringify({ is_active: isActive, reason }),
  });
}

export function listStaffHubs() {
  return request('/staff/hubs/', { method: 'GET' });
}

export function createStaffHub(payload) {
  return request('/staff/hubs/', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updateStaffHub(hubId, payload) {
  return request(`/staff/hubs/${hubId}/`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export function deleteStaffHub(hubId, { confirm = false } = {}) {
  return request(`/staff/hubs/${hubId}/`, {
    method: 'DELETE',
    body: JSON.stringify({ confirm }),
  });
}

export function listAuditLogs(params = {}) {
  return request(`/staff/audit-logs/${buildQuery(params)}`, { method: 'GET' });
}

// ── Staff: Forum moderation (moderator / admin) ───────────────────────────────

export function listForumModerationPosts(params = {}) {
  return request(`/forum/moderation/posts/${buildQuery(params)}`, { method: 'GET' });
}

export function moderateForumPost(postId, action, reason = '') {
  return request(`/forum/posts/${postId}/moderation/`, {
    method: 'PATCH',
    body: JSON.stringify({ action, reason }),
  });
}

export function moderationDeleteForumComment(commentId, reason = '') {
  return request(`/forum/moderation/comments/${commentId}/`, {
    method: 'DELETE',
    body: JSON.stringify({ reason }),
  });
}

// ── Staff: Help moderation (moderator / admin) ────────────────────────────────

export function listHelpRequestModeration(params = {}) {
  return request(`/help-requests/moderation/${buildQuery(params)}`, { method: 'GET' });
}

export function listHelpOfferModeration(params = {}) {
  return request(`/help-offers/moderation/${buildQuery(params)}`, { method: 'GET' });
}

export function moderationDeleteHelpRequest(id, reason = '') {
  return request(`/help-requests/${id}/`, {
    method: 'DELETE',
    body: JSON.stringify({ reason }),
  });
}

export function moderationDeleteHelpOffer(id, reason = '') {
  return request(`/help-offers/${id}/`, {
    method: 'DELETE',
    body: JSON.stringify({ reason }),
  });
}

export function moderationDeleteHelpComment(id, reason = '') {
  return request(`/help-requests/comments/${id}/`, {
    method: 'DELETE',
    body: JSON.stringify({ reason }),
  });
}

// ── Staff: Expertise verification (verification coordinator / admin) ──────────

export function listExpertiseVerifications(params = {}) {
  return request(`/staff/expertise-verifications/${buildQuery(params)}`, { method: 'GET' });
}

export function decideExpertiseVerification(expertiseId, decision, note = '') {
  return request(`/staff/expertise-verifications/${expertiseId}/decision/`, {
    method: 'PATCH',
    body: JSON.stringify({ status: decision, note }),
  });
}

// ── Badges ───────────────────────────────────────────────────────────────────

/**
 * GET /api/badges/my-badges/
 * Fetches the current user's badge progress.
 * Requires Authorization header.
 * * Success → array of badge objects
 */
export function getMyBadges() {
  return request('/api/badges/my-badges/', { 
    method: 'GET' 
  });
}

/**
 * GET /api/badges/users/{id}/
 * Fetches the badge progress for a specific user.
 */
export function getUserBadges(userId) {
  return request(`/api/badges/users/${userId}/`, { 
    method: 'GET' 
  });
}
