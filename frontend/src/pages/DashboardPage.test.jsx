import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import DashboardPage from './DashboardPage';
import { useAuth } from '../context/AuthContext';
import { STAFF_ROLE } from '../utils/staffRoles';

jest.mock('../context/AuthContext', () => ({
  useAuth: jest.fn(),
}));

jest.mock('../services/api', () => ({
  logout: jest.fn(),
}));

jest.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key, values) => (key === 'dashboard.welcome' ? `Welcome ${values.name}` : key),
  }),
}));

function renderDashboard(user) {
  useAuth.mockReturnValue({
    user,
    logoutUser: jest.fn(),
  });

  return render(
    <MemoryRouter initialEntries={['/dashboard']}>
      <Routes>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/staff" element={<div>STAFF DASHBOARD ROUTE</div>} />
        <Route path="/staff/users" element={<div>USER MANAGEMENT ROUTE</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('DashboardPage staff entry', () => {
  it('routes admins to the full staff dashboard instead of only user management', async () => {
    const user = {
      id: 1,
      full_name: 'Admin User',
      role: 'STANDARD',
      staff_role: STAFF_ROLE.ADMIN,
      profile: { availability_status: 'SAFE' },
    };

    renderDashboard(user);
    await userEvent.click(screen.getByText('Staff tools'));

    expect(screen.getByText('STAFF DASHBOARD ROUTE')).toBeInTheDocument();
    expect(screen.queryByText('USER MANAGEMENT ROUTE')).not.toBeInTheDocument();
  });

  it('routes moderators to the full staff dashboard so both moderation tools are reachable', async () => {
    const user = {
      id: 2,
      full_name: 'Moderator User',
      role: 'STANDARD',
      staff_role: STAFF_ROLE.MODERATOR,
      profile: { availability_status: 'SAFE' },
    };

    renderDashboard(user);
    await userEvent.click(screen.getByText('Staff tools'));

    expect(screen.getByText('STAFF DASHBOARD ROUTE')).toBeInTheDocument();
  });
});
