import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { logout } from '../services/api';

export default function DashboardPage() {
  const { user, logoutUser } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout(); // POST /logout — deletes token server-side
    logoutUser();   // clear client-side state
    navigate('/', { replace: true });
  };

  if (!user) return null; // guarded by ProtectedRoute, but just in case

  const roleLabel = user.role === 'EXPERT' ? '🎓 Expert' : '👤 Standard';

  const features = [
    { icon: '💬', title: 'Forum', desc: 'Community discussions', path: '/forum' },
    { icon: '🆘', title: 'Help Requests', desc: 'Ask for or offer help', path: '/help-requests' },
    { icon: '👤', title: 'Profile', desc: 'Manage your account', path: null },
    { icon: '📶', title: 'Offline Info', desc: 'Critical data access', path: null },
  ];

  return (
    <div className="page dashboard-page">
      <header className="dashboard-header">
        <h2 className="gradient-text">Emergency Hub</h2>
        <button className="btn btn-secondary btn-sm" onClick={handleLogout}>
          Logout
        </button>
      </header>

      <div className="welcome-card">
        <h1>Welcome, {user.full_name}!</h1>
        <div className="welcome-meta">
          <span className="badge">{roleLabel}</span>
          {user.expertise_field && (
            <span className="badge badge-accent">
              {user.expertise_field}
            </span>
          )}
          {user.neighborhood_address && (
            <span className="badge badge-muted">
              📍 {user.neighborhood_address}
            </span>
          )}
        </div>
      </div>

      <div className="dashboard-grid">
        {features.map((f) => (
          <div
            className={`dashboard-card ${f.path ? 'dashboard-card--clickable' : ''}`}
            key={f.title}
            onClick={() => f.path && navigate(f.path)}
            role={f.path ? 'link' : undefined}
          >
            <span className="dashboard-card-icon">{f.icon}</span>
            <h3>{f.title}</h3>
            <p>{f.desc}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
