import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

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
    setPreviewed(Object.keys(nextErrors).length === 0);
  };

  return (
    <div className="page help-create-page tutorial-page">
      <header className="help-requests-header">
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/tutorial')}>
          &larr; Tutorial dashboard
        </button>
        <h2 className="gradient-text">Practice Help Request</h2>
      </header>

      <div className="tutorial-scenario-strip">
        <div>
          <strong>Scenario</strong>
          <span>A neighbor needs drinking water after a power outage.</span>
        </div>
        <div>
          <strong>{completion}% ready</strong>
          <span>Clear details help nearby neighbors understand what kind of help is needed.</span>
        </div>
      </div>

      <div className="help-create-card">
        <div className="alert alert-success tutorial-alert">
          This is a practice request, so you can try it without asking anyone for help.
        </div>

        {previewed && (
          <div className="alert alert-success">
            Looks good. A request like this gives helpers the main details they need to respond.
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
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

          <div className="form-group">
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

          <div className="help-create-row">
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

          <button type="submit" className="btn btn-primary btn-block">
            Check my practice request
          </button>
        </form>
      </div>
    </div>
  );
}
