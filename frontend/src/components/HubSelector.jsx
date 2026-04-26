import { useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';

export default function HubSelector() {
  const { user, hubs, updateHub } = useAuth();
  const { t } = useTranslation();

  // Auto-assign first hub if user has none
  useEffect(() => {
    if (user && hubs.length && !user.hub) {
      updateHub(hubs[0]);
    }
  }, [user, hubs]);

  if (!user || !hubs.length) return null;

  const selectedHub = user.hub || hubs[0];

  const handleChange = (e) => {
    const hub = hubs.find((h) => String(h.id) === e.target.value);
    if (hub) updateHub(hub);
  };

  return (
    <div className="hub-selector-bar">
      <div className="hub-selector">
        <span className="hub-selector-label"> 📍{t('hub')}</span>
        <select value={String(selectedHub.id)} onChange={handleChange}>
          {hubs.map((h) => (
            <option key={h.id} value={String(h.id)}>
              {h.name}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
