import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import useTutorialGuide from '../components/TutorialGuide';
import { getAllTutorialHelpRequests } from '../utils/tutorialHelpData';

const HELP_LIST_TOUR_STEPS = [
  {
    target: 'filters',
    title: 'Filter by need',
    text: 'Categories help neighbors find requests they can answer quickly.',
  },
  {
    target: 'requests',
    title: 'Review requests',
    text: 'Open a request to see what is needed and how you might respond.',
  },
  {
    target: 'create',
    title: 'Create a request',
    text: 'Create a request when someone needs supplies, transport, shelter, or medical help.',
  },
];

const CATEGORIES = [
  { value: '', label: 'All' },
  { value: 'MEDICAL', label: 'Medical' },
  { value: 'FOOD', label: 'Food / water' },
  { value: 'SHELTER', label: 'Shelter' },
  { value: 'TRANSPORT', label: 'Transport' },
  { value: 'OTHER', label: 'Other' },
];

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
          &larr; Dashboard
        </button>
        <h2 className="gradient-text">Help Requests</h2>
        <div className="tutorial-header-actions">
          {RestartButton}
          <button
            className={`btn btn-primary btn-sm ${activeStep?.target === 'create' ? 'tutorial-tour-highlight' : ''}`}
            onClick={() => navigate('/tutorial/help-requests/new')}
          >
            New request
          </button>
        </div>
      </header>

      {GuidePanel}

      <div className="tutorial-scenario-strip">
        <div>
          <strong>Current situation</strong>
          <span>Neighbors are sharing practical needs after a power outage.</span>
        </div>
        <div>
          <strong>Neighborhood needs</strong>
          <span>Requests stay organized so helpers can quickly find where they are needed.</span>
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
                  { value: 'LOW', label: 'Low' },
                  { value: 'MEDIUM', label: 'Medium' },
                  { value: 'HIGH', label: 'High' },
                ], request.urgency)}
              </span>
            </div>

            <p className="tutorial-post-body">{request.description}</p>

            <div className="help-request-card-meta">
              {request.local && <span className="badge badge-accent">Your request</span>}
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
