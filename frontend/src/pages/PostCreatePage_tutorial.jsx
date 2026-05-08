import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import useTutorialGuide from '../components/TutorialGuide';
import { saveTutorialPost } from '../utils/tutorialStorage';

const POST_CREATE_TOUR_STEPS = [
  {
    target: 'type',
    title: 'Choose where the post belongs',
    text: 'Global posts are broad updates. Hub posts are local. Urgent posts should be reserved for time-sensitive warnings.',
  },
  {
    target: 'title',
    title: 'Use a scan-friendly title',
    text: 'A clear title helps neighbors understand the update before opening the full post.',
  },
  {
    target: 'content',
    title: 'Write the useful details',
    text: 'Share what happened, where it happened, and what neighbors should do next.',
  },
  {
    target: 'preview',
    title: 'Review the preview',
    text: 'The preview shows how your post will look in the forum.',
  },
  {
    target: 'submit',
    title: 'Save it safely',
    text: 'Save the post when the title and details are clear.',
  },
];

const TYPES = [
  { value: 'GLOBAL', label: 'Global', hint: 'Public neighborhood-wide update' },
  { value: 'STANDARD', label: 'Standard hub', hint: 'Local update for your hub' },
  { value: 'URGENT', label: 'Urgent hub', hint: 'Immediate safety warning' },
];

export default function PostCreatePageTutorial() {
  const navigate = useNavigate();
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
    if (!form.title.trim()) nextErrors.title = 'Add a short title before saving.';
    if (!form.content.trim()) nextErrors.content = 'Add details neighbors can act on.';
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
          &larr; Forum
          </button>
          <div className="tutorial-header-actions">
            {RestartButton}
          </div>
        </header>

        <h2 className="auth-title gradient-text">New Forum Post</h2>
        <p className="auth-subtitle">
          Compose a public update for neighbors.
        </p>

        {GuidePanel}

        <div className="tutorial-scenario-strip">
          <div>
            <strong>Scenario</strong>
            <span>Neighbors need to know where they can charge phones during the outage.</span>
          </div>
          <div>
            <strong>{completion}% ready</strong>
            <span>Clear posts reduce confusion and repeated questions.</span>
          </div>
        </div>

        {saved && (
          <div className="alert alert-success">
            Saved. It will appear in the forum.
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className={`form-group ${activeStep?.target === 'type' ? 'tutorial-tour-highlight' : ''}`}>
            <label htmlFor="tutorial-forum-type">Forum type</label>
            <select id="tutorial-forum-type" name="forumType" value={form.forumType} onChange={handleChange}>
              {TYPES.map((type) => (
                <option key={type.value} value={type.value}>{type.label}</option>
              ))}
            </select>
            <span className="tutorial-field-hint">{selectedType.hint}</span>
          </div>

          <div className={`form-group ${activeStep?.target === 'title' ? 'tutorial-tour-highlight' : ''}`}>
            <label htmlFor="tutorial-post-title">Title</label>
            <input
              id="tutorial-post-title"
              name="title"
              type="text"
              placeholder="Short update title"
              value={form.title}
              onChange={handleChange}
              className={errors.title ? 'input-error' : ''}
            />
            {errors.title && <span className="field-error">{errors.title}</span>}
          </div>

          <div className={`form-group ${activeStep?.target === 'content' ? 'tutorial-tour-highlight' : ''}`}>
            <label htmlFor="tutorial-post-content">Content</label>
            <textarea
              id="tutorial-post-content"
              name="content"
              rows={6}
              placeholder="What happened? Where? What should neighbors do?"
              value={form.content}
              onChange={handleChange}
              className={`post-edit-content${errors.content ? ' input-error' : ''}`}
            />
            {errors.content && <span className="field-error">{errors.content}</span>}
          </div>

          <div className="form-group">
            <label htmlFor="tutorial-image-urls">Image links <span className="optional-tag">optional</span></label>
            <textarea
              id="tutorial-image-urls"
              name="image_urls"
              rows={2}
              placeholder="One image link per line"
              value={form.image_urls}
              onChange={handleChange}
              className="post-edit-content"
            />
          </div>

          <div className={`tutorial-post-preview ${activeStep?.target === 'preview' ? 'tutorial-tour-highlight' : ''}`}>
            <span className="badge">{selectedType.label}</span>
            <h3>{form.title || 'Your post title'}</h3>
            <p>{form.content || 'Your post details will appear here.'}</p>
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
            Save post
          </button>
        </form>
      </div>
    </div>
  );
}
