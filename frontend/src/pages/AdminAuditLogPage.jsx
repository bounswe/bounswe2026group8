import { useEffect, useState, useCallback } from 'react';
import { listAuditLogs } from '../services/api';
import BackToDashboard from '../components/BackToDashboard';

export default function AdminAuditLogPage() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filters, setFilters] = useState({ action: '', target_type: '' });

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    const params = {};
    if (filters.action) params.action = filters.action;
    if (filters.target_type) params.target_type = filters.target_type;
    const { ok, data } = await listAuditLogs(params);
    if (ok) setLogs(data);
    else setError(data?.detail || 'Failed to load audit logs');
    setLoading(false);
  }, [filters.action, filters.target_type]);

  useEffect(() => { reload(); }, [reload]);

  return (
    <div className="page" style={{ padding: '1.5rem', maxWidth: 1100, margin: '0 auto' }}>
      <BackToDashboard />
      <h2 className="gradient-text">Audit log</h2>
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem', flexWrap: 'wrap' }}>
        <input
          placeholder="Filter action (e.g. STAFF_ROLE_CHANGED)"
          value={filters.action}
          onChange={(e) => setFilters((s) => ({ ...s, action: e.target.value }))}
          style={{ flex: 1, minWidth: 220, padding: '0.5rem' }}
        />
        <input
          placeholder="Filter target type (e.g. POST)"
          value={filters.target_type}
          onChange={(e) => setFilters((s) => ({ ...s, target_type: e.target.value }))}
          style={{ flex: 1, minWidth: 180, padding: '0.5rem' }}
        />
        <button className="btn btn-secondary btn-sm" onClick={reload}>Apply</button>
      </div>
      {loading && <p>Loading…</p>}
      {error && <p style={{ color: '#f87171' }}>{error}</p>}
      {!loading && !error && (
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--border-color, #333)' }}>
                <th style={{ padding: '0.5rem' }}>When</th>
                <th style={{ padding: '0.5rem' }}>Actor</th>
                <th style={{ padding: '0.5rem' }}>Action</th>
                <th style={{ padding: '0.5rem' }}>Target</th>
                <th style={{ padding: '0.5rem' }}>Reason</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id} style={{ borderBottom: '1px solid var(--border-color, #222)', verticalAlign: 'top' }}>
                  <td style={{ padding: '0.5rem', whiteSpace: 'nowrap' }}>
                    {new Date(log.created_at).toLocaleString()}
                  </td>
                  <td style={{ padding: '0.5rem' }}>{log.actor_email || '—'}</td>
                  <td style={{ padding: '0.5rem' }}>{log.action}</td>
                  <td style={{ padding: '0.5rem' }}>
                    {log.target_type}
                    {log.target_id ? ` #${log.target_id}` : ''}
                    {log.target_user_email ? ` (${log.target_user_email})` : ''}
                  </td>
                  <td style={{ padding: '0.5rem', color: 'var(--text-secondary)' }}>{log.reason || '—'}</td>
                </tr>
              ))}
              {logs.length === 0 && (
                <tr><td colSpan={5} style={{ padding: '1rem', textAlign: 'center' }}>No audit log entries.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
