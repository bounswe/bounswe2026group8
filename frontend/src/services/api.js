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

  // 204 No Content has no body — skip JSON parsing
  if (response.status === 204) {
    return { ok: true, status: 204, data: null };
  }

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
