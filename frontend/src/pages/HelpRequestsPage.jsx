/**
 * HelpRequestsPage — lists active help requests in the user's hub.
 *
 * Fetches from GET /help-requests/?hub_id=...&category=... and displays
 * results as glassmorphic cards. Supports category filtering via a
 * horizontal button bar. Shows loading, error, and empty states.
 */

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getHelpRequests } from '../services/api';

/** Category options for the filter bar — matches backend Category.choices. */
const CATEGORIES = [
  { value: '', label: 'All Categories' },
  { value: 'MEDICAL', label: 'Medical' },
  { value: 'FOOD', label: 'Food' },
  { value: 'SHELTER', label: 'Shelter' },
  { value: 'TRANSPORT', label: 'Transport' },
];

/** Maps urgency values to badge CSS classes for visual distinction. */
const URGENCY_CLASSES = {
  LOW: 'badge-muted',
  MEDIUM: 'badge-accent',
  HIGH: 'badge-urgency-high',
};

const URGENCY_LABELS = { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High' };

const STATUS_LABELS = {
  OPEN: 'Open',
  EXPERT_RESPONDING: 'Expert Responding',
  RESOLVED: 'Resolved',
};

/**
 * Formats an ISO date string into a human-readable relative time.
 * Shows "Just now", "5m ago", "3h ago", "2d ago", or a locale date.
 */
function formatDate(isoString) {
  const date = new Date(isoString);
  const diffMs = Date.now() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays < 7) return `${diffDays}d ago`;
  return date.toLocaleDateString();
}

export default function HelpRequestsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [category, setCategory] = useState('');

  // Re-fetch whenever the user loads or the category filter changes.
  useEffect(() => {
    if (!user) return;

    setLoading(true);
    setError('');

    const params = {};
    if (user.hub?.id) params.hub_id = user.hub.id;
    if (category) params.category = category;

    getHelpRequests(params)
      .then(({ ok, data }) => {
        if (ok) {
          setRequests(data);
        } else {
          setError(data.detail || 'Failed to load help requests.');
        }
      })
      .catch(() => setError('Network error. Please try again.'))
      .finally(() => setLoading(false));
  }, [user, category]);

  if (!user) return null;

  return (
    <div className="page help-requests-page">
      {/* Header with back button, title, and create button */}
      <header className="help-requests-header">
        <button
          className="btn btn-secondary btn-sm"
          onClick={() => navigate('/dashboard')}
        >
          &larr; Dashboard
        </button>
        <h2 className="gradient-text">Help Requests</h2>
        <button
          className="btn btn-primary btn-sm"
          onClick={() => navigate('/help-requests/new')}
        >
          + New Request
        </button>
      </header>

      {/* Hub context — tells the user which hub they're viewing */}
      {user.hub && (
        <p className="help-requests-hub">
          Showing requests for <strong>{user.hub.name}</strong>
        </p>
      )}

      {/* Category filter bar */}
      <div className="help-requests-filters">
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

      {/* Error state */}
      {error && <div className="alert alert-error">{error}</div>}

      {/* Loading state */}
      {loading && (
        <div className="help-requests-loading">
          <p>Loading help requests...</p>
        </div>
      )}

      {/* Empty state */}
      {!loading && !error && requests.length === 0 && (
        <div className="help-requests-empty">
          <span className="help-requests-empty-icon">🔍</span>
          <h3>No help requests found</h3>
          <p>
            {category
              ? 'Try selecting a different category.'
              : 'There are no help requests in your hub yet.'}
          </p>
        </div>
      )}

      {/* Request cards */}
      {!loading && !error && requests.length > 0 && (
        <div className="help-requests-list">
          {requests.map((req) => (
            <div
              className="help-request-card dashboard-card-link"
              key={req.id}
              onClick={() => navigate(`/help-requests/${req.id}`)}
            >
              {/* Top row: title + urgency badge */}
              <div className="help-request-card-top">
                <h3 className="help-request-card-title">{req.title}</h3>
                <span className={`badge ${URGENCY_CLASSES[req.urgency] || 'badge-muted'}`}>
                  {URGENCY_LABELS[req.urgency] || req.urgency}
                </span>
              </div>

              {/* Meta row: category + status badges */}
              <div className="help-request-card-meta">
                <span className="badge">{req.category}</span>
                <span className={`badge ${req.status === 'RESOLVED' ? 'badge-resolved' : 'badge-muted'}`}>
                  {STATUS_LABELS[req.status] || req.status}
                </span>
              </div>

              {/* Footer: author, date, comment count */}
              <div className="help-request-card-footer">
                <span className="help-request-card-author">
                  {req.author.full_name}
                </span>
                <span>{formatDate(req.created_at)}</span>
                {req.comment_count > 0 && (
                  <span className="help-request-card-comments">
                    💬 {req.comment_count}
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
