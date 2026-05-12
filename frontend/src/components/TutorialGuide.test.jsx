import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import useTutorialGuide from './TutorialGuide';

const STEPS = [
  { target: 'intro', title: 'Start here', text: 'This is the first step.' },
  { target: 'action', title: 'Take action', text: 'This is the second step.' },
  { target: 'finish', title: 'Finish up', text: 'This is the final step.' },
];

function TutorialGuideHarness({ storageKey = 'tutorial-guide-test-key', restartLabel }) {
  const { activeStep, stepIndex, GuidePanel, RestartButton } = useTutorialGuide({
    storageKey,
    steps: STEPS,
    restartLabel,
  });

  return (
    <div>
      <span data-testid="active-target">{activeStep?.target || 'none'}</span>
      <span data-testid="step-index">{stepIndex ?? 'none'}</span>
      {GuidePanel}
      {RestartButton}
    </div>
  );
}

describe('useTutorialGuide', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('starts on the first step when the guide has not been seen', () => {
    render(<TutorialGuideHarness />);

    expect(screen.getByTestId('active-target')).toHaveTextContent('intro');
    expect(screen.getByTestId('step-index')).toHaveTextContent('0');
    expect(screen.getByText('Start here')).toBeInTheDocument();
    expect(screen.getByText('This is the first step.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /previous/i })).toBeDisabled();
  });

  it('moves forward and backward through guide steps', async () => {
    const user = userEvent.setup();
    render(<TutorialGuideHarness />);

    await user.click(screen.getByRole('button', { name: /next/i }));
    expect(screen.getByTestId('active-target')).toHaveTextContent('action');
    expect(screen.getByText('Take action')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /previous/i })).toBeEnabled();

    await user.click(screen.getByRole('button', { name: /previous/i }));
    expect(screen.getByTestId('active-target')).toHaveTextContent('intro');
    expect(screen.getByText('Start here')).toBeInTheDocument();
  });

  it('marks the guide as seen when skipped', async () => {
    const user = userEvent.setup();
    render(<TutorialGuideHarness />);

    await user.click(screen.getByRole('button', { name: /skip/i }));

    expect(localStorage.getItem('tutorial-guide-test-key')).toBe('true');
    expect(screen.getByTestId('active-target')).toHaveTextContent('none');
    expect(screen.getByRole('button', { name: /show guide/i })).toBeInTheDocument();
  });

  it('hides skip on the last step and stores completion on finish', async () => {
    const user = userEvent.setup();
    render(<TutorialGuideHarness />);

    await user.click(screen.getByRole('button', { name: /next/i }));
    await user.click(screen.getByRole('button', { name: /next/i }));

    expect(screen.getByText('Finish up')).toBeInTheDocument();
    const skipButton = screen.getByText(/skip guide/i);
    expect(skipButton).toBeDisabled();
    expect(skipButton).toHaveAttribute('aria-hidden', 'true');

    await user.click(screen.getByRole('button', { name: /finish/i }));

    expect(localStorage.getItem('tutorial-guide-test-key')).toBe('true');
    expect(screen.getByTestId('active-target')).toHaveTextContent('none');
  });

  it('starts hidden when localStorage says the guide has already been seen', () => {
    localStorage.setItem('tutorial-guide-test-key', 'true');

    render(<TutorialGuideHarness />);

    expect(screen.getByTestId('active-target')).toHaveTextContent('none');
    expect(screen.queryByText('Start here')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /show guide/i })).toBeInTheDocument();
  });

  it('removes the seen flag and restarts the guide', async () => {
    const user = userEvent.setup();
    localStorage.setItem('tutorial-guide-test-key', 'true');

    render(<TutorialGuideHarness restartLabel="Restart tour" />);

    await user.click(screen.getByRole('button', { name: /restart tour/i }));

    expect(localStorage.getItem('tutorial-guide-test-key')).toBeNull();
    expect(screen.getByTestId('active-target')).toHaveTextContent('intro');
    expect(screen.getByText('Start here')).toBeInTheDocument();
  });
});
