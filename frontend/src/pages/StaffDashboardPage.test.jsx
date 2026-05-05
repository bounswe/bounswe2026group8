import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import StaffDashboardPage from './StaffDashboardPage';
import { useAuth } from '../context/AuthContext';
import { STAFF_ROLE } from '../utils/staffRoles';

jest.mock('../context/AuthContext', () => ({
  useAuth: jest.fn(),
}));

function renderStaffDashboard(staffRole) {
  useAuth.mockReturnValue({
    user: {
      id: 1,
      email: 'staff@example.com',
      full_name: 'Staff User',
      staff_role: staffRole,
    },
  });

  return render(
    <MemoryRouter>
      <StaffDashboardPage />
    </MemoryRouter>,
  );
}

describe('StaffDashboardPage', () => {
  it('shows admin, moderation, verification, and hub tools to admins', () => {
    renderStaffDashboard(STAFF_ROLE.ADMIN);

    expect(screen.getByText('User management')).toBeInTheDocument();
    expect(screen.getByText('Hub management')).toBeInTheDocument();
    expect(screen.getByText('Audit log')).toBeInTheDocument();
    expect(screen.getByText('Forum moderation')).toBeInTheDocument();
    expect(screen.getByText('Help moderation')).toBeInTheDocument();
    expect(screen.getByText('Expertise verification')).toBeInTheDocument();
  });

  it('does not show admin-only hub tools to moderators', () => {
    renderStaffDashboard(STAFF_ROLE.MODERATOR);

    expect(screen.getByText('Forum moderation')).toBeInTheDocument();
    expect(screen.getByText('Help moderation')).toBeInTheDocument();
    expect(screen.queryByText('Hub management')).not.toBeInTheDocument();
    expect(screen.queryByText('Expertise verification')).not.toBeInTheDocument();
  });
});
