const API_BASE = 'http://localhost:8000';

/**
 * Generic fetch wrapper that handles JSON and auth headers.
 */
async function request(endpoint, options = {}) {
  const token = localStorage.getItem('token');
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Token ${token}` } : {}),
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
