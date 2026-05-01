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
import { getMeshPosts, getMeshComments } from '../services/api';

const TYPE_BADGE = {
  NEED_HELP: { label: 'Need Help', color: '#f87171' },
  OFFER_HELP: { label: 'Offer Help', color: '#34d399' },
};

function formatTime(epochMillis) {
  if (!epochMillis) return '';
  const date = new Date(epochMillis);
  const sameDay =
    date.toDateString() === new Date().toDateString();
  if (sameDay) return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  return date.toLocaleString([], {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

function mapsUrl(lat, lon) {
  return `https://www.google.com/maps?q=${lat},${lon}`;
}

function LocationBlock({ lat, lon, accuracy, capturedAt }) {
  if (lat == null || lon == null) return null;
  const ageStr = (() => {
    if (!capturedAt) return null;
    const ageSec = Math.floor((Date.now() - capturedAt) / 1000);
    if (ageSec < 60) return `${ageSec}s old`;
    if (ageSec < 3600) return `${Math.floor(ageSec / 60)}m old`;
    if (ageSec < 86400) return `${Math.floor(ageSec / 3600)}h old`;
    return `${Math.floor(ageSec / 86400)}d old`;
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
        {ageStr && ` · fix ${ageStr}`}
      </span>
      <a
        href={mapsUrl(lat, lon)}
        target="_blank"
        rel="noopener noreferrer"
        className="btn btn-secondary btn-sm"
        style={{ marginLeft: 'auto' }}
      >
        View location
      </a>
    </div>
  );
}

export default function OfflineMessageDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

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
        setError(`Couldn't load post (HTTP ${postsRes.status})`);
        setLoading(false);
        return;
      }
      const found = (postsRes.data || []).find((p) => p.id === id);
      if (!found) {
        setError('Post not found.');
        setLoading(false);
        return;
      }
      setPost(found);
      setComments(commentsRes.ok ? commentsRes.data || [] : []);
      setLoading(false);
    })();
    return () => { cancelled = true; };
  }, [id]);

  if (loading) {
    return (
      <div className="page">
        <p style={{ color: '#94a3b8' }}>Loading…</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page">
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/offline-messages')}>
          ← Back
        </button>
        <p style={{ color: '#f87171', marginTop: '1rem' }}>{error}</p>
      </div>
    );
  }

  const badge = TYPE_BADGE[post.post_type];
  const author = post.author_display_name || `device-${post.author_device_id}`;

  return (
    <div className="page">
      <header className="dashboard-header">
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/offline-messages')}>
          ← Back
        </button>
      </header>

      <div className="welcome-card" style={{ marginBottom: '1.5rem' }}>
        {badge && (
          <span
            className="badge"
            style={{
              color: badge.color,
              borderColor: badge.color + '44',
              background: badge.color + '11',
              textTransform: 'uppercase',
              fontSize: '0.7rem',
              fontWeight: 'bold',
              marginBottom: '0.75rem',
              display: 'inline-block',
            }}
          >
            {badge.label}
          </span>
        )}
        <h1 style={{ marginBottom: '0.5rem' }}>{post.title || '(untitled)'}</h1>
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

      <h3 style={{ marginBottom: '0.75rem' }}>Comments ({comments.length})</h3>
      {comments.length === 0 ? (
        <p style={{ color: '#64748b' }}>No comments yet.</p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
          {comments.map((c) => {
            const cAuthor = c.author_display_name || `device-${c.author_device_id}`;
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
