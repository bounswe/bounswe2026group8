import { useNavigate } from 'react-router-dom';
import useTutorialGuide from '../components/TutorialGuide';

const TOUR_STEPS = [
  {
    target: 'welcome',
    title: 'Start here',
    text: 'Explore the main areas of the app and learn what to do during an emergency.',
  },
  {
    target: 'scenario',
    title: 'Follow the story',
    text: 'The guide follows one neighborhood emergency so each step feels connected.',
  },
  {
    target: 'Forum',
    title: 'Read community updates',
    text: 'Use the forum when you want to share or follow public neighborhood information.',
  },
  {
    target: 'Help Requests',
    title: 'Ask for help',
    text: 'Use help requests when someone needs supplies, shelter, transport, or medical help.',
  },
  {
    target: 'Emergency Info',
    title: 'Keep guidance close',
    text: 'Emergency Info gives you practical offline guidance. You can explore the map later after signing in.',
  },
];

export default function DashboardPageTutorial() {
  const navigate = useNavigate();
  const { activeStep, GuidePanel, RestartButton } = useTutorialGuide({
    storageKey: 'emergencyHubTutorialTourSeen',
    steps: TOUR_STEPS,
  });

  const features = [
    {
      icon: '💬',
      title: 'Forum',
      desc: 'Browse community updates, comment on posts, and write a new post.',
      path: '/tutorial/forum',
    },
    {
      icon: '🆘',
      title: 'Help Requests',
      desc: 'Review nearby needs, create a request, and see it in the list.',
      path: '/tutorial/help-requests',
    },
    {
      icon: '👤',
      title: 'Profile',
      desc: 'Profiles are saved for signed-in community members.',
      lockLabel: 'Sign in to use Profile',
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
      lockLabel: 'Sign in to use Offline Messages',
      path: null,
    },
  ];

  return (
    <div className="page dashboard-page tutorial-page">
      <header className="dashboard-header">
        <h2 className="gradient-text">Guided Tour</h2>
        <div className="tutorial-header-actions">
          {RestartButton}
          <button className="btn btn-secondary btn-sm" onClick={() => navigate('/signin')}>
            Back to sign in
          </button>
        </div>
      </header>

      {GuidePanel}

      <div className={`welcome-card ${activeStep?.target === 'welcome' ? 'tutorial-tour-highlight' : ''}`}>
        <h1>Welcome, Neighbor</h1>

        <div className="welcome-meta">
          <span className="badge">👤 Standard Member</span>
          <span className="badge" style={{ color: '#34d399', borderColor: '#34d39944', background: '#34d39911' }}>
            ● Safe
          </span>
          <span className="badge badge-muted">📍 Besiktas Neighborhood</span>
        </div>
      </div>

      <div className={`tutorial-scenario-strip ${activeStep?.target === 'scenario' ? 'tutorial-tour-highlight' : ''}`}>
        <div>
          <strong>Scenario</strong>
          <span>After a neighborhood power outage, learn how to ask for water safely.</span>
        </div>
        <div>
          <strong>Guided flow</strong>
          <span>Try the key actions and see how information moves through the app.</span>
        </div>
      </div>

      <div className="dashboard-grid">
        {features.map((f) => (
          <div
            className={`dashboard-card ${f.path ? 'dashboard-card--clickable' : ''} ${activeStep?.target === f.title ? 'tutorial-tour-highlight' : ''}`}
            key={f.title}
            onClick={() => f.path && navigate(f.path)}
            role={f.path ? 'link' : undefined}
          >
            <span className="dashboard-card-icon tutorial-card-icon">{f.icon}</span>
            <h3>{f.title}</h3>
            <p>{f.desc}</p>
            {!f.path && <span className="tutorial-card-lock">{f.lockLabel}</span>}
          </div>
        ))}
      </div>
    </div>
  );
}
