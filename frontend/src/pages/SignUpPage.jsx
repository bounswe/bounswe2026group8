import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { register } from '../services/api';

export default function SignUpPage() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    full_name: '',
    email: '',
    password: '',
    confirm_password: '',
    role: 'STANDARD',
    neighborhood_address: '',
    expertise_field: '',
  });

  const [errors, setErrors] = useState({});
  const [globalError, setGlobalError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

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
    if (!form.full_name.trim()) errs.full_name = 'Full name is required.';
    if (!form.email.trim()) errs.email = 'Email is required.';
    if (!form.password) errs.password = 'Password is required.';
    if (form.password.length > 0 && form.password.length < 8)
      errs.password = 'Password must be at least 8 characters.';
    if (form.password !== form.confirm_password)
      errs.confirm_password = 'Passwords do not match.';
    if (form.role === 'EXPERT' && !form.expertise_field.trim())
      errs.expertise_field = 'Expertise field is required for Expert users.';
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

    // Build the payload matching the backend contract exactly
    const payload = {
      full_name: form.full_name.trim(),
      email: form.email.trim(),
      password: form.password,
      confirm_password: form.confirm_password,
      role: form.role,
    };
    if (form.neighborhood_address.trim()) {
      payload.neighborhood_address = form.neighborhood_address.trim();
    }
    if (form.role === 'EXPERT') {
      payload.expertise_field = form.expertise_field.trim();
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
      setGlobalError(data.message || 'Registration failed');
    }
  };

  return (
    <div className="page auth-page">
      <div className="auth-card">
        <h2 className="auth-title">Create Account</h2>
        <p className="auth-subtitle">Join your neighbourhood emergency hub</p>

        {success && (
          <div className="alert alert-success">
            ✅ Account created! Redirecting to sign in…
          </div>
        )}

        {globalError && !success && (
          <div className="alert alert-error">{globalError}</div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          {/* Full Name */}
          <div className="form-group">
            <label htmlFor="full_name">Full Name</label>
            <input
              id="full_name"
              name="full_name"
              type="text"
              placeholder="e.g. Sheila Davis"
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
            <label htmlFor="email">Email</label>
            <input
              id="email"
              name="email"
              type="email"
              placeholder="sheila@example.com"
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
            <label htmlFor="password">Password</label>
            <div className="password-input-wrapper">
              <input
                id="password"
                name="password"
                type={showPassword ? 'text' : 'password'}
                placeholder="Min. 8 characters"
                value={form.password}
                onChange={handleChange}
                className={errors.password ? 'input-error' : ''}
              />
              <button
                type="button"
                className="password-toggle-btn"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
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
            <label htmlFor="confirm_password">Confirm Password</label>
            <div className="password-input-wrapper">
              <input
                id="confirm_password"
                name="confirm_password"
                type={showConfirmPassword ? 'text' : 'password'}
                placeholder="Repeat your password"
                value={form.confirm_password}
                onChange={handleChange}
                className={errors.confirm_password ? 'input-error' : ''}
              />
              <button
                type="button"
                className="password-toggle-btn"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
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
            <label htmlFor="role">Role</label>
            <select
              id="role"
              name="role"
              value={form.role}
              onChange={handleChange}
            >
              <option value="STANDARD">Standard</option>
              <option value="EXPERT">Expert</option>
            </select>
          </div>

          {/* Expertise field — only visible for EXPERT */}
          {form.role === 'EXPERT' && (
            <div className="form-group slide-in">
              <label htmlFor="expertise_field">Expertise Field</label>
              <input
                id="expertise_field"
                name="expertise_field"
                type="text"
                placeholder="e.g. Medical Doctor"
                value={form.expertise_field}
                onChange={handleChange}
                className={errors.expertise_field ? 'input-error' : ''}
              />
              {errors.expertise_field && (
                <span className="field-error">{errors.expertise_field}</span>
              )}
            </div>
          )}

          {/* Neighbourhood / Address (optional) */}
          <div className="form-group">
            <label htmlFor="neighborhood_address">
              Neighbourhood / Address{' '}
              <span className="optional-tag">optional</span>
            </label>
            <input
              id="neighborhood_address"
              name="neighborhood_address"
              type="text"
              placeholder="e.g. Sariyer, Istanbul"
              value={form.neighborhood_address}
              onChange={handleChange}
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-block"
            disabled={submitting}
          >
            {submitting ? 'Creating account…' : 'Sign Up'}
          </button>
        </form>

        <p className="auth-footer">
          Already have an account?{' '}
          <Link to="/signin" className="link">
            Sign In
          </Link>
        </p>
      </div>
    </div>
  );
}
