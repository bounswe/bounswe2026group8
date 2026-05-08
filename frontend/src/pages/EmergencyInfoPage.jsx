import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ChecklistModal from './EmergencyChecklistPage';
import { useTranslation } from 'react-i18next';
import useTutorialGuide from '../components/TutorialGuide';

const SECTIONS_META = [
    {
        id: 'checklist',
        icon: '📋',
        color: '#34d399',
    },
    {
        id: 'map',
        icon: '🗺️',
        path: '/emergency-info/map',
        color: '#38bdf8',
    },
];

export default function EmergencyInfoPage({ tutorialMode = false }) {
    const navigate = useNavigate();
    const [checklistOpen, setChecklistOpen] = useState(false);
    const { t } = useTranslation(); // Initialize hook
    const EMERGENCY_INFO_TOUR_STEPS = [
        { target: 'overview', title: t('tutorial.emergencyInfo.steps.overviewTitle'), text: t('tutorial.emergencyInfo.steps.overviewText') },
        { target: 'checklist', title: t('tutorial.emergencyInfo.steps.checklistTitle'), text: t('tutorial.emergencyInfo.steps.checklistText') },
        { target: 'map', title: t('tutorial.emergencyInfo.steps.mapTitle'), text: t('tutorial.emergencyInfo.steps.mapText') },
    ];
    const visibleSections = tutorialMode
        ? SECTIONS_META.filter((s) => s.id !== 'map')
        : SECTIONS_META;
    const { activeStep, GuidePanel, RestartButton } = useTutorialGuide({
        storageKey: 'emergencyHubEmergencyInfoTutorialSeen',
        steps: EMERGENCY_INFO_TOUR_STEPS,
    });

    const handleCardClick = (s) => {
        if (s.id === 'checklist') {
            setChecklistOpen(true);
        } else if (s.path) {
            navigate(s.path);
        }
    };

    return (
        <div className={`page emergency-info-page ${tutorialMode ? 'tutorial-page' : ''}`}>
            <header className="dashboard-header page-main-header">
                <button
                    className="btn btn-secondary btn-sm"
                    onClick={() => navigate(tutorialMode ? '/tutorial' : '/dashboard')}
                >
                    &larr; {t('emergency_info.header.back')}
                </button>
                <div className="page-title-block">
                    <h2 className="gradient-text">
                        {t('emergency_info.header.title')}
                    </h2>
                    <p>
                        {t('emergency_info.header.subtitle')}
                    </p>
                </div>
                {tutorialMode && (
                    <div className="tutorial-header-actions">
                        {RestartButton}
                    </div>
                )}
            </header>

            {tutorialMode && GuidePanel}

            {tutorialMode && (
                <div className={`tutorial-scenario-strip ${activeStep?.target === 'overview' ? 'tutorial-tour-highlight' : ''}`}>
                    <div>
                        <strong>{t('tutorial.common.currentSituation')}</strong>
                        <span>{t('tutorial.emergencyInfo.situationText')}</span>
                    </div>
                    <div>
                        <strong>{t('tutorial.common.nextAction')}</strong>
                        <span>{t('tutorial.emergencyInfo.actionText')}</span>
                    </div>
                </div>
            )}

            <div
                style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
                    gap: '16px',
                    marginTop: '24px',
                }}
            >
                {visibleSections.map((s) => (
                    <button
                        key={s.id}
                        className={tutorialMode && activeStep?.target === s.id ? 'tutorial-tour-highlight' : ''}
                        onClick={() => handleCardClick(s)}
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
                            {/* 4. Dynamically fetch the title based on the section id */}
                            {t(`emergency_info.sections.${s.id}.title`)}
                        </h2>
                        <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', lineHeight: 1.5 }}>
                            {/* 5. Dynamically fetch the description based on the section id */}
                            {t(`emergency_info.sections.${s.id}.desc`)}
                        </p>
                    </button>
                ))}
                {tutorialMode && (
                    <button
                        className={activeStep?.target === 'map' ? 'tutorial-tour-highlight' : ''}
                        style={{
                            background: 'var(--bg-card)',
                            border: '1px solid #38bdf833',
                            borderRadius: 'var(--radius-lg)',
                            padding: '28px 24px',
                            cursor: 'not-allowed',
                            textAlign: 'left',
                            opacity: 0.72,
                            backdropFilter: 'blur(8px)',
                        }}
                        disabled
                    >
                        <span style={{ fontSize: '2.5rem', display: 'block', marginBottom: '12px' }}>
                            {t('tutorial.emergencyInfo.mapLabel')}
                        </span>
                        <h2
                            style={{
                                color: '#38bdf8',
                                fontSize: '1.2rem',
                                fontWeight: 700,
                                marginBottom: '8px',
                            }}
                        >
                            {t('tutorial.emergencyInfo.mapPreview')}
                        </h2>
                        <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', lineHeight: 1.5 }}>
                            {t('tutorial.emergencyInfo.mapDesc')}
                        </p>
                    </button>
                )}
            </div>

            <ChecklistModal open={checklistOpen} onClose={() => setChecklistOpen(false)} />
        </div>
    );
}
