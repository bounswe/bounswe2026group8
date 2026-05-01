/**
 * OfflineMessagesPage — read-only archive of mesh posts uploaded by users
 * once they came back online. Tap a post to see its comments + view the
 * shared location (if the author opted in).
 */

import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getMeshPosts } from '../services/api';

const TYPE_BADGE = {
  NEED_HELP: { label: 'Need Help', color: '#f87171' },
  OFFER_HELP: { label: 'Offer Help', color: '#34d399' },
};

function formatTime(epochMillis) {
  if (!epochMillis) return '';
  const date = new Date(epochMillis);
  const diffMs = Date.now() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);
  if (diffMins < 1) return 'just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays < 7) return `${diffDays}d ago`;
  return date.toLocaleDateString();
}

export default function OfflineMessagesPage() {
  const navigate = useNavigate();
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const res = await getMeshPosts();
      if (cancelled) return;
      if (res.ok) {
        setPosts(res.data || []);
      } else {
        setError(`HTTP ${res.status}`);
      }
      setLoading(false);
    })();
    return () => { cancelled = true; };
  }, []);

  return (
    <div className="page">
      <header className="dashboard-header">
        <h2 className="gradient-text">📡 Offline Messages</h2>
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/dashboard')}>
          ← Dashboard
        </button>
      </header>

      <p style={{ color: '#94a3b8', fontSize: '0.9rem', marginBottom: '1rem' }}>
        Posts that users created in the offline mesh and uploaded once they came back online.
      </p>

      {loading && <p style={{ color: '#94a3b8' }}>Loading…</p>}
      {error && <p style={{ color: '#f87171' }}>Couldn't load: {error}</p>}
      {!loading && !error && posts.length === 0 && (
        <p style={{ color: '#64748b' }}>No offline messages have been uploaded yet.</p>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
        {posts.map((post) => {
          const badge = TYPE_BADGE[post.post_type];
          const author = post.author_display_name || `device-${post.author_device_id}`;
          const hasLocation = post.latitude != null && post.longitude != null;
          return (
            <Link
              to={`/offline-messages/${post.id}`}
              key={post.id}
              className="dashboard-card dashboard-card--clickable"
              style={{ textDecoration: 'none', color: 'inherit' }}
            >
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
                    marginBottom: '0.5rem',
                    display: 'inline-block',
                  }}
                >
                  {badge.label}
                </span>
              )}
              <h3 style={{ marginBottom: '0.5rem' }}>{post.title || '(untitled)'}</h3>
              <p style={{ color: '#cbd5e1', fontSize: '0.9rem', marginBottom: '0.5rem' }}>
                {post.body.length > 180 ? `${post.body.slice(0, 180)}…` : post.body}
              </p>
              {hasLocation && (
                <p style={{ color: '#94a3b8', fontSize: '0.8rem', marginBottom: '0.5rem' }}>
                  📍 location attached
                </p>
              )}
              <p style={{ color: '#64748b', fontSize: '0.8rem' }}>
                <strong style={{ color: '#38bdf8' }}>{author}</strong> · {formatTime(post.created_at)}
              </p>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
