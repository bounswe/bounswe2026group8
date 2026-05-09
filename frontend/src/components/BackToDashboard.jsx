import { Link } from 'react-router-dom';

/**
 * Small "← Back to dashboard" link rendered above staff page headers.
 *
 * Kept extremely simple so it picks up project styling automatically and
 * does not depend on any i18n keys (these pages are staff-only English).
 */
export default function BackToDashboard({ to = '/dashboard', label = '← Back to dashboard' }) {
  return (
    <div style={{ marginBottom: '0.75rem' }}>
      <Link
        to={to}
        className="btn btn-secondary btn-sm"
        style={{ textDecoration: 'none' }}
      >
        {label}
      </Link>
    </div>
  );
}
