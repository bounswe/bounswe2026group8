import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import useTutorialGuide from '../components/TutorialGuide';
import { getAllTutorialHelpRequests } from '../utils/tutorialHelpData';

const URGENCY_CLASSES = {
  LOW: 'badge-muted',
  MEDIUM: 'badge-accent',
  HIGH: 'badge-urgency-high',
};

function labelFor(items, value) {
  return items.find((item) => item.value === value)?.label || value;
}

export default function HelpRequestsPageTutorial() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const HELP_LIST_TOUR_STEPS = [
    { target: 'filters', title: t('tutorial.helpList.steps.filterTitle'), text: t('tutorial.helpList.steps.filterText') },
    { target: 'requests', title: t('tutorial.helpList.steps.reviewTitle'), text: t('tutorial.helpList.steps.reviewText') },
    { target: 'create', title: t('tutorial.helpList.steps.createTitle'), text: t('tutorial.helpList.steps.createText') },
  ];
  const CATEGORIES = [
    { value: '', label: t('tutorial.helpList.categories.all') },
    { value: 'MEDICAL', label: t('tutorial.helpList.categories.medical') },
    { value: 'FOOD', label: t('tutorial.helpList.categories.food') },
    { value: 'SHELTER', label: t('tutorial.helpList.categories.shelter') },
    { value: 'TRANSPORT', label: t('tutorial.helpList.categories.transport') },
    { value: 'OTHER', label: t('tutorial.helpList.categories.other') },
  ];
  const [category, setCategory] = useState('');
  const [allRequests] = useState(() => getAllTutorialHelpRequests());
  const { activeStep, GuidePanel, RestartButton } = useTutorialGuide({
    storageKey: 'emergencyHubHelpListTutorialSeen',
    steps: HELP_LIST_TOUR_STEPS,
  });

  const requests = useMemo(() => {
    return category ? allRequests.filter((request) => request.category === category) : allRequests;
  }, [allRequests, category]);

  return (
    <div className="page help-requests-page tutorial-page">
      <header className="help-requests-header page-main-header">
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/tutorial')}>
          {t('tutorial.common.backDashboard')}
        </button>
        <h2 className="gradient-text">{t('dashboard.features.help_requests.title')}</h2>
        <div className="tutorial-header-actions">
          {RestartButton}
          <button
            className={`btn btn-primary btn-sm ${activeStep?.target === 'create' ? 'tutorial-tour-highlight' : ''}`}
            onClick={() => navigate('/tutorial/help-requests/new')}
          >
            {t('tutorial.helpList.newRequest')}
          </button>
        </div>
      </header>

      {GuidePanel}

      <div className="tutorial-scenario-strip">
        <div>
          <strong>{t('tutorial.common.currentSituation')}</strong>
          <span>{t('tutorial.helpList.situationText')}</span>
        </div>
        <div>
          <strong>{t('tutorial.helpList.neighborhoodNeeds')}</strong>
          <span>{t('tutorial.helpList.needsText')}</span>
        </div>
      </div>

      <div className={`help-requests-filters ${activeStep?.target === 'filters' ? 'tutorial-tour-highlight' : ''}`}>
        {CATEGORIES.map((cat) => (
          <button
            key={cat.value}
            className={`btn btn-sm ${category === cat.value ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setCategory(cat.value)}
          >
            {cat.label}
          </button>
        ))}
      </div>

      <div className={`help-requests-list ${activeStep?.target === 'requests' ? 'tutorial-tour-highlight' : ''}`}>
        {requests.map((request) => (
          <article
            className="help-request-card tutorial-help-request-card"
            key={request.id}
            onClick={() => navigate(`/tutorial/help-requests/${request.id}`)}
          >
            <div className="help-request-card-top">
              <h3 className="help-request-card-title">{request.title}</h3>
              <span className={`badge ${URGENCY_CLASSES[request.urgency] || 'badge-muted'}`}>
                {labelFor([
                  { value: 'LOW', label: t('tutorial.helpList.urgency.low') },
                  { value: 'MEDIUM', label: t('tutorial.helpList.urgency.medium') },
                  { value: 'HIGH', label: t('tutorial.helpList.urgency.high') },
                ], request.urgency)}
              </span>
            </div>

            <p className="tutorial-post-body">{request.description}</p>

            <div className="help-request-card-meta">
              {request.local && <span className="badge badge-accent">{t('tutorial.common.yourRequest')}</span>}
              <span className="badge">{labelFor(CATEGORIES, request.category)}</span>
              <span className="badge badge-muted">{request.status}</span>
            </div>

            <div className="help-request-card-footer">
              <span className="help-request-card-author">{request.author}</span>
              <span>{request.location_text}</span>
              <span>{request.createdLabel}</span>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}
