import { useState, useEffect } from 'react';
import { useNavigate, Navigate, Link } from 'react-router-dom';
import { register, getExpertiseCategories } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';
import LocationPickerModal from '../components/LocationPickerModal';

export default function SignUpPage() {
  const navigate = useNavigate();
  const { isAuthenticated, loading } = useAuth();
  const { t, i18n } = useTranslation();

  const [expertiseCategories, setExpertiseCategories] = useState([]);
  const [pickerOpen, setPickerOpen] = useState(false);

  const [form, setForm] = useState({
    full_name: '',
    email: '',
    password: '',
    confirm_password: '',
    role: 'STANDARD',
    hub_country: '',
    hub_city: '',
    hub_district: '',
    neighborhood_address: '',
    category_id: '',
  });

  useEffect(() => {
    getExpertiseCategories().then(({ ok, data }) => { if (ok) setExpertiseCategories(data); });
  }, []);

  const [errors, setErrors] = useState({});
  const [globalError, setGlobalError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  if (loading) {
    return (
        <div className="page" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
          <p style={{ color: 'var(--text-secondary)' }}>{t('sign_up.loading')}</p>
        </div>
    );
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    // Clear field-level error on change
    if (errors[name]) {
      setErrors((prev) => {
        const copy = { ...prev };
        delete copy[name];
        return copy;
      });
    }
  };

  // ---------- client-side validation ----------
  const validate = () => {
    const errs = {};
    if (!form.full_name.trim()) errs.full_name = t('sign_up.errors.full_name_req');
    if (!form.email.trim()) errs.email = t('sign_up.errors.email_req');
    if (!form.password) errs.password = t('sign_up.errors.password_req');
    if (form.password.length > 0 && form.password.length < 8)
      errs.password = t('sign_up.errors.password_min');
    if (form.password !== form.confirm_password)
      errs.confirm_password = t('sign_up.errors.password_match');
    if (form.role === 'EXPERT' && !form.category_id)
      errs.category_id = t('sign_up.errors.expertise_req');
    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGlobalError('');
    setErrors({});

    const clientErrors = validate();
    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors);
      return;
    }

    setSubmitting(true);

    const payload = {
      full_name: form.full_name.trim(),
      email: form.email.trim(),
      password: form.password,
      confirm_password: form.confirm_password,
      role: form.role,
    };
    if (form.hub_country && form.hub_city) {
      payload.hub_country = form.hub_country;
      payload.hub_city = form.hub_city;
      if (form.hub_district) payload.hub_district = form.hub_district;
    }
    if (form.neighborhood_address.trim()) {
      payload.neighborhood_address = form.neighborhood_address.trim();
    }
    if (form.role === 'EXPERT') {
      payload.category_id = Number(form.category_id);
    }

    const { ok, data } = await register(payload);
    setSubmitting(false);

    if (ok) {
      setSuccess(true);
      setTimeout(() => navigate('/signin'), 1500);
    } else {
      // Backend returns { message, errors: { field: [msgs] } }
      if (data.errors) {
        const mapped = {};
        for (const [field, msgs] of Object.entries(data.errors)) {
          mapped[field] = Array.isArray(msgs) ? msgs.join(' ') : msgs;
        }
        setErrors(mapped);
      }
      setGlobalError(data.message || t('sign_up.errors.failed'));
    }
  };

  return (
      <div className="page auth-page">
        <div className="auth-card">
          <h2 className="auth-title">{t('sign_up.header.title')}</h2>
          <p className="auth-subtitle">{t('sign_up.header.subtitle')}</p>

          {success && (
              <div className="alert alert-success">
                ✅ {t('sign_up.success')}
              </div>
          )}

          {globalError && !success && (
              <div className="alert alert-error">{globalError}</div>
          )}

          <form onSubmit={handleSubmit} noValidate>
            {/* Full Name */}
            <div className="form-group">
              <label htmlFor="full_name">{t('sign_up.labels.full_name')}</label>
              <input
                  id="full_name"
                  name="full_name"
                  type="text"
                  placeholder={t('sign_up.placeholders.full_name')}
                  value={form.full_name}
                  onChange={handleChange}
                  className={errors.full_name ? 'input-error' : ''}
              />
              {errors.full_name && (
                  <span className="field-error">{errors.full_name}</span>
              )}
            </div>

            {/* Email */}
            <div className="form-group">
              <label htmlFor="email">{t('sign_up.labels.email')}</label>
              <input
                  id="email"
                  name="email"
                  type="email"
                  placeholder={t('sign_up.placeholders.email')}
                  value={form.email}
                  onChange={handleChange}
                  className={errors.email ? 'input-error' : ''}
              />
              {errors.email && (
                  <span className="field-error">{errors.email}</span>
              )}
            </div>

            {/* Password */}
            <div className="form-group">
              <label htmlFor="password">{t('sign_up.labels.password')}</label>
              <div className="password-input-wrapper">
                <input
                    id="password"
                    name="password"
                    type={showPassword ? 'text' : 'password'}
                    placeholder={t('sign_up.placeholders.password')}
                    value={form.password}
                    onChange={handleChange}
                    className={errors.password ? 'input-error' : ''}
                />
                <button
                    type="button"
                    className="password-toggle-btn"
                    onClick={() => setShowPassword(!showPassword)}
                    aria-label={showPassword ? t('sign_up.actions.hide_password') : t('sign_up.actions.show_password')}
                >
                  {showPassword ? '👁️' : '👁️‍🗨️'}
                </button>
              </div>
              {errors.password && (
                  <span className="field-error">{errors.password}</span>
              )}
            </div>

            {/* Confirm Password */}
            <div className="form-group">
              <label htmlFor="confirm_password">{t('sign_up.labels.confirm_password')}</label>
              <div className="password-input-wrapper">
                <input
                    id="confirm_password"
                    name="confirm_password"
                    type={showConfirmPassword ? 'text' : 'password'}
                    placeholder={t('sign_up.placeholders.confirm_password')}
                    value={form.confirm_password}
                    onChange={handleChange}
                    className={errors.confirm_password ? 'input-error' : ''}
                />
                <button
                    type="button"
                    className="password-toggle-btn"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    aria-label={showConfirmPassword ? t('sign_up.actions.hide_password') : t('sign_up.actions.show_password')}
                >
                  {showConfirmPassword ? '👁️' : '👁️‍🗨️'}
                </button>
              </div>
              {errors.confirm_password && (
                  <span className="field-error">{errors.confirm_password}</span>
              )}
            </div>

            {/* Role */}
            <div className="form-group">
              <label htmlFor="role">{t('sign_up.labels.role')}</label>
              <select
                  id="role"
                  name="role"
                  value={form.role}
                  onChange={handleChange}
              >
                <option value="STANDARD">{t('sign_up.roles.standard')}</option>
                <option value="EXPERT">{t('sign_up.roles.expert')}</option>
              </select>
            </div>

            {/* Hub */}
            <div className="form-group">
              <label>{t('sign_up.labels.hub')}</label>
              <button
                  type="button"
                  className="btn btn-secondary btn-block location-picker-trigger"
                  onClick={() => setPickerOpen(true)}
              >
                {form.hub_city
                    ? [form.hub_district, form.hub_city, form.hub_country].filter(Boolean).join(', ')
                    : t('sign_up.placeholders.select_hub')}
              </button>
            </div>

            {/* Expertise category — only visible for EXPERT */}
            {form.role === 'EXPERT' && (
                <div className="form-group slide-in">
                  <label htmlFor="category_id">{t('sign_up.labels.expertise_field')}</label>
                  <select
                      id="category_id"
                      name="category_id"
                      value={form.category_id}
                      onChange={handleChange}
                      className={errors.category_id ? 'input-error' : ''}
                  >
                    <option value="">— {t('sign_up.placeholders.expertise_field')} —</option>
                    {['MEDICAL', 'SHELTER', 'TRANSPORT', 'FOOD', 'OTHER'].map((grp) => {
                      const items = expertiseCategories.filter((c) => c.help_request_category === grp);
                      return items.length ? (
                          <optgroup key={grp} label={t(`help_requests.categories.${grp.toLowerCase()}`)}>
                            {items.map((c) => (
                                <option key={c.id} value={c.id}>{c.translations?.[i18n.language] || c.name}</option>
                            ))}
                          </optgroup>
                      ) : null;
                    })}
                  </select>
                  {errors.category_id && (
                      <span className="field-error">{errors.category_id}</span>
                  )}
                </div>
            )}

            {/* Neighbourhood / Address (optional) */}
            <div className="form-group">
              <label htmlFor="neighborhood_address">
                {t('sign_up.labels.neighborhood_address')}{' '}
                <span className="optional-tag">{t('sign_up.labels.optional')}</span>
              </label>
              <input
                  id="neighborhood_address"
                  name="neighborhood_address"
                  type="text"
                  placeholder={t('sign_up.placeholders.neighborhood_address')}
                  value={form.neighborhood_address}
                  onChange={handleChange}
              />
            </div>

            <button
                type="submit"
                className="btn btn-primary btn-block"
                disabled={submitting}
            >
              {submitting ? t('sign_up.actions.signing_up') : t('sign_up.actions.sign_up')}
            </button>
          </form>

          <p className="auth-footer">
            {t('sign_up.footer.already_have_account')}{' '}
            <Link to="/signin" className="link">
              {t('sign_up.actions.sign_in')}
            </Link>
          </p>
        </div>
        <LocationPickerModal
            open={pickerOpen}
            initial={{ country: form.hub_country, city: form.hub_city, district: form.hub_district }}
            onClose={() => setPickerOpen(false)}
            onSelect={({ country, city, district }) => {
              setForm((prev) => ({ ...prev, hub_country: country, hub_city: city, hub_district: district }));
              setPickerOpen(false);
            }}
        />
      </div>
  );
}