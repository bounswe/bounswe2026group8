import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LandingPage() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  // If already logged in, offer a shortcut to the dashboard
  const handleGetStarted = () => {
    navigate(isAuthenticated ? '/dashboard' : '/signup');
  };

  return (
    <div className="page landing-page">
      {/* Hero section */}
      <div className="landing-hero">
        <div className="landing-hero-glow" />
        <h1 className="landing-title">
          Neighborhood<br />
          <span className="gradient-text">Emergency Hub</span>
        </h1>
        <p className="landing-subtitle">
          Prepare for emergencies. Connect with neighbours. Build community
          resilience through mutual aid&nbsp;&mdash;&nbsp;even when the
          internet is down.
        </p>

        <div className="landing-actions">
          <button className="btn btn-primary" onClick={handleGetStarted}>
            Get Started
          </button>
          <button
            className="btn btn-secondary"
            onClick={() => navigate('/signin')}
          >
            Sign In
          </button>
        </div>
      </div>

      {/* Feature cards */}
      <div className="landing-features">
        {[
          {
            icon: '🛡️',
            title: 'Disaster Preparedness',
            desc: 'Access critical information and emergency plans for your neighbourhood.',
          },
          {
            icon: '🤝',
            title: 'Mutual Aid',
            desc: 'Offer and request help from neighbours during emergencies.',
          },
          {
            icon: '📡',
            title: 'Offline-First',
            desc: 'Key data stays accessible even without an internet connection.',
          },
        ].map((f) => (
          <div className="feature-card" key={f.title}>
            <span className="feature-icon">{f.icon}</span>
            <h3>{f.title}</h3>
            <p>{f.desc}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
