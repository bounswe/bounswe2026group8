/**
 * HelpRequestDetailPage — shows full request info, location map, and comments.
 *
 * Route: /help-requests/:id
 * Fetches request detail + comments, renders a map via Leaflet when
 * coordinates are present, and lets authenticated users add comments.
 */

import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getHelpRequest, getHelpComments, createHelpComment, updateHelpRequestStatus } from '../services/api';
import { MapContainer, TileLayer, Marker } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';

/* Fix Leaflet's default marker icon path — bundlers break the default URL. */
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

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

export default function HelpRequestDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [helpRequest, setHelpRequest] = useState(null);
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Comment form state
  const [commentText, setCommentText] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [commentError, setCommentError] = useState('');

  // Resolve button state
  const [resolving, setResolving] = useState(false);

  // Fetch request detail and comments in parallel on mount.
  useEffect(() => {
    setLoading(true);
    setError('');
    Promise.all([getHelpRequest(id), getHelpComments(id)])
      .then(([reqRes, commRes]) => {
        if (reqRes.ok) setHelpRequest(reqRes.data);
        else setError(reqRes.data.detail || 'Failed to load request.');
        if (commRes.ok) setComments(commRes.data);
      })
      .catch(() => setError('Network error. Please try again.'))
      .finally(() => setLoading(false));
  }, [id]);

  const handleSubmitComment = async (e) => {
    e.preventDefault();
    if (!commentText.trim()) return;

    setSubmitting(true);
    setCommentError('');

    const { ok, data } = await createHelpComment(id, commentText.trim());
    if (ok) {
      setComments((prev) => [...prev, data]);
      setCommentText('');
      // Refresh the request to get updated comment_count and status.
      const refreshed = await getHelpRequest(id);
      if (refreshed.ok) setHelpRequest(refreshed.data);
    } else {
      setCommentError(data.detail || 'Failed to post comment.');
    }
    setSubmitting(false);
  };

  /** Marks the help request as RESOLVED (author only). */
  const handleResolve = async () => {
    setResolving(true);
    const { ok } = await updateHelpRequestStatus(id, 'RESOLVED');
    if (ok) {
      // Refresh the request to reflect the new status.
      const refreshed = await getHelpRequest(id);
      if (refreshed.ok) setHelpRequest(refreshed.data);
    }
    setResolving(false);
  };

  if (!user) return null;

  if (loading) {
    return (
      <div className="page help-detail-page">
        <div className="help-requests-loading"><p>Loading...</p></div>
      </div>
    );
  }

  if (error || !helpRequest) {
    return (
      <div className="page help-detail-page">
        <div className="alert alert-error">{error || 'Request not found.'}</div>
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/help-requests')}>
          &larr; Back
        </button>
      </div>
    );
  }

  const hasCoords = helpRequest.latitude && helpRequest.longitude;

  return (
    <div className="page help-detail-page">
      {/* Header */}
      <header className="help-requests-header">
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/help-requests')}>
          &larr; Back
        </button>
        <h2 className="gradient-text">Request Detail</h2>
      </header>

      {/* Main info card */}
      <div className="help-detail-card">
        <h1 className="help-detail-title">{helpRequest.title}</h1>

        <div className="help-detail-badges">
          <span className="badge">{helpRequest.category}</span>
          <span className={`badge ${URGENCY_CLASSES[helpRequest.urgency] || 'badge-muted'}`}>
            {URGENCY_LABELS[helpRequest.urgency] || helpRequest.urgency}
          </span>
          <span className={`badge ${
            helpRequest.status === 'RESOLVED' ? 'badge-resolved'
            : helpRequest.status === 'EXPERT_RESPONDING' ? 'badge-expert-responding'
            : 'badge-muted'
          }`}>
            {STATUS_LABELS[helpRequest.status] || helpRequest.status}
          </span>
        </div>

        <p className="help-detail-description">{helpRequest.description}</p>

        <div className="help-detail-meta">
          <span>By <strong>{helpRequest.author.full_name}</strong></span>
          {helpRequest.hub_name && <span>Hub: {helpRequest.hub_name}</span>}
          <span>{formatDate(helpRequest.created_at)}</span>
        </div>

        {/* Resolve button — only visible to the author when not already resolved */}
        {helpRequest.author.id === user.id && helpRequest.status !== 'RESOLVED' && (
          <button
            className="btn btn-primary btn-sm help-detail-resolve-btn"
            onClick={handleResolve}
            disabled={resolving}
          >
            {resolving ? 'Resolving...' : 'Mark as Resolved'}
          </button>
        )}
      </div>

      {/* Location section */}
      {(hasCoords || helpRequest.location_text) && (
        <div className="help-detail-card">
          <h3 className="help-detail-section-title">Location</h3>
          {helpRequest.location_text && (
            <p className="help-detail-location-text">{helpRequest.location_text}</p>
          )}
          {hasCoords && (
            <div className="help-detail-map">
              <MapContainer
                center={[parseFloat(helpRequest.latitude), parseFloat(helpRequest.longitude)]}
                zoom={15}
                style={{ height: '100%', width: '100%', borderRadius: 'var(--radius-sm)' }}
                scrollWheelZoom={false}
              >
                <TileLayer
                  attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                  url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                <Marker position={[parseFloat(helpRequest.latitude), parseFloat(helpRequest.longitude)]} />
              </MapContainer>
            </div>
          )}
        </div>
      )}

      {/* Comments section */}
      <div className="help-detail-card">
        <h3 className="help-detail-section-title">
          Comments ({helpRequest.comment_count})
        </h3>

        {comments.length === 0 && (
          <p className="help-detail-no-comments">No comments yet. Be the first to respond.</p>
        )}

        {comments.length > 0 && (
          <div className="help-detail-comments">
            {comments.map((c) => (
              <div className="help-detail-comment" key={c.id}>
                <div className="help-detail-comment-header">
                  <strong>{c.author.full_name}</strong>
                  {c.author.role === 'EXPERT' && (
                    <span className="badge badge-expert-responding">Expert</span>
                  )}
                  <span className="help-detail-comment-date">{formatDate(c.created_at)}</span>
                </div>
                <p>{c.content}</p>
              </div>
            ))}
          </div>
        )}

        {/* Comment input form */}
        <form className="help-detail-comment-form" onSubmit={handleSubmitComment}>
          {commentError && <div className="alert alert-error">{commentError}</div>}
          <textarea
            className="help-detail-textarea"
            placeholder="Write a comment..."
            value={commentText}
            onChange={(e) => setCommentText(e.target.value)}
            rows={3}
          />
          <button
            className="btn btn-primary btn-sm"
            type="submit"
            disabled={submitting || !commentText.trim()}
          >
            {submitting ? 'Posting...' : 'Post Comment'}
          </button>
        </form>
      </div>
    </div>
  );
}
