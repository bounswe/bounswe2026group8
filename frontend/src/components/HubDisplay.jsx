import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';

export default function HubDisplay() {
  const { user } = useAuth();
  const { t } = useTranslation();

  if (!user || !user.hub) return null;

  const { hub } = user;
  const parts = [hub.district, hub.city, hub.country].filter(Boolean);
  const label = parts.length ? parts.join(', ') : hub.name;

  return (
    <div className="hub-display">
      <span className="hub-display-label">📍 {t('hub')}</span>
      <span className="hub-display-value" title={label}>{label}</span>
    </div>
  );
}
