import { useState, useRef } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { createPost, uploadImages, resolveImageUrl } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';

export default function PostCreatePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const selectedHub = user?.hub;
  const fileInputRef = useRef(null);
  const { t } = useTranslation(); // Initialize hook

  // Move TYPE_LABELS inside to map with translations
  const TYPE_LABELS = {
    GLOBAL: t('post_create.types.global'),
    STANDARD: t('post_create.types.standard'),
    URGENT: t('post_create.types.urgent')
  };

  const forumType = ['GLOBAL', 'STANDARD', 'URGENT'].includes(location.state?.forumType)
      ? location.state.forumType
      : 'GLOBAL';

  const [form, setForm] = useState({
    title: '',
    content: '',
    image_urls: '',
  });
  const [uploadedImages, setUploadedImages] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
    if (error) setError('');
  };

  const handleFileSelect = async (e) => {
    const files = Array.from(e.target.files);
    if (!files.length) return;

    setUploading(true);
    setError('');
    const { ok, data } = await uploadImages(files);
    setUploading(false);

    if (ok) {
      setUploadedImages((prev) => [...prev, ...data.urls]);
    } else {
      setError(data?.detail || t('post_create.errors.upload_failed'));
    }

    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const removeUploadedImage = (index) => {
    setUploadedImages((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!form.title.trim()) { setError(t('post_create.errors.title_required')); return; }
    if (!form.content.trim()) { setError(t('post_create.errors.content_required')); return; }
    if (forumType !== 'GLOBAL' && !selectedHub) {
      setError(t('post_create.errors.select_hub'));
      return;
    }

    setSubmitting(true);

    const pastedUrls = form.image_urls
        .split('\n')
        .map((u) => u.trim())
        .filter(Boolean);

    const allImages = [...uploadedImages, ...pastedUrls];

    const payload = {
      forum_type: forumType,
      title: form.title.trim(),
      content: form.content.trim(),
      image_urls: allImages,
    };
    if (forumType !== 'GLOBAL') {
      payload.hub = selectedHub.id;
    }

    const { ok, data } = await createPost(payload);
    setSubmitting(false);

    if (ok) {
      navigate(`/forum?tab=${forumType}`);
    } else {
      const msg = data?.detail || data?.title?.[0] || data?.content?.[0] || t('post_create.errors.create_failed');
      setError(msg);
    }
  };

  return (
      <div className="page auth-page">
        <div className="auth-card" style={{ maxWidth: 560 }}>
          {/* 4. Inject the translated type directly into the header string */}
          <h2 className="auth-title">{t('post_create.header.title', { type: TYPE_LABELS[forumType] })}</h2>
          <p className="auth-subtitle">
            {forumType === 'GLOBAL'
                ? t('post_create.header.subtitle_global')
                : t('post_create.header.subtitle_hub', { hub: selectedHub?.name || t('post_create.header.subtitle_hub_fallback') })}
          </p>

          {error && <div className="alert alert-error">{error}</div>}

          <form onSubmit={handleSubmit} noValidate>
            <div className="form-group">
              <label htmlFor="title">{t('post_create.labels.title')}</label>
              <input
                  id="title"
                  name="title"
                  type="text"
                  placeholder={t('post_create.placeholders.title')}
                  value={form.title}
                  onChange={handleChange}
              />
            </div>

            <div className="form-group">
              <label htmlFor="content">{t('post_create.labels.content')}</label>
              <textarea
                  id="content"
                  name="content"
                  rows={6}
                  placeholder={t('post_create.placeholders.content')}
                  value={form.content}
                  onChange={handleChange}
                  className="post-edit-content"
              />
            </div>

            <div className="form-group">
              <label>{t('post_create.labels.images')} <span className="optional-tag">{t('post_create.labels.optional')}</span></label>

              <div className="image-upload-area">
                <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={uploading}
                >
                  {uploading ? t('post_create.actions.uploading') : t('post_create.actions.upload')}
                </button>
                <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/jpeg,image/png,image/gif,image/webp"
                    multiple
                    onChange={handleFileSelect}
                    style={{ display: 'none' }}
                />
              </div>

              {uploadedImages.length > 0 && (
                  <div className="image-preview-list">
                    {uploadedImages.map((url, i) => (
                        <div className="image-preview-item" key={i}>
                          <img src={resolveImageUrl(url)} alt={`Upload ${i + 1}`} className="image-preview-thumb" />
                          <button
                              type="button"
                              className="image-preview-remove"
                              onClick={() => removeUploadedImage(i)}
                              title={t('post_create.actions.remove')}
                          >&times;</button>
                        </div>
                    ))}
                  </div>
              )}

              <textarea
                  id="image_urls"
                  name="image_urls"
                  rows={2}
                  placeholder={t('post_create.placeholders.image_urls')}
                  value={form.image_urls}
                  onChange={handleChange}
                  className="post-edit-content"
                  style={{ marginTop: '0.5rem' }}
              />
            </div>

            <button
                type="submit"
                className="btn btn-primary btn-block"
                disabled={submitting || uploading}
            >
              {submitting ? t('post_create.actions.creating') : t('post_create.actions.create')}
            </button>
          </form>

          <p className="auth-footer">
            <Link to={`/forum?tab=${forumType}`} className="link">&larr; {t('post_create.actions.back')}</Link>
          </p>
        </div>
      </div>
  );
}