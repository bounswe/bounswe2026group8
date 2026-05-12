/**
 * OfflineMessagesPage — read-only archive of mesh posts uploaded by users
 * once they came back online. Tap a post to see its comments + view the
 * shared location (if the author opted in).
 */

import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { getMeshPosts } from '../services/api';

const TYPE_BADGE_COLORS = {
  NEED_HELP: '#f87171',
  OFFER_HELP: '#34d399',
};

function useFormatTime() {
  const { t } = useTranslation();
  return (epochMillis) => {
    if (!epochMillis) return '';
    const date = new Date(epochMillis);
    const diffMs = Date.now() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);
    if (diffMins < 1) return t('offline_messages.time.just_now');
    if (diffMins < 60) return t('offline_messages.time.m_ago', { count: diffMins });
    if (diffHours < 24) return t('offline_messages.time.h_ago', { count: diffHours });
    if (diffDays < 7) return t('offline_messages.time.d_ago', { count: diffDays });
    return date.toLocaleDateString();
  };
}

export default function OfflineMessagesPage() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const formatTime = useFormatTime();
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

  const badgeFor = (postType) => {
    const color = TYPE_BADGE_COLORS[postType];
    if (!color) return null;
    const labelKey =
      postType === 'NEED_HELP'
        ? 'offline_messages.post_type.need_help'
        : 'offline_messages.post_type.offer_help';
    return { color, label: t(labelKey) };
  };

  return (
    <div className="page">
      <header className="dashboard-header page-main-header">
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/dashboard')}>
          &larr; {t('offline_messages.list.back')}
        </button>
        <h2 className="gradient-text">📡 {t('offline_messages.list.title')}</h2>
      </header>

      <p style={{ color: '#94a3b8', fontSize: '0.9rem', marginBottom: '1rem' }}>
        {t('offline_messages.list.subtitle')}
      </p>

      {loading && <p style={{ color: '#94a3b8' }}>{t('offline_messages.list.loading')}</p>}
      {error && (
        <p style={{ color: '#f87171' }}>
          {t('offline_messages.list.error', { error })}
        </p>
      )}
      {!loading && !error && posts.length === 0 && (
        <p style={{ color: '#64748b' }}>{t('offline_messages.list.empty')}</p>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
        {posts.map((post) => {
          const badge = badgeFor(post.post_type);
          const author =
            post.author_display_name ||
            t('offline_messages.list.device_fallback', { id: post.author_device_id });
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
              <h3 style={{ marginBottom: '0.5rem' }}>
                {post.title || t('offline_messages.list.untitled')}
              </h3>
              <p style={{ color: '#cbd5e1', fontSize: '0.9rem', marginBottom: '0.5rem' }}>
                {post.body.length > 180 ? `${post.body.slice(0, 180)}…` : post.body}
              </p>
              {hasLocation && (
                <p style={{ color: '#94a3b8', fontSize: '0.8rem', marginBottom: '0.5rem' }}>
                  {t('offline_messages.list.location_attached')}
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
