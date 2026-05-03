import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { logout } from '../services/api';
import { useTranslation } from 'react-i18next'; // 1. Import the hook

export default function DashboardPage() {
  const { user, logoutUser } = useAuth();
  const navigate = useNavigate();
  const { t } = useTranslation(); // 2. Initialize the hook

  const handleLogout = async () => {
    await logout(); // POST /logout — deletes token server-side
    logoutUser();   // clear client-side state
    navigate('/', { replace: true });
  };

  if (!user) return null; // guarded by ProtectedRoute, but just in case

  // 3. Emojis stay in code, text pulls from JSON
  const roleLabel = user.role === 'EXPERT'
      ? `🎓 ${t('dashboard.roles.expert')}`
      : `👤 ${t('dashboard.roles.standard')}`;

  // 4. Colors stay in code, labels pull from JSON
  const statusLabels = {
    SAFE: { label: t('dashboard.status.safe'), color: '#34d399' },
    NEEDS_HELP: { label: t('dashboard.status.needs_help'), color: '#f87171' },
    AVAILABLE_TO_HELP: { label: t('dashboard.status.available'), color: '#38bdf8' }
  };
  const avail = statusLabels[user.profile?.availability_status] || statusLabels.SAFE;

  // 5. Icons and Paths stay in code, text pulls from JSON
  const features = [
    { icon: '💬', title: t('dashboard.features.forum.title'), desc: t('dashboard.features.forum.desc'), path: '/forum' },
    { icon: '🆘', title: t('dashboard.features.help_requests.title'), desc: t('dashboard.features.help_requests.desc'), path: '/help-requests' },
    { icon: '👤', title: t('dashboard.features.profile.title'), desc: t('dashboard.features.profile.desc'), path: '/profile' },
    { icon: '📶', title: t('dashboard.features.emergency_info.title'), desc: t('dashboard.features.emergency_info.desc'), path: '/emergency-info' },
    { icon: '📡', title: t('dashboard.features.offline_messages.title'), desc: t('dashboard.features.offline_messages.desc'), path: '/offline-messages' },
  ];

  return (
      <div className="page dashboard-page">
        <header className="dashboard-header">
          <h2 className="gradient-text">{t('dashboard.header_title')}</h2>
          <button className="btn btn-secondary btn-sm" onClick={handleLogout}>
            {t('dashboard.btn_logout')}
          </button>
        </header>

        <div className="welcome-card">
          {/* 6. Inject dynamic variables using an object */}
          <h1>{t('dashboard.welcome', { name: user.full_name })}</h1>

          <div className="welcome-meta">
            <span className="badge">{roleLabel}</span>
            <span className="badge" style={{ color: avail.color, borderColor: avail.color + '44', background: avail.color + '11' }}>
            ● {avail.label}
          </span>
            {user.expertise_field && (
                <span className="badge badge-accent">
              {/* Database field: No translation needed */}
                  {user.expertise_field}
            </span>
            )}
            {user.neighborhood_address && (
                <span className="badge badge-muted">
              {/* Database field: No translation needed */}
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