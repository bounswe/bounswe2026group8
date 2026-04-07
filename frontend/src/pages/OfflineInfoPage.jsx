import { useNavigate } from 'react-router-dom';

export default function OfflineInfoPage() {
  const navigate = useNavigate();

  return (
    <div className="page">
      <header style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem' }}>
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/dashboard')}>← Back</button>
        <h2 className="gradient-text">Offline Information</h2>
      </header>

      <div className="welcome-card">
        <h3>📶 Critical Data Access</h3>
        <p>
          This page provides essential information that remains accessible even
          when you lose internet connectivity. Keep this data handy during
          emergencies.
        </p>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginTop: '1.5rem' }}>
        <div className="card">
          <h3>🚨 Emergency Contacts</h3>
          <ul style={{ margin: '0.5rem 0', paddingLeft: '1.25rem' }}>
            <li>Emergency Services: <strong>112</strong></li>
            <li>Fire Department: <strong>110</strong></li>
            <li>Ambulance: <strong>112</strong></li>
            <li>Police: <strong>155</strong></li>
            <li>AFAD (Disaster Management): <strong>122</strong></li>
          </ul>
        </div>

        <div className="card">
          <h3>🏥 First Aid Basics</h3>
          <ul style={{ margin: '0.5rem 0', paddingLeft: '1.25rem' }}>
            <li>Check the scene for safety before approaching.</li>
            <li>Call emergency services immediately.</li>
            <li>Apply pressure to stop bleeding.</li>
            <li>Do not move injured persons unless in immediate danger.</li>
            <li>Keep the person warm and calm until help arrives.</li>
          </ul>
        </div>

        <div className="card">
          <h3>🌍 Earthquake Safety</h3>
          <ul style={{ margin: '0.5rem 0', paddingLeft: '1.25rem' }}>
            <li><strong>Drop, Cover, Hold On</strong> during shaking.</li>
            <li>Stay away from windows and heavy objects.</li>
            <li>After shaking stops, evacuate to an open area.</li>
            <li>Do not use elevators.</li>
            <li>Be prepared for aftershocks.</li>
          </ul>
        </div>

        <div className="card">
          <h3>🎒 Emergency Kit Checklist</h3>
          <ul style={{ margin: '0.5rem 0', paddingLeft: '1.25rem' }}>
            <li>Water (at least 3 liters per person per day)</li>
            <li>Non-perishable food</li>
            <li>Flashlight and extra batteries</li>
            <li>First aid kit</li>
            <li>Whistle to signal for help</li>
            <li>Important documents (copies)</li>
            <li>Medications</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
