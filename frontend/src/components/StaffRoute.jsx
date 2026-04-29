import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { STAFF_ROLE } from '../utils/staffRoles';

/**
 * Route guard for staff-only pages.
 *
 * `allowedStaffRoles` is an array of roles that may access the wrapped page.
 * `ADMIN` is always allowed in addition to the listed roles, since admins
 * inherit moderator and verification-coordinator authority on the backend.
 */
export default function StaffRoute({ children, allowedStaffRoles = [] }) {
  const { user, isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div className="page" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
        <p style={{ color: 'var(--text-secondary)' }}>Loading…</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/signin" replace />;
  }

  const role = user?.staff_role;
  const allowed = new Set([STAFF_ROLE.ADMIN, ...allowedStaffRoles]);
  if (!role || !allowed.has(role)) {
    return (
      <div className="page" style={{ padding: '2rem', textAlign: 'center' }}>
        <h2>Access denied</h2>
        <p style={{ color: 'var(--text-secondary)' }}>
          You don&apos;t have permission to view this page.
        </p>
      </div>
    );
  }

  return children;
}
