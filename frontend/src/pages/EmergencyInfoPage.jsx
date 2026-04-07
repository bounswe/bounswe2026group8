import { useNavigate } from 'react-router-dom';

const SECTIONS = [
  {
    icon: '',
    title: 'Emergency Checklist',
    desc: 'First aid guides: displacement, checking vitals, CPR, and ABC protocol',
    path: '/emergency-info/checklist',
    color: '#34d399',
  },
  {
    icon: '🗺️',
    title: 'Map',
    desc: 'View nearby help resources and danger zones on the map',
    path: '/emergency-info/map',
    color: '#38bdf8',
  },
];

export default function EmergencyInfoPage() {
  const navigate = useNavigate();

  return (
    <div className="page">
      <header style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
        <button
          className="btn btn-secondary btn-sm"
          onClick={() => navigate(-1)}
          style={{ flexShrink: 0 }}
        >
          ← Back
        </button>
        <div>
          <h1 className="gradient-text" style={{ fontSize: '1.6rem', lineHeight: 1.2 }}>
            Emergency Info
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '2px' }}>
            Critical information available offline
          </p>
        </div>
      </header>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
          gap: '16px',
          marginTop: '24px',
        }}
      >
        {SECTIONS.map((s) => (
          <button
            key={s.path}
            onClick={() => navigate(s.path)}
            style={{
              background: 'var(--bg-card)',
              border: `1px solid ${s.color}33`,
              borderRadius: 'var(--radius-lg)',
              padding: '28px 24px',
              cursor: 'pointer',
              textAlign: 'left',
              transition: 'border-color 0.2s, transform 0.15s',
              backdropFilter: 'blur(8px)',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.borderColor = s.color + '99';
              e.currentTarget.style.transform = 'translateY(-2px)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.borderColor = s.color + '33';
              e.currentTarget.style.transform = 'translateY(0)';
            }}
          >
            <span style={{ fontSize: '2.5rem', display: 'block', marginBottom: '12px' }}>
              {s.icon}
            </span>
            <h2
              style={{
                color: s.color,
                fontSize: '1.2rem',
                fontWeight: 700,
                marginBottom: '8px',
              }}
            >
              {s.title}
            </h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', lineHeight: 1.5 }}>
              {s.desc}
            </p>
          </button>
        ))}
      </div>
    </div>
  );
}
