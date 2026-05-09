import { useEffect, useState, useCallback } from 'react';
import {
  listStaffUsers,
  updateAccountStatus,
  updateStaffRole,
} from '../services/api';
import { STAFF_ROLE } from '../utils/staffRoles';
import BackToDashboard from '../components/BackToDashboard';

const STAFF_ROLE_OPTIONS = [
  { value: STAFF_ROLE.NONE, label: 'None' },
  { value: STAFF_ROLE.MODERATOR, label: 'Moderator' },
  { value: STAFF_ROLE.VERIFICATION_COORDINATOR, label: 'Verification Coordinator' },
  { value: STAFF_ROLE.ADMIN, label: 'Admin' },
];

export default function AdminUsersPage() {
  const [search, setSearch] = useState('');
  const [staffRoleFilter, setStaffRoleFilter] = useState('');
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    const params = {};
    if (search.trim()) params.search = search.trim();
    if (staffRoleFilter) params.staff_role = staffRoleFilter;
    const { ok, data } = await listStaffUsers(params);
    if (ok) {
      setUsers(data);
    } else {
      setError(data?.detail || 'Failed to load users');
    }
    setLoading(false);
  }, [search, staffRoleFilter]);

  useEffect(() => {
    reload();
  }, [reload]);

  const handleStaffRoleChange = async (user, newRole) => {
    if (newRole === user.staff_role) return;
    const reason = window.prompt(
      `Reason for setting ${user.email} to ${newRole} (optional):`,
      '',
    );
    if (reason === null) return;
    const { ok, data } = await updateStaffRole(user.id, newRole, reason);
    if (ok) {
      setUsers((rows) => rows.map((u) => (u.id === user.id ? data : u)));
    } else {
      window.alert(data?.detail || 'Update failed');
    }
  };

  const handleStatusToggle = async (user) => {
    const willSuspend = user.is_active;
    const reason = window.prompt(
      `${willSuspend ? 'Suspend' : 'Reactivate'} ${user.email}. Reason:`,
      '',
    );
    if (!reason || !reason.trim()) {
      if (reason !== null) window.alert('A reason is required.');
      return;
    }
    const { ok, data } = await updateAccountStatus(user.id, !user.is_active, reason.trim());
    if (ok) {
      setUsers((rows) => rows.map((u) => (u.id === user.id ? data : u)));
    } else {
      window.alert(data?.detail || 'Update failed');
    }
  };

  return (
    <div className="page" style={{ padding: '1.5rem', maxWidth: 1100, margin: '0 auto' }}>
      <BackToDashboard to="/staff" label="← Back to staff dashboard" />
      <h2 className="gradient-text">User management</h2>
      <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', marginBottom: '1rem' }}>
        <input
          type="search"
          placeholder="Search email or name"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{ flex: 1, minWidth: 220, padding: '0.5rem 0.75rem' }}
        />
        <select
          value={staffRoleFilter}
          onChange={(e) => setStaffRoleFilter(e.target.value)}
          style={{ padding: '0.5rem 0.75rem' }}
        >
          <option value="">All staff roles</option>
          {STAFF_ROLE_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>{opt.label}</option>
          ))}
        </select>
        <button className="btn btn-secondary btn-sm" onClick={reload}>Refresh</button>
      </div>

      {loading && <p>Loading…</p>}
      {error && <p style={{ color: '#f87171' }}>{error}</p>}

      {!loading && !error && (
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--border-color, #333)' }}>
                <th style={{ padding: '0.5rem' }}>Email</th>
                <th style={{ padding: '0.5rem' }}>Name</th>
                <th style={{ padding: '0.5rem' }}>Role</th>
                <th style={{ padding: '0.5rem' }}>Staff role</th>
                <th style={{ padding: '0.5rem' }}>Active</th>
                <th style={{ padding: '0.5rem' }}>Hub</th>
                <th style={{ padding: '0.5rem' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id} style={{ borderBottom: '1px solid var(--border-color, #222)' }}>
                  <td style={{ padding: '0.5rem' }}>{u.email}</td>
                  <td style={{ padding: '0.5rem' }}>{u.full_name}</td>
                  <td style={{ padding: '0.5rem' }}>{u.role}</td>
                  <td style={{ padding: '0.5rem' }}>
                    <select
                      value={u.staff_role}
                      onChange={(e) => handleStaffRoleChange(u, e.target.value)}
                    >
                      {STAFF_ROLE_OPTIONS.map((opt) => (
                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                      ))}
                    </select>
                  </td>
                  <td style={{ padding: '0.5rem' }}>{u.is_active ? 'Yes' : 'No'}</td>
                  <td style={{ padding: '0.5rem' }}>{u.hub?.name ?? '—'}</td>
                  <td style={{ padding: '0.5rem' }}>
                    <button
                      className="btn btn-secondary btn-sm"
                      onClick={() => handleStatusToggle(u)}
                    >
                      {u.is_active ? 'Suspend' : 'Reactivate'}
                    </button>
                  </td>
                </tr>
              ))}
              {users.length === 0 && (
                <tr><td colSpan={7} style={{ padding: '1rem', textAlign: 'center' }}>No users found.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
