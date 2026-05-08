import { useEffect, useState, useCallback } from 'react';
import {
  createStaffHub,
  deleteStaffHub,
  listStaffHubs,
  updateStaffHub,
} from '../services/api';
import BackToDashboard from '../components/BackToDashboard';

export default function AdminHubsPage() {
  const [hubs, setHubs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [newHub, setNewHub] = useState({ name: '', slug: '' });

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    const { ok, data } = await listStaffHubs();
    if (ok) setHubs(data);
    else setError(data?.detail || 'Failed to load hubs');
    setLoading(false);
  }, []);

  useEffect(() => { reload(); }, [reload]);

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!newHub.name.trim()) return;
    const payload = { name: newHub.name.trim() };
    if (newHub.slug.trim()) payload.slug = newHub.slug.trim();
    const { ok, data } = await createStaffHub(payload);
    if (ok) {
      setNewHub({ name: '', slug: '' });
      setHubs((rows) => [...rows, data]);
    } else {
      window.alert(data?.detail || JSON.stringify(data));
    }
  };

  const handleRename = async (hub) => {
    const nextName = window.prompt('New name', hub.name);
    if (!nextName || nextName === hub.name) return;
    const { ok, data } = await updateStaffHub(hub.id, { name: nextName });
    if (ok) setHubs((rows) => rows.map((h) => (h.id === hub.id ? data : h)));
    else window.alert(data?.detail || 'Update failed');
  };

  const handleDelete = async (hub) => {
    const ok1 = window.confirm(
      `Delete hub "${hub.name}"? This will cascade-delete its posts, help requests, and help offers.`,
    );
    if (!ok1) return;
    const { ok, data } = await deleteStaffHub(hub.id, { confirm: true });
    if (ok) setHubs((rows) => rows.filter((h) => h.id !== hub.id));
    else window.alert(data?.detail || 'Delete failed');
  };

  return (
    <div className="page" style={{ padding: '1.5rem', maxWidth: 900, margin: '0 auto' }}>
      <BackToDashboard to="/staff" label="← Back to staff dashboard" />
      <h2 className="gradient-text">Hub management</h2>

      <form onSubmit={handleCreate} style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem', flexWrap: 'wrap' }}>
        <input
          required
          placeholder="Name"
          value={newHub.name}
          onChange={(e) => setNewHub((s) => ({ ...s, name: e.target.value }))}
          style={{ flex: 1, minWidth: 180, padding: '0.5rem' }}
        />
        <input
          placeholder="Slug (optional)"
          value={newHub.slug}
          onChange={(e) => setNewHub((s) => ({ ...s, slug: e.target.value }))}
          style={{ minWidth: 180, padding: '0.5rem' }}
        />
        <button type="submit" className="btn btn-primary btn-sm">Create hub</button>
      </form>

      {loading && <p>Loading…</p>}
      {error && <p style={{ color: '#f87171' }}>{error}</p>}

      {!loading && !error && (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ textAlign: 'left', borderBottom: '1px solid var(--border-color, #333)' }}>
              <th style={{ padding: '0.5rem' }}>Name</th>
              <th style={{ padding: '0.5rem' }}>Slug</th>
              <th style={{ padding: '0.5rem' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {hubs.map((h) => (
              <tr key={h.id} style={{ borderBottom: '1px solid var(--border-color, #222)' }}>
                <td style={{ padding: '0.5rem' }}>{h.name}</td>
                <td style={{ padding: '0.5rem' }}>{h.slug}</td>
                <td style={{ padding: '0.5rem', display: 'flex', gap: '0.5rem' }}>
                  <button className="btn btn-secondary btn-sm" onClick={() => handleRename(h)}>Rename</button>
                  <button className="btn btn-secondary btn-sm" onClick={() => handleDelete(h)} style={{ color: '#f87171' }}>Delete</button>
                </td>
              </tr>
            ))}
            {hubs.length === 0 && (
              <tr><td colSpan={3} style={{ padding: '1rem', textAlign: 'center' }}>No hubs.</td></tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  );
}
