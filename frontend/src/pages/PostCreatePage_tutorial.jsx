import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import useTutorialGuide from '../components/TutorialGuide';
import { saveTutorialPost } from '../utils/tutorialStorage';

export default function PostCreatePageTutorial() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const POST_CREATE_TOUR_STEPS = [
    { target: 'type', title: t('tutorial.postCreate.steps.typeTitle'), text: t('tutorial.postCreate.steps.typeText') },
    { target: 'title', title: t('tutorial.postCreate.steps.titleTitle'), text: t('tutorial.postCreate.steps.titleText') },
    { target: 'content', title: t('tutorial.postCreate.steps.contentTitle'), text: t('tutorial.postCreate.steps.contentText') },
    { target: 'preview', title: t('tutorial.postCreate.steps.previewTitle'), text: t('tutorial.postCreate.steps.previewText') },
    { target: 'submit', title: t('tutorial.postCreate.steps.submitTitle'), text: t('tutorial.postCreate.steps.submitText') },
  ];
  const TYPES = [
    { value: 'GLOBAL', label: t('forum.tabs.global'), hint: t('tutorial.postCreate.typeHints.global') },
    { value: 'STANDARD', label: t('tutorial.forum.standardHub'), hint: t('tutorial.postCreate.typeHints.standard') },
    { value: 'URGENT', label: t('tutorial.forum.urgentHub'), hint: t('tutorial.postCreate.typeHints.urgent') },
  ];
  const [form, setForm] = useState({
    forumType: 'GLOBAL',
    title: 'Charging station open at the community center',
    content: 'Power is still out in nearby buildings. Volunteers can help people charge phones until 18:00. Bring your own cable if possible.',
    image_urls: '',
  });
  const [errors, setErrors] = useState({});
  const [saved, setSaved] = useState(false);
  const { activeStep, GuidePanel, RestartButton } = useTutorialGuide({
    storageKey: 'emergencyHubForumCreateTutorialSeen',
    steps: POST_CREATE_TOUR_STEPS,
  });

  const selectedType = TYPES.find((type) => type.value === form.forumType) || TYPES[0];

  const completion = useMemo(() => {
    const fields = ['forumType', 'title', 'content'];
    const filled = fields.filter((field) => String(form[field]).trim()).length;
    return Math.round((filled / fields.length) * 100);
  }, [form]);

  const imageUrls = useMemo(() => (
    form.image_urls
      .split('\n')
      .map((url) => url.trim())
      .filter(Boolean)
  ), [form.image_urls]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((current) => ({ ...current, [name]: value }));
    setSaved(false);
    if (errors[name]) {
      setErrors((current) => {
        const next = { ...current };
        delete next[name];
        return next;
      });
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const nextErrors = {};
    if (!form.title.trim()) nextErrors.title = t('tutorial.postCreate.titleError');
    if (!form.content.trim()) nextErrors.content = t('tutorial.postCreate.contentError');
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    saveTutorialPost({
      title: form.title.trim(),
      body: form.content.trim(),
      forumType: form.forumType,
      imageUrls,
      author: 'You',
      role: 'STANDARD',
      status: 'Safe',
      comments: 0,
      upvotes: 1,
      createdLabel: 'just now',
      local: true,
    });
    setSaved(true);
    navigate('/tutorial/forum');
  };

  return (
    <div className="page auth-page tutorial-page">
      <div className="auth-card tutorial-create-post-card">
        <header className="tutorial-form-header">
          <button className="btn btn-secondary btn-sm" onClick={() => navigate('/tutorial/forum')}>
          {t('tutorial.common.backForum')}
          </button>
          <div className="tutorial-header-actions">
            {RestartButton}
          </div>
        </header>

        <h2 className="auth-title gradient-text">{t('tutorial.postCreate.title')}</h2>
        <p className="auth-subtitle">
          {t('tutorial.postCreate.subtitle')}
        </p>

        {GuidePanel}

        <div className="tutorial-scenario-strip">
          <div>
          <strong>{t('tutorial.common.currentSituation')}</strong>
          <span>{t('tutorial.postCreate.situationText')}</span>
          </div>
          <div>
            <strong>{t('tutorial.common.ready', { percent: completion })}</strong>
            <span>{t('tutorial.postCreate.readyText')}</span>
          </div>
        </div>

        {saved && (
          <div className="alert alert-success">
            {t('tutorial.postCreate.saved')}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className={`form-group ${activeStep?.target === 'type' ? 'tutorial-tour-highlight' : ''}`}>
            <label htmlFor="tutorial-forum-type">{t('tutorial.postCreate.forumType')}</label>
            <select id="tutorial-forum-type" name="forumType" value={form.forumType} onChange={handleChange}>
              {TYPES.map((type) => (
                <option key={type.value} value={type.value}>{type.label}</option>
              ))}
            </select>
            <span className="tutorial-field-hint">{selectedType.hint}</span>
          </div>

          <div className={`form-group ${activeStep?.target === 'title' ? 'tutorial-tour-highlight' : ''}`}>
            <label htmlFor="tutorial-post-title">{t('help_request_create.labels.title')}</label>
            <input
              id="tutorial-post-title"
              name="title"
              type="text"
              placeholder={t('tutorial.postCreate.shortTitle')}
              value={form.title}
              onChange={handleChange}
              className={errors.title ? 'input-error' : ''}
            />
            {errors.title && <span className="field-error">{errors.title}</span>}
          </div>

          <div className={`form-group ${activeStep?.target === 'content' ? 'tutorial-tour-highlight' : ''}`}>
            <label htmlFor="tutorial-post-content">{t('tutorial.postCreate.content')}</label>
            <textarea
              id="tutorial-post-content"
              name="content"
              rows={6}
              placeholder={t('tutorial.postCreate.contentPlaceholder')}
              value={form.content}
              onChange={handleChange}
              className={`post-edit-content${errors.content ? ' input-error' : ''}`}
            />
            {errors.content && <span className="field-error">{errors.content}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="tutorial-image-urls">{t('tutorial.postCreate.imageLinks')} <span className="optional-tag">{t('tutorial.postCreate.optional')}</span></label>
            <textarea
              id="tutorial-image-urls"
              name="image_urls"
              rows={2}
              placeholder={t('tutorial.postCreate.imagePlaceholder')}
              value={form.image_urls}
              onChange={handleChange}
              className="post-edit-content"
            />
          </div>

          <div className={`tutorial-post-preview ${activeStep?.target === 'preview' ? 'tutorial-tour-highlight' : ''}`}>
            <span className="badge">{selectedType.label}</span>
            <h3>{form.title || t('tutorial.postCreate.previewTitleFallback')}</h3>
            <p>{form.content || t('tutorial.postCreate.previewBodyFallback')}</p>
            {imageUrls.length > 0 && (
              <div className="tutorial-preview-images">
                {imageUrls.slice(0, 3).map((url) => (
                  <span key={url}>{url}</span>
                ))}
              </div>
            )}
          </div>

          <button
            type="submit"
            className={`btn btn-primary btn-block ${activeStep?.target === 'submit' ? 'tutorial-tour-highlight' : ''}`}
          >
            {t('tutorial.postCreate.save')}
          </button>
        </form>
      </div>
    </div>
  );
}
