import { useEffect, useState, useCallback } from 'react';
import {
  listHelpRequestModeration,
  listHelpOfferModeration,
  moderationDeleteHelpRequest,
  moderationDeleteHelpOffer,
} from '../services/api';
import BackToDashboard from '../components/BackToDashboard';

const TABS = ['requests', 'offers'];

export default function HelpModerationPage() {
  const [tab, setTab] = useState('requests');
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    const fetcher = tab === 'requests'
      ? listHelpRequestModeration
      : listHelpOfferModeration;
    const { ok, data } = await fetcher();
    if (ok) setItems(data);
    else setError(data?.detail || 'Failed to load');
    setLoading(false);
  }, [tab]);

  useEffect(() => { reload(); }, [reload]);

  const handleDelete = async (item) => {
    const reason = window.prompt('Reason for deletion:', '');
    if (!reason || !reason.trim()) {
      if (reason !== null) window.alert('A reason helps the audit log; aborting.');
      return;
    }
    const fn = tab === 'requests' ? moderationDeleteHelpRequest : moderationDeleteHelpOffer;
    const { ok, data } = await fn(item.id, reason.trim());
    if (ok) setItems((rows) => rows.filter((r) => r.id !== item.id));
    else window.alert(data?.detail || 'Delete failed');
  };

  return (
    <div className="page" style={{ padding: '1.5rem', maxWidth: 1100, margin: '0 auto' }}>
      <BackToDashboard />
      <h2 className="gradient-text">Help moderation</h2>
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
        {TABS.map((t) => (
          <button
            key={t}
            className={`btn btn-${t === tab ? 'primary' : 'secondary'} btn-sm`}
            onClick={() => setTab(t)}
          >
            {t === 'requests' ? 'Help requests' : 'Help offers'}
          </button>
        ))}
      </div>

      {loading && <p>Loading…</p>}
      {error && <p style={{ color: '#f87171' }}>{error}</p>}

      {!loading && !error && (
        <ul style={{ listStyle: 'none', padding: 0, display: 'grid', gap: '0.75rem' }}>
          {items.map((item) => (
            <li key={item.id} className="welcome-card" style={{ padding: '1rem' }}>
              <header style={{ display: 'flex', justifyContent: 'space-between' }}>
                <strong>
                  {tab === 'requests' ? item.title : item.skill_or_resource}
                </strong>
                <span className="badge">{item.category}</span>
              </header>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9em' }}>
                by {item.author?.email} · {item.hub_name || 'no hub'}
                {tab === 'requests' && ` · ${item.status}`}
              </p>
              {tab === 'offers' && (
                <p style={{ marginTop: '0.25rem' }}>{item.description}</p>
              )}
              <button
                className="btn btn-secondary btn-sm"
                onClick={() => handleDelete(item)}
                style={{ color: '#f87171' }}
              >
                Delete
              </button>
            </li>
          ))}
          {items.length === 0 && <li>Nothing to moderate.</li>}
        </ul>
      )}
    </div>
  );
}
