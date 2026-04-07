import { useNavigate } from 'react-router-dom';

export default function EmergencyMapPage() {
  const navigate = useNavigate();

  return (
    <div className="page">
      <header style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/emergency-info')}>
          ← Back
        </button>
        <h1 className="gradient-text" style={{ fontSize: '1.5rem' }}>
          Map
        </h1>
      </header>

      <div
        style={{
          background: 'var(--bg-card)',
          border: '1px solid var(--border)',
          borderRadius: 'var(--radius-lg)',
          padding: '64px 24px',
          textAlign: 'center',
          backdropFilter: 'blur(8px)',
        }}
      >
        <span style={{ fontSize: '3rem', display: 'block', marginBottom: '16px' }}>🗺️</span>
        <h2 style={{ color: 'var(--text-primary)', marginBottom: '8px' }}>Coming Soon</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
          The map feature is currently under development.
        </p>
      </div>
    </div>
  );
}
