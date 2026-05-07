import { useEffect, useState, useCallback } from 'react';
import {
  decideExpertiseVerification,
  listExpertiseVerifications,
} from '../services/api';
import BackToDashboard from '../components/BackToDashboard';

const STATUSES = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'ALL', label: 'All' },
];

export default function ExpertiseVerificationPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState('PENDING');

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    const { ok, data } = await listExpertiseVerifications({ status: statusFilter });
    if (ok) setItems(data);
    else setError(data?.detail || 'Failed to load');
    setLoading(false);
  }, [statusFilter]);

  useEffect(() => { reload(); }, [reload]);

  const decide = async (item, decision) => {
    let note = '';
    if (decision === 'REJECTED') {
      note = window.prompt('Rejection note (required):', '');
      if (!note || !note.trim()) {
        if (note !== null) window.alert('A note is required when rejecting.');
        return;
      }
      note = note.trim();
    } else if (decision === 'APPROVED') {
      note = window.prompt('Optional note for approval:', '') || '';
    } else if (decision === 'PENDING') {
      note = window.prompt('Optional note for reopening:', '') || '';
    }
    const { ok, data } = await decideExpertiseVerification(item.id, decision, note);
    if (ok) reload();
    else window.alert(data?.detail || JSON.stringify(data));
  };

  return (
    <div className="page" style={{ padding: '1.5rem', maxWidth: 1100, margin: '0 auto' }}>
      <BackToDashboard to="/staff" label="← Back to staff dashboard" />
      <h2 className="gradient-text">Expertise verification</h2>

      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem', alignItems: 'center' }}>
        <label>Status:</label>
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          {STATUSES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
        </select>
        <button className="btn btn-secondary btn-sm" onClick={reload}>Refresh</button>
      </div>

      {loading && <p>Loading…</p>}
      {error && <p style={{ color: '#f87171' }}>{error}</p>}

      {!loading && !error && (
        <ul style={{ listStyle: 'none', padding: 0, display: 'grid', gap: '0.75rem' }}>
          {items.map((item) => (
            <li key={item.id} className="welcome-card" style={{ padding: '1rem' }}>
              <header style={{ display: 'flex', justifyContent: 'space-between' }}>
                <strong>{item.field}</strong>
                <span className="badge">{item.verification_status}</span>
              </header>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9em' }}>
                {item.user?.email} · {item.certification_level}
                {item.reviewed_by_name && ` · last reviewed by ${item.reviewed_by_name}`}
              </p>
              {item.certification_document_url && (
                <p>
                  <a href={item.certification_document_url} target="_blank" rel="noopener noreferrer">
                    View certification document
                  </a>
                </p>
              )}
              {item.verification_note && (
                <p style={{ color: 'var(--text-secondary)' }}>Note: {item.verification_note}</p>
              )}
              <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
                {item.verification_status !== 'APPROVED' && (
                  <button className="btn btn-secondary btn-sm" onClick={() => decide(item, 'APPROVED')}>Approve</button>
                )}
                {item.verification_status !== 'REJECTED' && (
                  <button className="btn btn-secondary btn-sm" onClick={() => decide(item, 'REJECTED')} style={{ color: '#f87171' }}>Reject</button>
                )}
                {item.verification_status !== 'PENDING' && (
                  <button className="btn btn-secondary btn-sm" onClick={() => decide(item, 'PENDING')}>Reopen</button>
                )}
              </div>
            </li>
          ))}
          {items.length === 0 && <li>Nothing to review.</li>}
        </ul>
      )}
    </div>
  );
}
