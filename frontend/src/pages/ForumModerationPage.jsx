import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  listForumModerationPosts,
  moderateForumPost,
} from '../services/api';
import BackToDashboard from '../components/BackToDashboard';

export default function ForumModerationPage() {
  const navigate = useNavigate();
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    const params = {};
    if (statusFilter) params.status = statusFilter;
    const { ok, data } = await listForumModerationPosts(params);
    if (ok) setPosts(data);
    else setError(data?.detail || 'Failed to load moderation queue');
    setLoading(false);
  }, [statusFilter]);

  useEffect(() => { reload(); }, [reload]);

  const act = async (post, action) => {
    let reason = '';
    if (action === 'HIDE' || action === 'REMOVE') {
      reason = window.prompt(`Reason for ${action}:`, '');
      if (!reason || !reason.trim()) {
        if (reason !== null) window.alert('Reason required for HIDE/REMOVE.');
        return;
      }
      reason = reason.trim();
    }
    const { ok, data } = await moderateForumPost(post.id, action, reason);
    if (ok) {
      reload();
    } else {
      window.alert(data?.detail || 'Action failed');
    }
  };

  const openPost = (post) => {
    navigate(`/forum/posts/${post.id}`);
  };

  const handleCardKeyDown = (event, post) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      openPost(post);
    }
  };

  return (
    <div className="page" style={{ padding: '1.5rem', maxWidth: 1100, margin: '0 auto' }}>
      <BackToDashboard to="/staff" label="← Back to staff dashboard" />
      <h2 className="gradient-text">Forum moderation</h2>
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem', alignItems: 'center' }}>
        <label>Status:</label>
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">Reported or non-active</option>
          <option value="ACTIVE">Active</option>
          <option value="HIDDEN">Hidden</option>
          <option value="REMOVED">Removed</option>
        </select>
        <button className="btn btn-secondary btn-sm" onClick={reload}>Refresh</button>
      </div>

      {loading && <p>Loading…</p>}
      {error && <p style={{ color: '#f87171' }}>{error}</p>}

      {!loading && !error && (
        <ul style={{ listStyle: 'none', padding: 0, display: 'grid', gap: '0.75rem' }}>
          {posts.map((post) => (
            <li
              key={post.id}
              className="welcome-card"
              role="link"
              tabIndex={0}
              onClick={() => openPost(post)}
              onKeyDown={(event) => handleCardKeyDown(event, post)}
              style={{ padding: '1rem', cursor: 'pointer' }}
            >
              <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                <div>
                  <strong>{post.title}</strong>
                  <span style={{ marginLeft: '0.5rem', color: 'var(--text-secondary)' }}>
                    by {post.author?.email}
                  </span>
                </div>
                <span className="badge">{post.status}</span>
              </header>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9em', marginTop: '0.25rem' }}>
                {post.hub_name} · {post.forum_type} · {post.comment_count} comments
              </p>
              <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
                {post.status !== 'HIDDEN' && (
                  <button
                    className="btn btn-secondary btn-sm"
                    onClick={(event) => {
                      event.stopPropagation();
                      act(post, 'HIDE');
                    }}
                  >
                    Hide
                  </button>
                )}
                {post.status !== 'ACTIVE' && (
                  <button
                    className="btn btn-secondary btn-sm"
                    onClick={(event) => {
                      event.stopPropagation();
                      act(post, 'RESTORE');
                    }}
                  >
                    Restore
                  </button>
                )}
                {post.status !== 'REMOVED' && (
                  <button
                    className="btn btn-secondary btn-sm"
                    onClick={(event) => {
                      event.stopPropagation();
                      act(post, 'REMOVE');
                    }}
                    style={{ color: '#f87171' }}
                  >
                    Remove
                  </button>
                )}
              </div>
            </li>
          ))}
          {posts.length === 0 && <li>No posts in moderation queue.</li>}
        </ul>
      )}
    </div>
  );
}
