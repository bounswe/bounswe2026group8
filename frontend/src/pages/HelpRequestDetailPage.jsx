/**
 * HelpRequestDetailPage — shows full request info, location map, and comments.
 *
 * Route: /help-requests/:id
 * Fetches request detail + comments, renders a map via Leaflet when
 * coordinates are present, and lets authenticated users add comments.
 */

import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getHelpRequest, getHelpComments, createHelpComment, updateHelpRequestStatus, deleteHelpRequest, deleteHelpComment, resolveImageUrl, takeOnHelpRequest, releaseHelpRequest } from '../services/api';
import { MapContainer, TileLayer, Marker } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { useTranslation } from 'react-i18next';

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

// Pass 't' to the date formatter
function formatDate(isoString, t) {
  const date = new Date(isoString);
  const diffMs = Date.now() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);
  if (diffMins < 1) return t('help_request_detail.time.just_now');
  if (diffMins < 60) return t('help_request_detail.time.m_ago', { count: diffMins });
  if (diffHours < 24) return t('help_request_detail.time.h_ago', { count: diffHours });
  if (diffDays < 7) return t('help_request_detail.time.d_ago', { count: diffDays });
  return date.toLocaleDateString();
}

const AVAIL_COLORS = { SAFE: '#34d399', NEEDS_HELP: '#f87171', AVAILABLE_TO_HELP: '#38bdf8' };

// Pass 't' down to the status component
function AuthorStatus({ profile, t }) {
  const s = profile?.availability_status;
  if (!s || !AVAIL_COLORS[s]) return null;
  const c = AVAIL_COLORS[s];

  const AVAIL_LABELS = {
    SAFE: t('help_request_detail.availability.safe'),
    NEEDS_HELP: t('help_request_detail.availability.needs_help'),
    AVAILABLE_TO_HELP: t('help_request_detail.availability.available')
  };

  return <span className="badge" style={{ color: c, borderColor: c + '44', background: c + '11', fontSize: '0.7rem', padding: '1px 6px' }}>● {AVAIL_LABELS[s]}</span>;
}

export default function HelpRequestDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const { t } = useTranslation(); // Initialize hook

  // Move label objects inside the component to map with 't' dynamically
  const URGENCY_LABELS = {
    LOW: t('help_request_detail.urgency.low'),
    MEDIUM: t('help_request_detail.urgency.medium'),
    HIGH: t('help_request_detail.urgency.high')
  };

  const STATUS_LABELS = {
    OPEN: t('help_request_detail.status.open'),
    EXPERT_RESPONDING: t('help_request_detail.status.expert_responding'),
    RESOLVED: t('help_request_detail.status.resolved'),
  };

  const [helpRequest, setHelpRequest] = useState(null);
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [commentText, setCommentText] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [commentError, setCommentError] = useState('');

  const [resolving, setResolving] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  const [takingOn, setTakingOn] = useState(false);
  const [releasing, setReleasing] = useState(false);

  useEffect(() => {
    setLoading(true);
    setError('');
    Promise.all([getHelpRequest(id), getHelpComments(id)])
        .then(([reqRes, commRes]) => {
          if (reqRes.ok) setHelpRequest(reqRes.data);
          else setError(reqRes.data.detail || t('help_request_detail.states.fetch_failed'));
          if (commRes.ok) setComments(commRes.data);
        })
        .catch(() => setError(t('help_request_detail.states.network_error')))
        .finally(() => setLoading(false));
  }, [id, t]);

  const handleSubmitComment = async (e) => {
    e.preventDefault();
    if (!commentText.trim()) return;

    setSubmitting(true);
    setCommentError('');

    const { ok, data } = await createHelpComment(id, commentText.trim());
    if (ok) {
      setComments((prev) => [...prev, data]);
      setCommentText('');
      const refreshed = await getHelpRequest(id);
      if (refreshed.ok) setHelpRequest(refreshed.data);
    } else {
      setCommentError(data.detail || t('help_request_detail.states.post_failed'));
    }
    setSubmitting(false);
  };

  const handleDeleteRequest = async () => {
    setShowDeleteConfirm(false);
    const { ok } = await deleteHelpRequest(id);
    if (ok) navigate(-1);
  };

  const handleDeleteComment = async (commentId) => {
    const { ok } = await deleteHelpComment(commentId);
    if (ok) {
      setComments((prev) => prev.filter((c) => c.id !== commentId));
      setHelpRequest((r) => r && { ...r, comment_count: Math.max(0, r.comment_count - 1) });
    }
  };

  const handleResolve = async () => {
    setResolving(true);
    const { ok } = await updateHelpRequestStatus(id, 'RESOLVED');
    if (ok) {
      const refreshed = await getHelpRequest(id);
      if (refreshed.ok) setHelpRequest(refreshed.data);
    }
    setResolving(false);
  };

  const handleTakeOn = async () => {
    setTakingOn(true);
    setError('');
    const { ok, data } = await takeOnHelpRequest(id);
    if (ok) {
      const refreshed = await getHelpRequest(id);
      if (refreshed.ok) setHelpRequest(refreshed.data);
    } else {
      setError(data?.detail || 'Failed to take on request. You might not have the required expertise.');
    }
    setTakingOn(false);
  };

  const handleRelease = async () => {
    setReleasing(true);
    setError('');
    const { ok, data } = await releaseHelpRequest(id);
    if (ok) {
      const refreshed = await getHelpRequest(id);
      if (refreshed.ok) setHelpRequest(refreshed.data);
    } else {
      setError(data?.detail || 'Failed to release request.');
    }
    setReleasing(false);
  };

  if (!user) return null;

  if (loading) {
    return (
        <div className="page help-detail-page">
          <div className="help-requests-loading"><p>{t('help_request_detail.states.loading')}</p></div>
        </div>
    );
  }

  if (error || !helpRequest) {
    return (
        <div className="page help-detail-page">
          <div className="alert alert-error">{error || t('help_request_detail.states.not_found')}</div>
          <button className="btn btn-secondary btn-sm" onClick={() => navigate(-1)}>
            &larr; {t('help_request_detail.header.back')}
          </button>
        </div>
    );
  }

  const hasCoords = helpRequest.latitude && helpRequest.longitude;

  return (
      <div className="page help-detail-page">
        <header className="help-requests-header">
          <button className="btn btn-secondary btn-sm" onClick={() => navigate(-1)}>
            &larr; {t('help_request_detail.header.back')}
          </button>
          <h2 className="gradient-text">{t('help_request_detail.header.title')}</h2>
        </header>

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

          {helpRequest.image_urls && helpRequest.image_urls.length > 0 && (
              <div className="post-images">
                {helpRequest.image_urls.map((url, i) => (
                    <img
                        key={i}
                        src={resolveImageUrl(url)}
                        alt={`Attachment ${i + 1}`}
                        className="post-image"
                    />
                ))}
              </div>
          )}

          <div className="help-detail-meta">
            <span>{t('help_request_detail.labels.by')}<Link to={`/users/${helpRequest.author.id}`} className="author-link"><strong>{helpRequest.author.full_name}</strong></Link></span>
            {helpRequest.author.role === 'EXPERT' && <span className="badge badge-expert-responding">{t('help_request_detail.labels.expert')}</span>}
            <AuthorStatus profile={helpRequest.author.profile} t={t} />
            {helpRequest.hub_name && <span>{t('help_request_detail.labels.hub')}{helpRequest.hub_name}</span>}
            <span>{formatDate(helpRequest.created_at, t)}</span>
          </div>

          {helpRequest.is_expert_responding && helpRequest.assigned_expert && (
              <div className="help-detail-assigned">
                <span className="badge badge-expert-responding">
                  Assigned to: <Link to={`/users/${helpRequest.assigned_expert.id}`} style={{ color: 'inherit', textDecoration: 'underline' }}>{helpRequest.assigned_expert_username}</Link>
                </span>
              </div>
          )}

          {helpRequest.author.id === user.id && (
              <div className="post-owner-actions">
                {helpRequest.status !== 'RESOLVED' && (
                    <button
                        className="btn btn-primary btn-sm help-detail-resolve-btn"
                        onClick={handleResolve}
                        disabled={resolving}
                    >
                      {resolving ? t('help_request_detail.actions.resolving') : t('help_request_detail.actions.mark_resolved')}
                    </button>
                )}
                <button
                    className="btn btn-danger btn-sm"
                    onClick={() => setShowDeleteConfirm(true)}
                >
                  {t('help_request_detail.actions.delete')}
                </button>
              </div>
          )}

          {user.role === 'EXPERT' && user.id !== helpRequest.author.id && (
              <div className="post-owner-actions" style={{ marginTop: '10px' }}>
                {!helpRequest.is_expert_responding && helpRequest.status !== 'RESOLVED' && (
                    <button
                        className="btn btn-primary btn-sm"
                        onClick={handleTakeOn}
                        disabled={takingOn}
                    >
                      {takingOn ? 'Taking On...' : 'Take On Request'}
                    </button>
                )}
                {helpRequest.is_expert_responding && helpRequest.assigned_expert?.id === user.id && helpRequest.status !== 'RESOLVED' && (
                    <button
                        className="btn btn-secondary btn-sm"
                        onClick={handleRelease}
                        disabled={releasing}
                    >
                      {releasing ? 'Releasing...' : 'Release Request'}
                    </button>
                )}
              </div>
          )}
        </div>

        {showDeleteConfirm && (
            <div className="modal-overlay" onClick={() => setShowDeleteConfirm(false)}>
              <div className="modal-card" onClick={(e) => e.stopPropagation()}>
                <h3>{t('help_request_detail.modal.title')}</h3>
                <p className="modal-body-text">{t('help_request_detail.modal.desc')}</p>
                <div className="modal-actions">
                  <button className="btn btn-danger btn-sm" onClick={handleDeleteRequest}>{t('help_request_detail.actions.delete')}</button>
                  <button className="btn btn-secondary btn-sm" onClick={() => setShowDeleteConfirm(false)}>{t('help_request_detail.actions.cancel')}</button>
                </div>
              </div>
            </div>
        )}

        {(hasCoords || helpRequest.location_text) && (
            <div className="help-detail-card">
              <h3 className="help-detail-section-title">{t('help_request_detail.location.title')}</h3>
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

        <div className="help-detail-card">
          <h3 className="help-detail-section-title">
            {t('help_request_detail.comments.title', { count: helpRequest.comment_count })}
          </h3>

          {comments.length === 0 && (
              <p className="help-detail-no-comments">{t('help_request_detail.comments.empty')}</p>
          )}

          {comments.length > 0 && (
              <div className="help-detail-comments">
                {comments.map((c) => (
                    <div className="help-detail-comment" key={c.id}>
                      <div className="help-detail-comment-header">
                        <Link to={`/users/${c.author.id}`} className="author-link"><strong>{c.author.full_name}</strong></Link>
                        {c.author.role === 'EXPERT' && (
                            <span className="badge badge-expert-responding">{t('help_request_detail.labels.expert')}</span>
                        )}
                        <div className="comment-right-group">
                          <span className="help-detail-comment-date">{formatDate(c.created_at, t)}</span>
                          {user.id === c.author.id && (
                              <button
                                  className="comment-delete"
                                  onClick={() => handleDeleteComment(c.id)}
                                  title={t('help_request_detail.comments.delete_title')}
                              >
                                &times;
                              </button>
                          )}
                        </div>
                      </div>
                      <p>{c.content}</p>
                    </div>
                ))}
              </div>
          )}

          <form className="help-detail-comment-form" onSubmit={handleSubmitComment}>
            {commentError && <div className="alert alert-error">{commentError}</div>}
            <textarea
                className="help-detail-textarea"
                placeholder={t('help_request_detail.comments.placeholder')}
                value={commentText}
                onChange={(e) => setCommentText(e.target.value)}
                rows={3}
            />
            <button
                className="btn btn-primary btn-sm"
                type="submit"
                disabled={submitting || !commentText.trim()}
            >
              {submitting ? t('help_request_detail.comments.posting') : t('help_request_detail.comments.post')}
            </button>
          </form>
        </div>
      </div>
  );
}