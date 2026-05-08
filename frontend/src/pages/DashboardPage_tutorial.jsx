import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import useTutorialGuide from '../components/TutorialGuide';

export default function DashboardPageTutorial() {
  const navigate = useNavigate();
  const { t } = useTranslation();

  const tourSteps = [
    { target: 'welcome', title: t('tutorial.dashboard.steps.startTitle'), text: t('tutorial.dashboard.steps.startText') },
    { target: 'scenario', title: t('tutorial.dashboard.steps.situationTitle'), text: t('tutorial.dashboard.steps.situationText') },
    { target: 'Forum', title: t('tutorial.dashboard.steps.forumTitle'), text: t('tutorial.dashboard.steps.forumText') },
    { target: 'Help Requests', title: t('tutorial.dashboard.steps.helpTitle'), text: t('tutorial.dashboard.steps.helpText') },
    { target: 'Emergency Info', title: t('tutorial.dashboard.steps.infoTitle'), text: t('tutorial.dashboard.steps.infoText') },
  ];

  const { activeStep, GuidePanel, RestartButton } = useTutorialGuide({
    storageKey: 'emergencyHubTutorialTourSeen',
    steps: tourSteps,
  });

  const features = [
    {
      icon: '💬',
      title: t('dashboard.features.forum.title'),
      target: 'Forum',
      desc: t('tutorial.dashboard.featureDesc.forum'),
      path: '/tutorial/forum',
    },
    {
      icon: '🆘',
      title: t('dashboard.features.help_requests.title'),
      target: 'Help Requests',
      desc: t('tutorial.dashboard.featureDesc.help'),
      path: '/tutorial/help-requests',
    },
    {
      icon: '👤',
      title: t('dashboard.features.profile.title'),
      target: 'Profile',
      desc: t('tutorial.dashboard.featureDesc.profile'),
      lockLabel: t('tutorial.dashboard.signInProfile'),
      path: null,
    },
    {
      icon: '📶',
      title: t('dashboard.features.emergency_info.title'),
      target: 'Emergency Info',
      desc: t('tutorial.dashboard.featureDesc.info'),
      path: '/tutorial/emergency-info',
    },
    {
      icon: '📡',
      title: t('dashboard.features.offline_messages.title'),
      target: 'Offline Messages',
      desc: t('tutorial.dashboard.featureDesc.messages'),
      lockLabel: t('tutorial.dashboard.signInMessages'),
      path: null,
    },
  ];

  return (
    <div className="page dashboard-page tutorial-page">
      <header className="dashboard-header">
        <h2 className="gradient-text">{t('dashboard.header_title')}</h2>
        <div className="tutorial-header-actions">
          {RestartButton}
          <button className="btn btn-secondary btn-sm" onClick={() => navigate('/signin')}>
            {t('tutorial.common.backSignIn')}
          </button>
        </div>
      </header>

      {GuidePanel}

      <div className={`welcome-card ${activeStep?.target === 'welcome' ? 'tutorial-tour-highlight' : ''}`}>
        <h1>{t('tutorial.dashboard.welcome')}</h1>

        <div className="welcome-meta">
          <span className="badge">👤 {t('tutorial.dashboard.standardMember')}</span>
          <span className="badge" style={{ color: '#34d399', borderColor: '#34d39944', background: '#34d39911' }}>
            ● {t('tutorial.dashboard.safe')}
          </span>
          <span className="badge badge-muted">📍 {t('tutorial.dashboard.neighborhood')}</span>
        </div>
      </div>

      <div className={`tutorial-scenario-strip ${activeStep?.target === 'scenario' ? 'tutorial-tour-highlight' : ''}`}>
        <div>
          <strong>{t('tutorial.common.currentSituation')}</strong>
          <span>{t('tutorial.dashboard.steps.situationText')}</span>
        </div>
        <div>
          <strong>{t('tutorial.dashboard.whatCanDo')}</strong>
          <span>{t('tutorial.dashboard.actionText')}</span>
        </div>
      </div>

      <div className="dashboard-grid">
        {features.map((f) => (
          <div
            className={`dashboard-card ${f.path ? 'dashboard-card--clickable' : ''} ${activeStep?.target === f.target ? 'tutorial-tour-highlight' : ''}`}
            key={f.target}
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
