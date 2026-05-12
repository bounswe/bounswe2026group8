import { useState } from 'react';
import { useTranslation } from 'react-i18next';

const TOPIC_META = [
  { slug: 'displacement', icon: '🚶', color: '#f59e0b' },
  { slug: 'checking', icon: '🔍', color: '#38bdf8' },
  { slug: 'cpr', icon: '❤️', color: '#f87171' },
  { slug: 'abc', icon: '🫁', color: '#818cf8' },
];

// Pass 't' down so DetailView can grab the correct paragraph text
function DetailView({ topic, t }) {
  // Dynamically fetch the multiline string based on the active topic's slug
  const translatedText = t(`checklist.topics.${topic.slug}.text`);

  return (
      <div className="cl-detail">
        <div
            className="cl-detail-card"
            style={{ borderColor: topic.color + '33' }}
        >
          {translatedText.split('\n').map((line, i) => {
            if (line.trim() === '') return <br key={i} />;
            const isBullet = line.trim().startsWith('•') || line.trim().startsWith('—');
            const isStep = /^\d+\./.test(line.trim());
            const isHeading = line.trim() === line.trim().toUpperCase() && line.trim().length > 4 && !isBullet && !isStep;

            return (
                <p
                    key={i}
                    className={
                      isHeading ? 'cl-line-heading' : isBullet || isStep ? 'cl-line-step' : 'cl-line-body'
                    }
                    style={isHeading ? { color: topic.color } : undefined}
                >
                  {line}
                </p>
            );
          })}
        </div>
      </div>
  );
}

export default function ChecklistModal({ open, onClose }) {
  const [activeTopic, setActiveTopic] = useState(null);
  const { t } = useTranslation(); // 4. Initialize hook

  if (!open) return null;

  const handleClose = () => {
    setActiveTopic(null);
    onClose();
  };

  const handleBack = () => setActiveTopic(null);

  const currentTopic = TOPIC_META.find((t) => t.slug === activeTopic);

  return (
      <div className="cl-overlay" onClick={handleClose}>
        <div className="cl-modal" onClick={(e) => e.stopPropagation()}>
          {/* Header */}
          <div className="cl-modal-header">
            {currentTopic ? (
                <>
                  <button className="btn btn-secondary btn-sm" onClick={handleBack}>
                    ← {t('checklist.header.back')}
                  </button>
                  <span className="cl-modal-icon">{currentTopic.icon}</span>
                  <h2 className="cl-modal-title" style={{ color: currentTopic.color }}>
                    {t(`checklist.topics.${currentTopic.slug}.title`)}
                  </h2>
                </>
            ) : (
                <h2 className="cl-modal-title gradient-text">{t('checklist.header.title')}</h2>
            )}
            <button className="cl-close-btn" onClick={handleClose}>✕</button>
          </div>

          {/* Body */}
          <div className="cl-modal-body">
            {currentTopic ? (
                <DetailView topic={currentTopic} t={t} />
            ) : (
                <>
                  <p className="cl-subtitle">
                    {t('checklist.subtitle')}
                  </p>
                  <div className="cl-grid">
                    {TOPIC_META.map((topic) => (
                        <button
                            key={topic.slug}
                            className="cl-topic-card"
                            style={{ borderColor: topic.color + '33' }}
                            onClick={() => setActiveTopic(topic.slug)}
                            onMouseEnter={(e) => {
                              e.currentTarget.style.borderColor = topic.color + '99';
                              e.currentTarget.style.transform = 'translateY(-2px)';
                            }}
                            onMouseLeave={(e) => {
                              e.currentTarget.style.borderColor = topic.color + '33';
                              e.currentTarget.style.transform = 'translateY(0)';
                            }}
                        >
                          <span className="cl-topic-icon">{topic.icon}</span>
                          <h3 className="cl-topic-title" style={{ color: topic.color }}>
                            {/* Dynamically fetch the title based on the slug */}
                            {t(`checklist.topics.${topic.slug}.title`)}
                          </h3>
                          <p className="cl-topic-summary">
                            {/* Dynamically fetch the summary based on the slug */}
                            {t(`checklist.topics.${topic.slug}.summary`)}
                          </p>
                        </button>
                    ))}
                  </div>
                </>
            )}
          </div>
        </div>
      </div>
  );
}