/**
 * Tests for the StaffRoute guard.
 */

import { render, screen, act } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import StaffRoute from './StaffRoute';
import { AuthProvider } from '../context/AuthContext';
import { STAFF_ROLE } from '../utils/staffRoles';

jest.mock('../services/api');
import * as api from '../services/api';

function renderWithAuth(initialPath, allowedStaffRoles) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <AuthProvider>
        <Routes>
          <Route
            path="/staff"
            element={
              <StaffRoute allowedStaffRoles={allowedStaffRoles}>
                <div>STAFF AREA</div>
              </StaffRoute>
            }
          />
          <Route path="/signin" element={<div>SIGN IN</div>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('StaffRoute', () => {
  beforeEach(() => {
    localStorage.clear();
    api.getHubs.mockResolvedValue({ ok: true, data: [] });
    api.getMe.mockReset();
  });

  it('redirects unauthenticated users to /signin', async () => {
    api.getMe.mockResolvedValue({ ok: false, status: 401, data: null });
    await act(async () => {
      renderWithAuth('/staff', [STAFF_ROLE.MODERATOR]);
    });
    expect(screen.getByText('SIGN IN')).toBeInTheDocument();
  });

  it('shows access denied when staff role is insufficient', async () => {
    localStorage.setItem('token', 'tok');
    api.getMe.mockResolvedValue({
      ok: true,
      data: { id: 1, email: 'u@e.com', full_name: 'U', role: 'STANDARD', staff_role: 'NONE' },
    });
    await act(async () => {
      renderWithAuth('/staff', [STAFF_ROLE.MODERATOR]);
    });
    expect(screen.getByText(/access denied/i)).toBeInTheDocument();
  });

  it('admin always passes regardless of allowedStaffRoles', async () => {
    localStorage.setItem('token', 'tok');
    api.getMe.mockResolvedValue({
      ok: true,
      data: { id: 1, email: 'a@e.com', full_name: 'A', role: 'STANDARD', staff_role: 'ADMIN' },
    });
    await act(async () => {
      renderWithAuth('/staff', []);
    });
    expect(screen.getByText('STAFF AREA')).toBeInTheDocument();
  });

  it('moderator passes when allowedStaffRoles includes MODERATOR', async () => {
    localStorage.setItem('token', 'tok');
    api.getMe.mockResolvedValue({
      ok: true,
      data: { id: 1, email: 'm@e.com', full_name: 'M', role: 'STANDARD', staff_role: 'MODERATOR' },
    });
    await act(async () => {
      renderWithAuth('/staff', [STAFF_ROLE.MODERATOR]);
    });
    expect(screen.getByText('STAFF AREA')).toBeInTheDocument();
  });

  it('verification coordinator is blocked from moderation-only routes', async () => {
    localStorage.setItem('token', 'tok');
    api.getMe.mockResolvedValue({
      ok: true,
      data: { id: 1, email: 'v@e.com', full_name: 'V', role: 'STANDARD', staff_role: 'VERIFICATION_COORDINATOR' },
    });
    await act(async () => {
      renderWithAuth('/staff', [STAFF_ROLE.MODERATOR]);
    });
    expect(screen.getByText(/access denied/i)).toBeInTheDocument();
  });
});
