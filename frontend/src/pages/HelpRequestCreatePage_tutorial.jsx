import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import useTutorialGuide from '../components/TutorialGuide';
import { saveTutorialHelpRequest } from '../utils/tutorialStorage';

export default function HelpRequestCreatePageTutorial() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const HELP_REQUEST_TOUR_STEPS = [
    { target: 'scenario', title: t('tutorial.helpCreate.steps.scenarioTitle'), text: t('tutorial.helpCreate.steps.scenarioText') },
    { target: 'title', title: t('tutorial.helpCreate.steps.titleTitle'), text: t('tutorial.helpCreate.steps.titleText') },
    { target: 'description', title: t('tutorial.helpCreate.steps.detailsTitle'), text: t('tutorial.helpCreate.steps.detailsText') },
    { target: 'category', title: t('tutorial.helpCreate.steps.categoryTitle'), text: t('tutorial.helpCreate.steps.categoryText') },
    { target: 'submit', title: t('tutorial.helpCreate.steps.submitTitle'), text: t('tutorial.helpCreate.steps.submitText') },
  ];
  const CATEGORIES = [
    { value: 'MEDICAL', label: t('tutorial.helpList.categories.medical') },
    { value: 'FOOD', label: t('tutorial.helpList.categories.food') },
    { value: 'SHELTER', label: t('tutorial.helpList.categories.shelter') },
    { value: 'TRANSPORT', label: t('tutorial.helpList.categories.transport') },
    { value: 'OTHER', label: t('tutorial.helpList.categories.other') },
  ];
  const URGENCIES = [
    { value: 'LOW', label: t('tutorial.helpList.urgency.low') },
    { value: 'MEDIUM', label: t('tutorial.helpList.urgency.medium') },
    { value: 'HIGH', label: t('tutorial.helpList.urgency.high') },
  ];
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
    if (!form.title.trim()) nextErrors.title = t('tutorial.helpCreate.titleError');
    if (!form.description.trim()) nextErrors.description = t('tutorial.helpCreate.descError');
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    saveTutorialHelpRequest({
      title: form.title.trim(),
      description: form.description.trim(),
      category: form.category,
      urgency: form.urgency,
      location_text: form.location_text.trim() || t('tutorial.helpCreate.noLocation'),
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
          {t('tutorial.common.backHelpRequests')}
        </button>
        <h2 className="gradient-text">{t('tutorial.helpCreate.title')}</h2>
        <div className="tutorial-header-actions">
          {RestartButton}
        </div>
      </header>

      {GuidePanel}

      <div className={`tutorial-scenario-strip ${activeStep?.target === 'scenario' ? 'tutorial-tour-highlight' : ''}`}>
        <div>
          <strong>{t('tutorial.common.currentSituation')}</strong>
          <span>{t('tutorial.helpCreate.situationText')}</span>
        </div>
        <div>
          <strong>{t('tutorial.common.ready', { percent: completion })}</strong>
          <span>{t('tutorial.helpCreate.readyText')}</span>
        </div>
      </div>

      <div className="help-create-card">
        <div className="alert alert-success tutorial-alert">
          {t('tutorial.helpCreate.info')}
        </div>

        {previewed && (
          <div className="alert alert-success">
            {t('tutorial.helpCreate.previewed')}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className={`form-group ${activeStep?.target === 'title' ? 'tutorial-tour-highlight' : ''}`}>
            <label htmlFor="tutorial-title">{t('tutorial.helpCreate.titleLabel')}</label>
            <input
              id="tutorial-title"
              name="title"
              type="text"
              placeholder={t('tutorial.helpCreate.titlePlaceholder')}
              value={form.title}
              onChange={handleChange}
              className={errors.title ? 'input-error' : ''}
            />
            {errors.title && <span className="field-error">{errors.title}</span>}
          </div>

          <div className={`form-group ${activeStep?.target === 'description' ? 'tutorial-tour-highlight' : ''}`}>
            <label htmlFor="tutorial-description">{t('tutorial.helpCreate.descLabel')}</label>
            <textarea
              id="tutorial-description"
              name="description"
              className={`help-create-textarea${errors.description ? ' input-error' : ''}`}
              placeholder={t('tutorial.helpCreate.descPlaceholder')}
              value={form.description}
              onChange={handleChange}
              rows={5}
            />
            {errors.description && <span className="field-error">{errors.description}</span>}
          </div>

          <div className={`help-create-row ${activeStep?.target === 'category' ? 'tutorial-tour-highlight' : ''}`}>
            <div className="form-group help-create-half">
              <label htmlFor="tutorial-category">{t('tutorial.helpCreate.category')}</label>
              <select id="tutorial-category" name="category" value={form.category} onChange={handleChange}>
                {CATEGORIES.map((category) => (
                  <option key={category.value} value={category.value}>{category.label}</option>
                ))}
              </select>
            </div>

            <div className="form-group help-create-half">
              <label htmlFor="tutorial-urgency">{t('tutorial.helpCreate.urgency')}</label>
              <select id="tutorial-urgency" name="urgency" value={form.urgency} onChange={handleChange}>
                {URGENCIES.map((urgency) => (
                  <option key={urgency.value} value={urgency.value}>{urgency.label}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="tutorial-location">{t('tutorial.helpCreate.locationNote')} <span className="optional-tag">{t('tutorial.helpCreate.recommended')}</span></label>
            <input
              id="tutorial-location"
              name="location_text"
              type="text"
              placeholder={t('tutorial.helpCreate.locationPlaceholder')}
              value={form.location_text}
              onChange={handleChange}
            />
          </div>

          <div className="form-group">
            <label>{t('tutorial.helpCreate.imagesGps')} <span className="optional-tag">{t('tutorial.helpCreate.afterSignIn')}</span></label>
            <div className="help-create-location-row">
              <button type="button" className="btn btn-secondary btn-sm" disabled>{t('tutorial.helpCreate.addPhoto')}</button>
              <button type="button" className="btn btn-secondary btn-sm" disabled>{t('tutorial.helpCreate.useLocation')}</button>
            </div>
          </div>

          <button
            type="submit"
            className={`btn btn-primary btn-block ${activeStep?.target === 'submit' ? 'tutorial-tour-highlight' : ''}`}
          >
            {t('tutorial.helpCreate.save')}
          </button>
        </form>
      </div>
    </div>
  );
}
