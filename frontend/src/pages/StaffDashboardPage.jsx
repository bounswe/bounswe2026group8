import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import BackToDashboard from '../components/BackToDashboard';
import {
  canModerate,
  canVerifyExpertise,
  isAdmin,
  staffRoleLabel,
} from '../utils/staffRoles';

export default function StaffDashboardPage() {
  const { user } = useAuth();
  const cards = [];
  if (isAdmin(user)) {
    cards.push(
      { path: '/staff/users', title: 'User management', desc: 'Search users, change staff roles, suspend or reactivate accounts.' },
      { path: '/staff/hubs', title: 'Hub management', desc: 'Create, rename, or delete neighbourhood hubs.' },
      { path: '/staff/audit-logs', title: 'Audit log', desc: 'Review every staff action taken on the platform.' },
    );
  }
  if (canModerate(user)) {
    cards.push(
      { path: '/staff/moderation/forum', title: 'Forum moderation', desc: 'Triage reported and hidden forum content.' },
      { path: '/staff/moderation/help', title: 'Help moderation', desc: 'Remove abusive help requests, offers, or comments.' },
    );
  }
  if (canVerifyExpertise(user)) {
    cards.push(
      { path: '/staff/verification/expertise', title: 'Expertise verification', desc: 'Approve, reject, or reopen expertise certifications.' },
    );
  }

  return (
    <div className="page" style={{ padding: '2rem', maxWidth: 960, margin: '0 auto' }}>
      <BackToDashboard />
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
        <h2 className="gradient-text">Staff dashboard</h2>
        <span className="badge">{staffRoleLabel(user?.staff_role) || 'No staff role'}</span>
      </header>
      <p style={{ color: 'var(--text-secondary)' }}>
        These tools are gated by your staff role. Anything you don&apos;t see here is not available to you.
      </p>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1rem', marginTop: '1.5rem' }}>
        {cards.length === 0 && (
          <div className="welcome-card">No staff tools available for your role.</div>
        )}
        {cards.map((card) => (
          <Link
            key={card.path}
            to={card.path}
            className="welcome-card"
            style={{ textDecoration: 'none', color: 'inherit' }}
          >
            <h3 style={{ marginTop: 0 }}>{card.title}</h3>
            <p style={{ color: 'var(--text-secondary)' }}>{card.desc}</p>
          </Link>
        ))}
      </div>
    </div>
  );
}
