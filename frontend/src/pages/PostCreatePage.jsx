import { useState, useRef } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { createPost, uploadImages } from '../services/api';
import { useHub } from '../context/HubContext';

const TYPE_LABELS = { GLOBAL: 'Global', STANDARD: 'Standard', URGENT: 'Urgent' };

export default function PostCreatePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { selectedHub } = useHub();
  const fileInputRef = useRef(null);

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
      setError(data?.detail || 'Image upload failed.');
    }

    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const removeUploadedImage = (index) => {
    setUploadedImages((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!form.title.trim()) { setError('Title is required.'); return; }
    if (!form.content.trim()) { setError('Content is required.'); return; }
    if (forumType !== 'GLOBAL' && !selectedHub) {
      setError('Please select a hub first.');
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
      navigate('/forum', { state: { forumTab: forumType } });
    } else {
      const msg = data?.detail || data?.title?.[0] || data?.content?.[0] || 'Failed to create post.';
      setError(msg);
    }
  };

  return (
    <div className="page auth-page">
      <div className="auth-card" style={{ maxWidth: 560 }}>
        <h2 className="auth-title">New {TYPE_LABELS[forumType]} Post</h2>
        <p className="auth-subtitle">
          {forumType === 'GLOBAL'
            ? 'Visible to everyone (all hubs)'
            : `Posting to ${selectedHub?.name || '…'}`}
        </p>

        {error && <div className="alert alert-error">{error}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label htmlFor="title">Title</label>
            <input
              id="title"
              name="title"
              type="text"
              placeholder="What's happening?"
              value={form.title}
              onChange={handleChange}
            />
          </div>

          <div className="form-group">
            <label htmlFor="content">Content</label>
            <textarea
              id="content"
              name="content"
              rows={6}
              placeholder="Describe the situation, share info, or ask for help..."
              value={form.content}
              onChange={handleChange}
              className="post-edit-content"
            />
          </div>

          <div className="form-group">
            <label>Images <span className="optional-tag">optional</span></label>

            <div className="image-upload-area">
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploading}
              >
                {uploading ? 'Uploading...' : 'Upload from Device'}
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
                    <img src={url} alt={`Upload ${i + 1}`} className="image-preview-thumb" />
                    <button
                      type="button"
                      className="image-preview-remove"
                      onClick={() => removeUploadedImage(i)}
                      title="Remove"
                    >&times;</button>
                  </div>
                ))}
              </div>
            )}

            <textarea
              id="image_urls"
              name="image_urls"
              rows={2}
              placeholder="Or paste image URLs here, one per line"
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
            {submitting ? 'Creating...' : 'Create Post'}
          </button>
        </form>

        <p className="auth-footer">
          <Link to="/forum" state={{ forumTab: forumType }} className="link">&larr; Back to Forum</Link>
        </p>
      </div>
    </div>
  );
}
