/**
 * HelpRequestCreatePage — form for submitting a new help request.
 *
 * Route: /help-requests/new
 * Sends POST /help-requests/ with title, description, category, urgency,
 * optional location_text, and optional lat/lng from the browser geolocation API.
 * On success, redirects to the newly created request's detail page.
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { createHelpRequest } from '../services/api';

/** Category options — matches backend Category.choices. */
const CATEGORIES = [
  { value: 'MEDICAL', label: 'Medical' },
  { value: 'FOOD', label: 'Food' },
  { value: 'SHELTER', label: 'Shelter' },
  { value: 'TRANSPORT', label: 'Transport' },
];

/** Urgency options — matches backend Urgency.choices. */
const URGENCIES = [
  { value: 'LOW', label: 'Low' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH', label: 'High' },
];

export default function HelpRequestCreatePage() {
  const { user } = useAuth();
  const navigate = useNavigate();

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

  /* ── Geolocation state ──────────────────────────────────────────────────── */
  const [locating, setLocating] = useState(false);
  const [locationStatus, setLocationStatus] = useState('');

  /** Generic change handler — clears field error on edit. */
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

  /** Uses the browser Geolocation API to populate lat/lng fields. */
  const handleUseLocation = () => {
    if (!navigator.geolocation) {
      setLocationStatus('Geolocation is not supported by your browser.');
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
        setLocationStatus('Location captured.');
        setLocating(false);
      },
      (err) => {
        setLocationStatus(`Could not get location: ${err.message}`);
        setLocating(false);
      },
    );
  };

  /* ── Client-side validation ─────────────────────────────────────────────── */
  const validate = () => {
    const errs = {};
    if (!form.title.trim()) errs.title = 'Title is required.';
    if (!form.description.trim()) errs.description = 'Description is required.';
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

    // Build payload — only include optional fields when they have values.
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

    const { ok, data } = await createHelpRequest(payload);
    setSubmitting(false);

    if (ok) {
      // Redirect to the newly created request's detail page.
      navigate(`/help-requests/${data.id}`);
    } else {
      // Map per-field errors from the API response.
      if (typeof data === 'object' && data !== null) {
        const mapped = {};
        for (const [field, msgs] of Object.entries(data)) {
          if (field === 'detail' || field === 'non_field_errors') continue;
          mapped[field] = Array.isArray(msgs) ? msgs.join(' ') : msgs;
        }
        if (Object.keys(mapped).length > 0) setErrors(mapped);
      }
      setGlobalError(
        data.detail || data.non_field_errors?.[0] || 'Failed to create help request.',
      );
    }
  };

  if (!user) return null;

  return (
    <div className="page help-create-page">
      {/* Header */}
      <header className="help-requests-header">
        <button
          className="btn btn-secondary btn-sm"
          onClick={() => navigate('/help-requests')}
        >
          &larr; Back
        </button>
        <h2 className="gradient-text">New Help Request</h2>
      </header>

      {/* Form card */}
      <div className="help-create-card">
        {globalError && <div className="alert alert-error">{globalError}</div>}

        <form onSubmit={handleSubmit} noValidate>
          {/* Title */}
          <div className="form-group">
            <label htmlFor="title">Title</label>
            <input
              id="title"
              name="title"
              type="text"
              placeholder="Brief summary of your request"
              value={form.title}
              onChange={handleChange}
              className={errors.title ? 'input-error' : ''}
            />
            {errors.title && <span className="field-error">{errors.title}</span>}
          </div>

          {/* Description */}
          <div className="form-group">
            <label htmlFor="description">Description</label>
            <textarea
              id="description"
              name="description"
              className={`help-create-textarea${errors.description ? ' input-error' : ''}`}
              placeholder="Describe what help you need..."
              value={form.description}
              onChange={handleChange}
              rows={5}
            />
            {errors.description && (
              <span className="field-error">{errors.description}</span>
            )}
          </div>

          {/* Category & Urgency — side by side on desktop */}
          <div className="help-create-row">
            <div className="form-group help-create-half">
              <label htmlFor="category">Category</label>
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
              <label htmlFor="urgency">Urgency</label>
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

          {/* Location text — optional */}
          <div className="form-group">
            <label htmlFor="location_text">
              Location description <span className="optional-tag">optional</span>
            </label>
            <input
              id="location_text"
              name="location_text"
              type="text"
              placeholder="e.g. Near Central Park entrance"
              value={form.location_text}
              onChange={handleChange}
            />
          </div>

          {/* Geolocation — optional */}
          <div className="form-group">
            <label>
              Coordinates <span className="optional-tag">optional</span>
            </label>
            <div className="help-create-location-row">
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={handleUseLocation}
                disabled={locating}
              >
                {locating ? 'Locating...' : 'Use my location'}
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

          {/* Submit */}
          <button
            type="submit"
            className="btn btn-primary btn-block"
            disabled={submitting}
          >
            {submitting ? 'Submitting...' : 'Submit Request'}
          </button>
        </form>
      </div>
    </div>
  );
}
