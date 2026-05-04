import { useNavigate } from 'react-router-dom';

export default function DashboardPageTutorial() {
  const navigate = useNavigate();

  const features = [
    {
      icon: '💬',
      title: 'Forum',
      desc: 'Browse sample community updates and see how neighbors share public information.',
      path: '/tutorial/forum',
    },
    {
      icon: '🆘',
      title: 'Help Requests',
      desc: 'Practice creating a help request and learn what details neighbors need.',
      path: '/tutorial/help-requests/new',
    },
    {
      icon: '👤',
      title: 'Profile',
      desc: 'Profiles are saved for signed-in community members.',
      path: null,
    },
    {
      icon: '📶',
      title: 'Emergency Info',
      desc: 'Review emergency guidance you can keep handy offline.',
      path: '/tutorial/emergency-info',
    },
    {
      icon: '📡',
      title: 'Offline Messages',
      desc: 'Offline messages are available once you sign in on your own account.',
      path: null,
    },
  ];

  return (
    <div className="page dashboard-page tutorial-page">
      <header className="dashboard-header">
        <h2 className="gradient-text">Tutorial Mode</h2>
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/signin')}>
          Back to sign in
        </button>
      </header>

      <div className="welcome-card">
        <h1>Welcome, Demo Resident</h1>

        <div className="welcome-meta">
          <span className="badge">👤 Standard Member</span>
          <span className="badge" style={{ color: '#34d399', borderColor: '#34d39944', background: '#34d39911' }}>
            ● Safe
          </span>
          <span className="badge badge-muted">📍 Besiktas Demo Neighborhood</span>
        </div>
      </div>

      <div className="tutorial-scenario-strip">
        <div>
          <strong>Scenario</strong>
          <span>After a neighborhood power outage, learn how to ask for water safely.</span>
        </div>
        <div>
          <strong>Practice space</strong>
          <span>You can explore safely here without changing anything in the community.</span>
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
            <span className="dashboard-card-icon tutorial-card-icon">{f.icon}</span>
            <h3>{f.title}</h3>
            <p>{f.desc}</p>
            {!f.path && <span className="tutorial-card-lock">Sign in to use this</span>}
          </div>
        ))}
      </div>
    </div>
  );
}
