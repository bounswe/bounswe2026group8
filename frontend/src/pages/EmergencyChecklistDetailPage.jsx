import { useNavigate, useParams } from 'react-router-dom';
import { CHECKLIST_TOPICS } from './EmergencyChecklistPage';

export default function EmergencyChecklistDetailPage() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const topic = CHECKLIST_TOPICS.find((t) => t.slug === slug);

  if (!topic) {
    return (
      <div className="page">
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/emergency-info/checklist')}>
          ← Back
        </button>
        <p style={{ color: 'var(--error)', marginTop: '24px' }}>Topic not found.</p>
      </div>
    );
  }

  return (
    <div className="page">
      <header style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/emergency-info/checklist')}>
          ← Back
        </button>
        <span style={{ fontSize: '1.6rem' }}>{topic.icon}</span>
        <h1 style={{ color: topic.color, fontSize: '1.4rem', fontWeight: 700 }}>
          {topic.title}
        </h1>
      </header>

      <div
        style={{
          background: 'var(--bg-card)',
          border: `1px solid ${topic.color}33`,
          borderRadius: 'var(--radius-lg)',
          padding: '28px 24px',
          backdropFilter: 'blur(8px)',
        }}
      >
        {topic.text.split('\n').map((line, i) => {
          if (line.trim() === '') return <br key={i} />;
          const isBullet = line.trim().startsWith('•') || line.trim().startsWith('—');
          const isStep = /^\d+\./.test(line.trim());
          const isHeading = line.trim() === line.trim().toUpperCase() && line.trim().length > 4 && !isBullet && !isStep;

          return (
            <p
              key={i}
              style={{
                color: isHeading ? topic.color : isBullet || isStep ? 'var(--text-primary)' : 'var(--text-secondary)',
                fontWeight: isHeading ? 700 : isBullet || isStep ? 500 : 400,
                fontSize: isHeading ? '0.9rem' : '0.95rem',
                lineHeight: 1.7,
                marginBottom: '2px',
                letterSpacing: isHeading ? '0.05em' : undefined,
                textTransform: isHeading ? 'uppercase' : undefined,
                paddingLeft: isBullet ? '4px' : undefined,
              }}
            >
              {line}
            </p>
          );
        })}
      </div>
    </div>
  );
}
