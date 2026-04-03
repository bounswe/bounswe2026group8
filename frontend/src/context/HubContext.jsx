import { createContext, useContext, useState, useEffect } from 'react';
import { getHubs } from '../services/api';
import { useAuth } from './AuthContext';

const HubContext = createContext(null);

export function HubProvider({ children }) {
  const { user } = useAuth();
  const [hubs, setHubs] = useState([]);
  const [selectedHub, setSelectedHub] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getHubs().then(({ ok, data }) => {
      if (ok) {
        setHubs(data);
        const saved = localStorage.getItem('selectedHubId');
        const istanbul = data.find((h) => h.slug === 'istanbul');
        // Priority: user's registered hub > manually saved hub > istanbul fallback
        const userHub = user?.hub ? data.find((h) => h.id === user.hub.id) : null;
        const initial = userHub
          || (saved ? data.find((h) => String(h.id) === saved) : null)
          || istanbul;
        if (initial) setSelectedHub(initial);
      }
      setLoading(false);
    });
  }, []);

  // Sync selected hub when the logged-in user changes (e.g. after login/logout)
  useEffect(() => {
    if (!hubs.length) return;
    if (user?.hub) {
      const hub = hubs.find((h) => h.id === user.hub.id);
      if (hub) setSelectedHub(hub);
    }
  }, [user]);

  const selectHub = (hub) => {
    setSelectedHub(hub);
    localStorage.setItem('selectedHubId', String(hub.id));
  };

  return (
    <HubContext.Provider value={{ hubs, selectedHub, selectHub, loading }}>
      {children}
    </HubContext.Provider>
  );
}

export function useHub() {
  const context = useContext(HubContext);
  if (!context) {
    throw new Error('useHub must be used within a HubProvider');
  }
  return context;
}
