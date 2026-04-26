/**
 * HelpRequestCreatePage — form for submitting a new help request.
 *
 * Route: /help-requests/new
 * Sends POST /help-requests/ with title, description, category, urgency,
 * optional location_text, and optional lat/lng from the browser geolocation API.
 * On success, redirects to the newly created request's detail page.
 */

import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { createHelpRequest, uploadHelpRequestImages, resolveImageUrl } from '../services/api';
import { useTranslation } from 'react-i18next';

export default function HelpRequestCreatePage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const fileInputRef = useRef(null);
  const { t } = useTranslation(); // Initialize hook

  // Move arrays inside the component to access 't' for labels
  const CATEGORIES = [
    { value: 'MEDICAL', label: t('help_request_create.categories.medical') },
    { value: 'FOOD', label: t('help_request_create.categories.food') },
    { value: 'SHELTER', label: t('help_request_create.categories.shelter') },
    { value: 'TRANSPORT', label: t('help_request_create.categories.transport') },
    { value: 'OTHER', label: t('help_request_create.categories.other') },
  ];

  const URGENCIES = [
    { value: 'LOW', label: t('help_request_create.urgencies.low') },
    { value: 'MEDIUM', label: t('help_request_create.urgencies.medium') },
    { value: 'HIGH', label: t('help_request_create.urgencies.high') },
  ];

  /* ── Form state ─────────────────────────────────────────────────────────── */
  const [form, setForm] = useState({
    title: '',
    description: '',
    category: 'MEDICAL',
    urgency: 'MEDIUM',
    location_text: '',
    latitude: '',
    longitude: '',
  });

  const [errors, setErrors] = useState({});
  const [globalError, setGlobalError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  /* ── Image upload state ─────────────────────────────────────────────────── */
  const [uploadedImages, setUploadedImages] = useState([]);
  const [uploading, setUploading] = useState(false);

  /* ── Geolocation state ──────────────────────────────────────────────────── */
  const [locating, setLocating] = useState(false);
  const [locationStatus, setLocationStatus] = useState('');

  const handleFileSelect = async (e) => {
    const files = Array.from(e.target.files);
    if (!files.length) return;

    setUploading(true);
    setGlobalError('');
    const { ok, data } = await uploadHelpRequestImages(files);
    setUploading(false);

    if (ok) {
      setUploadedImages((prev) => [...prev, ...data.urls]);
    } else {
      setGlobalError(data?.detail || t('help_request_create.errors.upload_failed'));
    }

    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const removeUploadedImage = (index) => {
    setUploadedImages((prev) => prev.filter((_, i) => i !== index));
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => {
        const copy = { ...prev };
        delete copy[name];
        return copy;
      });
    }
  };

  const handleUseLocation = () => {
    if (!navigator.geolocation) {
      setLocationStatus(t('help_request_create.location_status.not_supported'));
      return;
    }

    setLocating(true);
    setLocationStatus('');

    navigator.geolocation.getCurrentPosition(
        (position) => {
          setForm((prev) => ({
            ...prev,
            latitude: position.coords.latitude.toFixed(6),
            longitude: position.coords.longitude.toFixed(6),
          }));
          setLocationStatus(t('help_request_create.location_status.captured'));
          setLocating(false);
        },
        (err) => {
          setLocationStatus(t('help_request_create.location_status.failed', { message: err.message }));
          setLocating(false);
        },
    );
  };

  /* ── Client-side validation ─────────────────────────────────────────────── */
  const validate = () => {
    const errs = {};
    if (!form.title.trim()) errs.title = t('help_request_create.errors.title_required');
    if (!form.description.trim()) errs.description = t('help_request_create.errors.description_required');
    return errs;
  };

  /* ── Submit handler ─────────────────────────────────────────────────────── */
  const handleSubmit = async (e) => {
    e.preventDefault();
    setGlobalError('');
    setErrors({});

    const clientErrors = validate();
    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors);
      return;
    }

    setSubmitting(true);

    const payload = {
      title: form.title.trim(),
      description: form.description.trim(),
      category: form.category,
      urgency: form.urgency,
    };
    if (form.location_text.trim()) payload.location_text = form.location_text.trim();
    if (form.latitude && form.longitude) {
      payload.latitude = form.latitude;
      payload.longitude = form.longitude;
    }
    if (uploadedImages.length > 0) payload.image_urls = uploadedImages;

    const { ok, data } = await createHelpRequest(payload);
    setSubmitting(false);

    if (ok) {
      navigate(`/help-requests/${data.id}`);
    } else {
      if (typeof data === 'object' && data !== null) {
        const mapped = {};
        for (const [field, msgs] of Object.entries(data)) {
          if (field === 'detail' || field === 'non_field_errors') continue;
          mapped[field] = Array.isArray(msgs) ? msgs.join(' ') : msgs;
        }
        if (Object.keys(mapped).length > 0) setErrors(mapped);
      }
      setGlobalError(
          data.detail || data.non_field_errors?.[0] || t('help_request_create.errors.submit_failed'),
      );
    }
  };

  if (!user) return null;

  return (
      <div className="page help-create-page">
        <header className="help-requests-header">
          <button
              className="btn btn-secondary btn-sm"
              onClick={() => navigate('/help-requests')}
          >
            &larr; {t('help_request_create.header.back')}
          </button>
          <h2 className="gradient-text">{t('help_request_create.header.title')}</h2>
        </header>

        <div className="help-create-card">
          {globalError && <div className="alert alert-error">{globalError}</div>}

          <form onSubmit={handleSubmit} noValidate>
            <div className="form-group">
              <label htmlFor="title">{t('help_request_create.labels.title')}</label>
              <input
                  id="title"
                  name="title"
                  type="text"
                  placeholder={t('help_request_create.placeholders.title')}
                  value={form.title}
                  onChange={handleChange}
                  className={errors.title ? 'input-error' : ''}
              />
              {errors.title && <span className="field-error">{errors.title}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="description">{t('help_request_create.labels.description')}</label>
              <textarea
                  id="description"
                  name="description"
                  className={`help-create-textarea${errors.description ? ' input-error' : ''}`}
                  placeholder={t('help_request_create.placeholders.description')}
                  value={form.description}
                  onChange={handleChange}
                  rows={5}
              />
              {errors.description && (
                  <span className="field-error">{errors.description}</span>
              )}
            </div>

            <div className="form-group">
              <label>{t('help_request_create.labels.images')} <span className="optional-tag">{t('help_request_create.labels.optional')}</span></label>

              <div className="image-upload-area">
                <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={uploading}
                >
                  {uploading ? t('help_request_create.actions.uploading') : t('help_request_create.actions.upload')}
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
                              title={t('help_request_create.actions.remove')}
                          >&times;</button>
                        </div>
                    ))}
                  </div>
              )}
            </div>

            <div className="help-create-row">
              <div className="form-group help-create-half">
                <label htmlFor="category">{t('help_request_create.labels.category')}</label>
                <select
                    id="category"
                    name="category"
                    value={form.category}
                    onChange={handleChange}
                >
                  {CATEGORIES.map((c) => (
                      <option key={c.value} value={c.value}>{c.label}</option>
                  ))}
                </select>
                {errors.category && (
                    <span className="field-error">{errors.category}</span>
                )}
              </div>

              <div className="form-group help-create-half">
                <label htmlFor="urgency">{t('help_request_create.labels.urgency')}</label>
                <select
                    id="urgency"
                    name="urgency"
                    value={form.urgency}
                    onChange={handleChange}
                >
                  {URGENCIES.map((u) => (
                      <option key={u.value} value={u.value}>{u.label}</option>
                  ))}
                </select>
                {errors.urgency && (
                    <span className="field-error">{errors.urgency}</span>
                )}
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="location_text">
                {t('help_request_create.labels.location_text')} <span className="optional-tag">{t('help_request_create.labels.optional')}</span>
              </label>
              <input
                  id="location_text"
                  name="location_text"
                  type="text"
                  placeholder={t('help_request_create.placeholders.location_text')}
                  value={form.location_text}
                  onChange={handleChange}
              />
            </div>

            <div className="form-group">
              <label>
                {t('help_request_create.labels.coordinates')} <span className="optional-tag">{t('help_request_create.labels.optional')}</span>
              </label>
              <div className="help-create-location-row">
                <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={handleUseLocation}
                    disabled={locating}
                >
                  {locating ? t('help_request_create.actions.locating') : t('help_request_create.actions.use_location')}
                </button>
                {form.latitude && form.longitude && (
                    <span className="help-create-coords">
                  {form.latitude}, {form.longitude}
                </span>
                )}
              </div>
              {locationStatus && (
                  <span className="help-create-location-status">{locationStatus}</span>
              )}
            </div>

            <button
                type="submit"
                className="btn btn-primary btn-block"
                disabled={submitting || uploading}
            >
              {submitting ? t('help_request_create.actions.submitting') : t('help_request_create.actions.submit')}
            </button>
          </form>
        </div>
      </div>
  );
}