import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

const TOUR_STORAGE_KEY = 'emergencyHubTutorialTourSeen';

const TOUR_STEPS = [
  {
    target: 'welcome',
    title: 'Start here',
    text: 'This is a safe practice version of the app. You can explore without changing anything for other people.',
    position: 'right',
  },
  {
    target: 'scenario',
    title: 'Follow the story',
    text: 'The tutorial uses one simple emergency situation so each step feels connected.',
    position: 'top',
  },
  {
    target: 'Forum',
    title: 'Read community updates',
    text: 'Use the forum when you want to share or follow public neighborhood information.',
    position: 'right',
  },
  {
    target: 'Help Requests',
    title: 'Practice asking for help',
    text: 'This is the most important practice step when someone needs supplies, shelter, transport, or medical help.',
    position: 'bottom',
  },
  {
    target: 'Emergency Info',
    title: 'Keep guidance close',
    text: 'Emergency Info gives you practical offline guidance. You can explore the map later after signing in.',
    position: 'left',
  },
];

export default function DashboardPageTutorial() {
  const navigate = useNavigate();
  const [tourStep, setTourStep] = useState(null);

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

  useEffect(() => {
    if (window.localStorage.getItem(TOUR_STORAGE_KEY) !== 'true') {
      setTourStep(0);
    }
  }, []);

  const closeTour = () => {
    window.localStorage.setItem(TOUR_STORAGE_KEY, 'true');
    setTourStep(null);
  };

  const restartTour = () => {
    window.localStorage.removeItem(TOUR_STORAGE_KEY);
    setTourStep(0);
  };

  const nextTourStep = () => {
    if (tourStep === TOUR_STEPS.length - 1) {
      closeTour();
    } else {
      setTourStep((step) => step + 1);
    }
  };

  const activeTour = tourStep !== null ? TOUR_STEPS[tourStep] : null;

  return (
    <div className="page dashboard-page tutorial-page">
      <header className="dashboard-header">
        <h2 className="gradient-text">Tutorial Mode</h2>
        <div className="tutorial-header-actions">
          {!activeTour && (
            <button className="btn btn-secondary btn-sm" onClick={restartTour}>
              Show guide
            </button>
          )}
          <button className="btn btn-secondary btn-sm" onClick={() => navigate('/signin')}>
            Back to sign in
          </button>
        </div>
      </header>

      {activeTour && (
        <section className="tutorial-guide-panel" aria-live="polite">
          <div>
            <span className="tutorial-tour-count">
              Step {tourStep + 1} of {TOUR_STEPS.length}
            </span>
            <h3>{activeTour.title}</h3>
            <p>{activeTour.text}</p>
          </div>
          <div className="tutorial-tour-actions">
            <button type="button" className="btn btn-secondary btn-sm" onClick={closeTour}>
              Skip guide
            </button>
            <button type="button" className="btn btn-primary btn-sm" onClick={nextTourStep}>
              {tourStep === TOUR_STEPS.length - 1 ? 'Finish guide' : 'Next'}
            </button>
          </div>
        </section>
      )}

      <div className={`welcome-card ${activeTour?.target === 'welcome' ? 'tutorial-tour-highlight' : ''}`}>
        <h1>Welcome, Demo Resident</h1>

        <div className="welcome-meta">
          <span className="badge">👤 Standard Member</span>
          <span className="badge" style={{ color: '#34d399', borderColor: '#34d39944', background: '#34d39911' }}>
            ● Safe
          </span>
          <span className="badge badge-muted">📍 Besiktas Demo Neighborhood</span>
        </div>
      </div>

      <div className={`tutorial-scenario-strip ${activeTour?.target === 'scenario' ? 'tutorial-tour-highlight' : ''}`}>
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
            className={`dashboard-card ${f.path ? 'dashboard-card--clickable' : ''} ${activeTour?.target === f.title ? 'tutorial-tour-highlight' : ''}`}
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
