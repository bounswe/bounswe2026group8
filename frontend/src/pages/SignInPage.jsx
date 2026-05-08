import { useState } from 'react';
import { useNavigate, Navigate, Link } from 'react-router-dom';
import { login } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';

export default function SignInPage() {
  const navigate = useNavigate();
  const { loginUser, isAuthenticated, loading } = useAuth();
  const { t } = useTranslation(); // Initialize hook

  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  if (loading) {
    return (
        <div className="page" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
          <p style={{ color: 'var(--text-secondary)' }}>{t('sign_in.loading')}</p>
        </div>
    );
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleChange = (e) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
    if (error) setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!form.email.trim() || !form.password) {
      setError(t('sign_in.errors.empty_fields'));
      return;
    }

    setSubmitting(true);
    const { ok, data } = await login({
      email: form.email.trim(),
      password: form.password,
    });
    setSubmitting(false);

    if (ok) {
      // Backend returns { message, token, refresh, user }
      loginUser(data.token, data.user);
      navigate('/dashboard', { replace: true });
    } else {
      // Backend returns { message: "Invalid email or password" }
      setError(data.message || t('sign_in.errors.invalid_credentials'));
    }
  };

  return (
      <div className="page auth-page">
        <div className="auth-card">
          <h2 className="auth-title">{t('sign_in.header.title')}</h2>
          <p className="auth-subtitle">{t('sign_in.header.subtitle')}</p>

          {error && <div className="alert alert-error">{error}</div>}

          <form onSubmit={handleSubmit} noValidate>
            <div className="form-group">
              <label htmlFor="email">{t('sign_in.labels.email')}</label>
              <input
                  id="email"
                  name="email"
                  type="email"
                  placeholder={t('sign_in.placeholders.email')}
                  value={form.email}
                  onChange={handleChange}
              />
            </div>

            <div className="form-group">
              <label htmlFor="password">{t('sign_in.labels.password')}</label>
              <div className="password-input-wrapper">
                <input
                    id="password"
                    name="password"
                    type={showPassword ? 'text' : 'password'}
                    placeholder={t('sign_in.placeholders.password')}
                    value={form.password}
                    onChange={handleChange}
                />
                <button
                    type="button"
                    className="password-toggle-btn"
                    onClick={() => setShowPassword(!showPassword)}
                    aria-label={showPassword ? t('sign_in.actions.hide_password') : t('sign_in.actions.show_password')}
                >
                  {showPassword ? '👁️' : '👁️‍🗨️'}
                </button>
              </div>
            </div>

            <button
                type="submit"
                className="btn btn-primary btn-block"
                disabled={submitting}
            >
              {submitting ? t('sign_in.actions.signing_in') : t('sign_in.actions.sign_in')}
            </button>
          </form>

          <div className="auth-tutorial-divider">
            <span>{t('tutorial.common.newHere')}</span>
          </div>

          <button
              type="button"
              className="btn btn-secondary btn-block auth-tutorial-btn"
              onClick={() => navigate('/tutorial')}
          >
            {t('tutorial.common.tryScenario')}
          </button>

          <p className="auth-footer">
            {t('sign_in.footer.no_account')}{' '}
            <Link to="/signup" className="link">
              {t('sign_in.actions.sign_up')}
            </Link>
          </p>
        </div>
      </div>
  );
}
