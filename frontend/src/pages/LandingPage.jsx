import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';

export default function LandingPage() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  // Extract translator 't' and engine 'i18n'
  const { t, i18n } = useTranslation();

  // If already logged in, offer a shortcut to the dashboard
  const handleGetStarted = () => {
    navigate(isAuthenticated ? '/dashboard' : '/signup');
  };

  // Helper to change languages
  const changeLanguage = (lng) => {
    i18n.changeLanguage(lng);
  };

  return (
    <div className="page landing-page">

      {/* Hero section */}
      <div className="landing-hero">
        <div className="landing-hero-glow" />
        <h1 className="landing-title">
          {t('landing.title_main')}<br />
          <span className="gradient-text">{t('landing.title_sub')}</span>
        </h1>
        <p className="landing-subtitle">
          {t('landing.subtitle')}
        </p>

        <div className="landing-actions">
          <button className="btn btn-primary" onClick={handleGetStarted}>
            {t('landing.btn_get_started')}
          </button>
          <button
              className="btn btn-secondary"
              onClick={() => navigate('/signin')}
          >
            {t('landing.btn_sign_in')}
          </button>
        </div>
      </div>

      {/* Feature cards */}
      <div className="landing-features">
        {t('landing.features', { returnObjects: true }).map((f, index) => (
            <div className="feature-card" key={index}>
              <span className="feature-icon">{f.icon}</span>
              <h3>{f.title}</h3>
              <p>{f.desc}</p>
            </div>
        ))}
      </div>
    </div>
  );
}
