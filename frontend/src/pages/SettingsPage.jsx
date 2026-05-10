import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { getSettings, updateSettings } from '../services/api';
import { getStoredTheme, isDarkTheme, saveTheme } from '../utils/theme';

const NOTIFICATION_FIELDS = [
  'notify_help_requests',
  'notify_urgent_posts',
  'notify_expertise_matches_only',
];

const PRIVACY_FIELDS = [
  'show_phone_number',
  'show_emergency_contact',
  'show_medical_info',
  'show_availability_status',
  'show_bio',
  'show_location',
  'show_resources',
  'show_expertise',
];

function SettingsToggle({ checked, disabled, label, desc, onChange }) {
  return (
    <label className={`settings-toggle-row ${disabled ? 'settings-toggle-row-disabled' : ''}`}>
      <span className="settings-toggle-copy">
        <span className="settings-toggle-title">{label}</span>
        <span className="settings-toggle-desc">{desc}</span>
      </span>
      <span className="toggle-label">
        <input
          type="checkbox"
          className="toggle-cb"
          checked={!!checked}
          disabled={disabled}
          onChange={onChange}
        />
        <span className="toggle-track"><span className="toggle-thumb" /></span>
      </span>
    </label>
  );
}

export default function SettingsPage() {
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [settings, setSettings] = useState(null);
  const [theme, setTheme] = useState(() => getStoredTheme());
  const [loading, setLoading] = useState(true);
  const [savingField, setSavingField] = useState(null);
  const [toast, setToast] = useState(null);

  useEffect(() => {
    getSettings().then(({ ok, data }) => {
      if (ok) setSettings(data);
      else setToast({ type: 'error', msg: t('settings.toasts.load_failed') });
      setLoading(false);
    });
  }, [t]);

  const notify = (msg, type = 'success') => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 2500);
  };

  const toggleTheme = () => {
    const nextTheme = isDarkTheme(theme) ? 'light' : 'dark';
    setTheme(saveTheme(nextTheme));
  };

  const toggleField = async (field) => {
    const nextValue = !settings[field];
    const previous = settings;
    setSettings((current) => ({ ...current, [field]: nextValue }));
    setSavingField(field);

    const { ok, data } = await updateSettings({ [field]: nextValue });
    setSavingField(null);

    if (ok) {
      setSettings(data);
      notify(t('settings.toasts.saved'));
    } else {
      setSettings(previous);
      notify(data?.message || t('settings.toasts.save_failed'), 'error');
    }
  };

  if (loading) {
    return (
      <div className="page settings-page">
        <p>{t('settings.states.loading')}</p>
      </div>
    );
  }

  if (!settings) {
    return (
      <div className="page settings-page">
        <div className="alert alert-error">{t('settings.states.unavailable')}</div>
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/dashboard')}>
          &larr; {t('settings.header.back')}
        </button>
      </div>
    );
  }

  return (
    <div className="page settings-page">
      {toast && <div className={`profile-toast ${toast.type === 'error' ? 'profile-toast-error' : ''}`}>{toast.msg}</div>}

      <header className="dashboard-header page-main-header">
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/dashboard')}>
          &larr; {t('settings.header.back')}
        </button>
        <h2>{t('settings.header.title')}</h2>
      </header>

      <div className="profile-section-card">
        <h4 className="profile-section-title">{t('settings.sections.appearance')}</h4>
        <div className="settings-list">
          <SettingsToggle
            checked={isDarkTheme(theme)}
            label={t('settings.fields.dark_mode.label')}
            desc={t('settings.fields.dark_mode.desc')}
            onChange={toggleTheme}
          />
        </div>
      </div>

      <div className="profile-section-card">
        <h4 className="profile-section-title">{t('settings.sections.notifications')}</h4>
        <div className="settings-list">
          {NOTIFICATION_FIELDS.map((field) => (
            <SettingsToggle
              key={field}
              checked={settings[field]}
              disabled={savingField === field}
              label={t(`settings.fields.${field}.label`)}
              desc={t(`settings.fields.${field}.desc`)}
              onChange={() => toggleField(field)}
            />
          ))}
        </div>
      </div>

      <div className="profile-section-card">
        <h4 className="profile-section-title">{t('settings.sections.privacy')}</h4>
        <p className="settings-section-desc">{t('settings.privacy_note')}</p>
        <div className="settings-list">
          {PRIVACY_FIELDS.map((field) => (
            <SettingsToggle
              key={field}
              checked={settings[field]}
              disabled={savingField === field}
              label={t(`settings.fields.${field}.label`)}
              desc={t(`settings.fields.${field}.desc`)}
              onChange={() => toggleField(field)}
            />
          ))}
        </div>
      </div>
    </div>
  );
}
