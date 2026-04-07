import { useNavigate } from 'react-router-dom';

export const CHECKLIST_TOPICS = [
  {
    slug: 'displacement',
    icon: '🚶',
    title: 'Displacement Skills',
    summary: 'How to safely move an injured patient to a safe area',
    color: '#f59e0b',
    text: `If there are dangers around (such as fire, electric dangers or others) the patient has to be moved to a safe place (if it is safe for the first aid provider to do so), where providing the required first aid procedures is possible.

— In case of a possible severe spinal injury: when a patient seems to have a possible serious injury in the spinal cord (in the backbone, either at the neck part or the back part), that patient must not be moved except if that is necessary. When necessary, it must be done as little as possible and very carefully. These precautions avoid many risks of causing further damages for the patient's mobility in the future.

Usually, the patient needs to end up lying down, in a face-up position, on a sufficiently firm surface (for example, on the floor, which allows you to perform the chest compressions of cardiopulmonary resuscitation).`,
  },
  {
    slug: 'checking',
    icon: '🔍',
    title: 'Checking Skills',
    summary: 'Evaluate the victim\'s condition and identify main threats to life',
    color: '#38bdf8',
    text: `Evaluate the condition of the victim, first attending to the main threats to life.

• Check Responsiveness: Tap the patient firmly on the shoulder and shout loudly, such as: "Can you hear me?"

• Control Severe Bleeding: If the victim has a wound that bleeds abundantly, this requires immediate treatment (begin by applying direct pressure to the wound).

• Check Pulse: If the patient does not react, check the carotid pulse by placing two fingers on either side of the neck. Alternatively, check the radial pulse on the wrist, under the thumb.

• Check Breathing: Place your ear near their mouth to listen/feel for air, while watching their chest to see if it rises and falls.

CRITICAL: Do not waste too much time checking. You should take no more than 10 seconds to check for breathing and a pulse before taking further action.`,
  },
  {
    slug: 'cpr',
    icon: '❤️',
    title: 'Cardiopulmonary Resuscitation',
    summary: 'Step-by-step CPR procedure for an unresponsive victim',
    color: '#f87171',
    text: `1. Place the heel of one hand on the center of the chest.

2. Place the other hand on top and interlace fingers.

3. Push hard and fast — at least 2 inches deep, 100–120 compressions per minute.

4. Let the chest rise completely between pushes.

5. If trained, provide 2 rescue breaths after every 30 compressions.`,
  },
  {
    slug: 'abc',
    icon: '🫁',
    title: 'ABC Protocol',
    summary: 'Airway, Breathing, Circulation — preserve life in emergencies',
    color: '#818cf8',
    text: `A — AIRWAY: Gently tilt the head back and lift the chin to open the airway.

B — BREATHING: Look, listen, and feel for normal breathing for no more than 10 seconds.

C — CIRCULATION: Check for severe bleeding. If the person is not breathing normally, begin CPR immediately.


PRESERVING LIFE

The patient must have an open airway — an unobstructed passage that allows air to travel from the mouth/nose into the lungs. Conscious people maintain this automatically, but unconscious patients may be unable to do so.

• The Recovery Position: The patient may be placed laying on their side. This relaxes the patient, clears the tongue from the pharynx, and avoids a common cause of death: choking on regurgitated stomach contents.

• Choking: The airway can become blocked by a foreign object. To dislodge it, use anti-choking methods (such as back slaps, chest thrusts, or abdominal thrusts).

• Transition to CPR: Once the airway has been opened, reassess the patient's breathing. If there is no breathing, or it is abnormal (e.g., agonal breathing), initiate CPR to force air into the lungs and manually pump blood around the body.`,
  },
];

export default function EmergencyChecklistPage() {
  const navigate = useNavigate();

  return (
    <div className="page">
      <header style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/emergency-info')}>
          ← Back
        </button>
        <h1 className="gradient-text" style={{ fontSize: '1.5rem' }}>
          Emergency Checklist
        </h1>
      </header>

      <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '24px' }}>
        Tap a topic to view the step-by-step first aid guide.
      </p>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
          gap: '16px',
        }}
      >
        {CHECKLIST_TOPICS.map((topic) => (
          <button
            key={topic.slug}
            onClick={() => navigate(`/emergency-info/checklist/${topic.slug}`)}
            style={{
              background: 'var(--bg-card)',
              border: `1px solid ${topic.color}33`,
              borderRadius: 'var(--radius-lg)',
              padding: '24px 20px',
              cursor: 'pointer',
              textAlign: 'left',
              transition: 'border-color 0.2s, transform 0.15s',
              backdropFilter: 'blur(8px)',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.borderColor = topic.color + '99';
              e.currentTarget.style.transform = 'translateY(-2px)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.borderColor = topic.color + '33';
              e.currentTarget.style.transform = 'translateY(0)';
            }}
          >
            <span style={{ fontSize: '2rem', display: 'block', marginBottom: '10px' }}>
              {topic.icon}
            </span>
            <h2
              style={{
                color: topic.color,
                fontSize: '1rem',
                fontWeight: 700,
                marginBottom: '6px',
              }}
            >
              {topic.title}
            </h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', lineHeight: 1.5 }}>
              {topic.summary}
            </p>
          </button>
        ))}
      </div>
    </div>
  );
}
