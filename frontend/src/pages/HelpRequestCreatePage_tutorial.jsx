import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import useTutorialGuide from '../components/TutorialGuide';
import { saveTutorialHelpRequest } from '../utils/tutorialStorage';

const HELP_REQUEST_TOUR_STEPS = [
  {
    target: 'scenario',
    title: 'Start with the situation',
    text: 'Start with the situation so neighbors understand who needs help and why.',
  },
  {
    target: 'title',
    title: 'Write a clear title',
    text: 'A short title helps neighbors understand the need quickly while scanning a list.',
  },
  {
    target: 'description',
    title: 'Add useful details',
    text: 'Describe who needs help, what is needed, and any safety details helpers should know.',
  },
  {
    target: 'category',
    title: 'Pick category and urgency',
    text: 'These choices help neighbors find urgent needs and understand what they can do.',
  },
  {
    target: 'submit',
    title: 'Check before sending',
    text: 'Review the details before adding the request to the list.',
  },
];

const CATEGORIES = [
  { value: 'MEDICAL', label: 'Medical' },
  { value: 'FOOD', label: 'Food / water' },
  { value: 'SHELTER', label: 'Shelter' },
  { value: 'TRANSPORT', label: 'Transport' },
  { value: 'OTHER', label: 'Other' },
];

const URGENCIES = [
  { value: 'LOW', label: 'Low' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH', label: 'High' },
];

export default function HelpRequestCreatePageTutorial() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    title: 'Need drinking water for an elderly neighbor',
    description: 'Our building has no running water after the outage. One elderly neighbor cannot walk to the distribution point.',
    category: 'FOOD',
    urgency: 'MEDIUM',
    location_text: 'Near Besiktas community center, Block B entrance',
  });
  const [errors, setErrors] = useState({});
  const [previewed, setPreviewed] = useState(false);
  const { activeStep, GuidePanel, RestartButton } = useTutorialGuide({
    storageKey: 'emergencyHubHelpRequestTutorialSeen',
    steps: HELP_REQUEST_TOUR_STEPS,
  });

  const completion = useMemo(() => {
    const fields = ['title', 'description', 'category', 'urgency', 'location_text'];
    const filled = fields.filter((field) => String(form[field]).trim()).length;
    return Math.round((filled / fields.length) * 100);
  }, [form]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setPreviewed(false);
    if (errors[name]) {
      setErrors((prev) => {
        const copy = { ...prev };
        delete copy[name];
        return copy;
      });
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const nextErrors = {};
    if (!form.title.trim()) nextErrors.title = 'A clear title helps neighbors understand the need quickly.';
    if (!form.description.trim()) nextErrors.description = 'Describe who needs help, what is needed, and any safety details.';
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    saveTutorialHelpRequest({
      title: form.title.trim(),
      description: form.description.trim(),
      category: form.category,
      urgency: form.urgency,
      location_text: form.location_text.trim() || 'No location note added',
      author: 'You',
      status: 'Open',
      createdLabel: 'just now',
      local: true,
    });
    setPreviewed(true);
    navigate('/tutorial/help-requests');
  };

  return (
    <div className="page help-create-page tutorial-page">
      <header className="help-requests-header">
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/tutorial/help-requests')}>
          &larr; Help requests
        </button>
        <h2 className="gradient-text">New Help Request</h2>
        <div className="tutorial-header-actions">
          {RestartButton}
        </div>
      </header>

      {GuidePanel}

      <div className={`tutorial-scenario-strip ${activeStep?.target === 'scenario' ? 'tutorial-tour-highlight' : ''}`}>
        <div>
          <strong>Current situation</strong>
          <span>A neighbor needs drinking water after a power outage.</span>
        </div>
        <div>
          <strong>{completion}% ready</strong>
          <span>Clear details help nearby neighbors understand what kind of help is needed.</span>
        </div>
      </div>

      <div className="help-create-card">
        <div className="alert alert-success tutorial-alert">
          Add enough detail so nearby neighbors can understand what kind of help is needed.
        </div>

        {previewed && (
          <div className="alert alert-success">
            Looks good. A request like this gives helpers the main details they need to respond.
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className={`form-group ${activeStep?.target === 'title' ? 'tutorial-tour-highlight' : ''}`}>
            <label htmlFor="tutorial-title">Title</label>
            <input
              id="tutorial-title"
              name="title"
              type="text"
              placeholder="Short summary of the need"
              value={form.title}
              onChange={handleChange}
              className={errors.title ? 'input-error' : ''}
            />
            {errors.title && <span className="field-error">{errors.title}</span>}
          </div>

          <div className={`form-group ${activeStep?.target === 'description' ? 'tutorial-tour-highlight' : ''}`}>
            <label htmlFor="tutorial-description">Description</label>
            <textarea
              id="tutorial-description"
              name="description"
              className={`help-create-textarea${errors.description ? ' input-error' : ''}`}
              placeholder="Explain who needs help and what would be useful."
              value={form.description}
              onChange={handleChange}
              rows={5}
            />
            {errors.description && <span className="field-error">{errors.description}</span>}
          </div>

          <div className={`help-create-row ${activeStep?.target === 'category' ? 'tutorial-tour-highlight' : ''}`}>
            <div className="form-group help-create-half">
              <label htmlFor="tutorial-category">Category</label>
              <select id="tutorial-category" name="category" value={form.category} onChange={handleChange}>
                {CATEGORIES.map((category) => (
                  <option key={category.value} value={category.value}>{category.label}</option>
                ))}
              </select>
            </div>

            <div className="form-group help-create-half">
              <label htmlFor="tutorial-urgency">Urgency</label>
              <select id="tutorial-urgency" name="urgency" value={form.urgency} onChange={handleChange}>
                {URGENCIES.map((urgency) => (
                  <option key={urgency.value} value={urgency.value}>{urgency.label}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="tutorial-location">Location note <span className="optional-tag">recommended</span></label>
            <input
              id="tutorial-location"
              name="location_text"
              type="text"
              placeholder="Example: apartment entrance, street corner, shelter desk"
              value={form.location_text}
              onChange={handleChange}
            />
          </div>

          <div className="form-group">
            <label>Images and precise GPS <span className="optional-tag">available after sign in</span></label>
            <div className="help-create-location-row">
              <button type="button" className="btn btn-secondary btn-sm" disabled>Add photo</button>
              <button type="button" className="btn btn-secondary btn-sm" disabled>Use my location</button>
            </div>
          </div>

          <button
            type="submit"
            className={`btn btn-primary btn-block ${activeStep?.target === 'submit' ? 'tutorial-tour-highlight' : ''}`}
          >
            Save request
          </button>
        </form>
      </div>
    </div>
  );
}
