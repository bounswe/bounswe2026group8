/**
 * OfflineMessageDetailPage — shows one mesh post + its comments. Read-only.
 * If the post or any comment has a shared location, a "View location" button
 * opens the coordinates on Google Maps in a new tab.
 *
 * The post itself isn't fetched as a single object — the list endpoint already
 * returned everything we need, so we read it from navigation state if present
 * and otherwise re-fetch the list and find it. Keeps the API surface small.
 */

import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { getMeshPosts, getMeshComments } from '../services/api';

const TYPE_BADGE_COLORS = {
  NEED_HELP: '#f87171',
  OFFER_HELP: '#34d399',
};

function mapsUrl(lat, lon) {
  return `https://www.google.com/maps?q=${lat},${lon}`;
}

function LocationBlock({ lat, lon, accuracy, capturedAt }) {
  const { t } = useTranslation();
  if (lat == null || lon == null) return null;
  const ageStr = (() => {
    if (!capturedAt) return null;
    const ageSec = Math.floor((Date.now() - capturedAt) / 1000);
    if (ageSec < 60) return t('offline_messages.location.fix_seconds', { count: ageSec });
    if (ageSec < 3600) {
      return t('offline_messages.location.fix_minutes', { count: Math.floor(ageSec / 60) });
    }
    if (ageSec < 86400) {
      return t('offline_messages.location.fix_hours', { count: Math.floor(ageSec / 3600) });
    }
    return t('offline_messages.location.fix_days', { count: Math.floor(ageSec / 86400) });
  })();

  return (
    <div
      style={{
        marginTop: '0.75rem',
        padding: '0.75rem',
        background: '#1e293b',
        border: '1px solid #334155',
        borderRadius: '8px',
        display: 'flex',
        alignItems: 'center',
        gap: '0.75rem',
        flexWrap: 'wrap',
      }}
    >
      <span style={{ color: '#94a3b8', fontSize: '0.85rem' }}>
        📍 {lat.toFixed(5)}, {lon.toFixed(5)}
        {accuracy != null && ` · ±${Math.round(accuracy)}m`}
        {ageStr && ` · ${ageStr}`}
      </span>
      <a
        href={mapsUrl(lat, lon)}
        target="_blank"
        rel="noopener noreferrer"
        className="btn btn-secondary btn-sm"
        style={{ marginLeft: 'auto' }}
      >
        {t('offline_messages.detail.view_location')}
      </a>
    </div>
  );
}

function useLocaleAwareTimeFormatter() {
  return (epochMillis) => {
    if (!epochMillis) return '';
    const date = new Date(epochMillis);
    const sameDay = date.toDateString() === new Date().toDateString();
    if (sameDay) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
    return date.toLocaleString([], {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };
}

export default function OfflineMessageDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const formatTime = useLocaleAwareTimeFormatter();

  const [post, setPost] = useState(null);
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      // Fetch post (via list — server has no single-post GET; cheap enough
      // since the archive isn't enormous and we cache nothing).
      const [postsRes, commentsRes] = await Promise.all([
        getMeshPosts(),
        getMeshComments(id),
      ]);
      if (cancelled) return;
      if (!postsRes.ok) {
        setError(t('offline_messages.detail.error_load', { status: postsRes.status }));
        setLoading(false);
        return;
      }
      const found = (postsRes.data || []).find((p) => p.id === id);
      if (!found) {
        setError(t('offline_messages.detail.error_not_found'));
        setLoading(false);
        return;
      }
      setPost(found);
      setComments(commentsRes.ok ? commentsRes.data || [] : []);
      setLoading(false);
    })();
    return () => { cancelled = true; };
  }, [id, t]);

  if (loading) {
    return (
      <div className="page">
        <p style={{ color: '#94a3b8' }}>{t('offline_messages.detail.loading')}</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page">
        <button
          className="btn btn-secondary btn-sm"
          onClick={() => navigate('/offline-messages')}
        >
          {t('offline_messages.detail.back')}
        </button>
        <p style={{ color: '#f87171', marginTop: '1rem' }}>{error}</p>
      </div>
    );
  }

  const badgeColor = TYPE_BADGE_COLORS[post.post_type];
  const badgeLabel = post.post_type === 'NEED_HELP'
    ? t('offline_messages.post_type.need_help')
    : post.post_type === 'OFFER_HELP'
      ? t('offline_messages.post_type.offer_help')
      : null;
  const author =
    post.author_display_name ||
    t('offline_messages.detail.device_fallback', { id: post.author_device_id });

  return (
    <div className="page">
      <header className="dashboard-header">
        <button
          className="btn btn-secondary btn-sm"
          onClick={() => navigate('/offline-messages')}
        >
          {t('offline_messages.detail.back')}
        </button>
      </header>

      <div className="welcome-card" style={{ marginBottom: '1.5rem' }}>
        {badgeColor && badgeLabel && (
          <span
            className="badge"
            style={{
              color: badgeColor,
              borderColor: badgeColor + '44',
              background: badgeColor + '11',
              textTransform: 'uppercase',
              fontSize: '0.7rem',
              fontWeight: 'bold',
              marginBottom: '0.75rem',
              display: 'inline-block',
            }}
          >
            {badgeLabel}
          </span>
        )}
        <h1 style={{ marginBottom: '0.5rem' }}>
          {post.title || t('offline_messages.detail.untitled')}
        </h1>
        <p style={{ color: '#94a3b8', fontSize: '0.85rem', marginBottom: '1rem' }}>
          <strong style={{ color: '#38bdf8' }}>{author}</strong> · {formatTime(post.created_at)}
        </p>
        <p style={{ color: '#e2e8f0', lineHeight: 1.5, whiteSpace: 'pre-wrap' }}>
          {post.body}
        </p>
        <LocationBlock
          lat={post.latitude}
          lon={post.longitude}
          accuracy={post.loc_accuracy_meters}
          capturedAt={post.loc_captured_at}
        />
      </div>

      <h3 style={{ marginBottom: '0.75rem' }}>
        {t('offline_messages.detail.comments_heading', { count: comments.length })}
      </h3>
      {comments.length === 0 ? (
        <p style={{ color: '#64748b' }}>{t('offline_messages.detail.no_comments')}</p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
          {comments.map((c) => {
            const cAuthor =
              c.author_display_name ||
              t('offline_messages.detail.device_fallback', { id: c.author_device_id });
            return (
              <div
                key={c.id}
                style={{
                  padding: '0.75rem 1rem',
                  background: '#111827',
                  border: '1px solid #334155',
                  borderRadius: '8px',
                }}
              >
                <p style={{ color: '#94a3b8', fontSize: '0.8rem', marginBottom: '0.25rem' }}>
                  <strong style={{ color: '#38bdf8' }}>{cAuthor}</strong> · {formatTime(c.created_at)}
                </p>
                <p style={{ color: '#e2e8f0', whiteSpace: 'pre-wrap' }}>{c.body}</p>
                <LocationBlock
                  lat={c.latitude}
                  lon={c.longitude}
                  accuracy={c.loc_accuracy_meters}
                  capturedAt={c.loc_captured_at}
                />
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
