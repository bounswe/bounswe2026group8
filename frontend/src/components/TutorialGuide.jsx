import { useState } from 'react';

export default function useTutorialGuide({ storageKey, steps, restartLabel = 'Show guide' }) {
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
            Step {stepIndex + 1} of {steps.length}
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
            Skip guide
          </button>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={previousGuideStep}
            disabled={stepIndex === 0}
          >
            Previous
          </button>
          <button type="button" className="btn btn-primary btn-sm" onClick={nextGuideStep}>
            {stepIndex === steps.length - 1 ? 'Finish guide' : 'Next'}
          </button>
        </div>
      </section>
    ) : null,
    RestartButton: !activeStep ? (
      <button type="button" className="btn btn-secondary btn-sm" onClick={restartGuide}>
        {restartLabel}
      </button>
    ) : null,
  };
}
