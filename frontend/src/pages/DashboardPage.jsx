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

  // Placeholder feature cards for future subgroup work
  const features = [
    { icon: '💬', title: 'Forum', desc: 'Community discussions' },
    { icon: '🆘', title: 'Help Requests', desc: 'Ask for or offer help' },
    { icon: '👤', title: 'Profile', desc: 'Manage your account' },
    { icon: '📶', title: 'Offline Info', desc: 'Critical data access' },
  ];

  return (
    <div className="page dashboard-page">
      {/* Top bar */}
      <header className="dashboard-header">
        <h2 className="gradient-text">Emergency Hub</h2>
        <button className="btn btn-secondary btn-sm" onClick={handleLogout}>
          Logout
        </button>
      </header>

      {/* Welcome card */}
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

      {/* Feature grid */}
      <div className="dashboard-grid">
        {features.map((f) => (
          <div className="dashboard-card" key={f.title}>
            <span className="dashboard-card-icon">{f.icon}</span>
            <h3>{f.title}</h3>
            <p>{f.desc}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
