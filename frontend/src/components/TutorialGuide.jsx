import { useState } from 'react';
import { useTranslation } from 'react-i18next';

export default function useTutorialGuide({ storageKey, steps, restartLabel = 'Show guide' }) {
  const { t } = useTranslation();
  const [stepIndex, setStepIndex] = useState(() => (
    window.localStorage.getItem(storageKey) === 'true' ? null : 0
  ));

  const closeGuide = () => {
    window.localStorage.setItem(storageKey, 'true');
    setStepIndex(null);
  };

  const restartGuide = () => {
    window.localStorage.removeItem(storageKey);
    setStepIndex(0);
  };

  const nextGuideStep = () => {
    if (stepIndex === steps.length - 1) {
      closeGuide();
    } else {
      setStepIndex((current) => current + 1);
    }
  };

  const previousGuideStep = () => {
    setStepIndex((current) => Math.max(0, current - 1));
  };

  const activeStep = stepIndex !== null ? steps[stepIndex] : null;

  return {
    activeStep,
    stepIndex,
    GuidePanel: activeStep ? (
      <section className="tutorial-guide-panel" aria-live="polite">
        <div>
          <span className="tutorial-tour-count">
            {t('tutorial.guide.step', { current: stepIndex + 1, total: steps.length })}
          </span>
          <h3>{activeStep.title}</h3>
          <p>{activeStep.text}</p>
        </div>
        <div className="tutorial-tour-actions">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={closeGuide}
            disabled={stepIndex === steps.length - 1}
            aria-hidden={stepIndex === steps.length - 1}
            tabIndex={stepIndex === steps.length - 1 ? -1 : 0}
            style={stepIndex === steps.length - 1 ? { visibility: 'hidden' } : undefined}
          >
            {t('tutorial.guide.skip')}
          </button>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={previousGuideStep}
            disabled={stepIndex === 0}
          >
            {t('tutorial.guide.previous')}
          </button>
          <button type="button" className="btn btn-primary btn-sm" onClick={nextGuideStep}>
            {stepIndex === steps.length - 1 ? t('tutorial.guide.finish') : t('tutorial.guide.next')}
          </button>
        </div>
      </section>
    ) : null,
    RestartButton: !activeStep ? (
      <button type="button" className="btn btn-secondary btn-sm" onClick={restartGuide}>
        {restartLabel === 'Show guide' ? t('tutorial.guide.show') : restartLabel}
      </button>
    ) : null,
  };
}
