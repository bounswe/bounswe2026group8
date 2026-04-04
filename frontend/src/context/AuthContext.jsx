import { createContext, useContext, useState, useEffect } from 'react';
import { getMe, getHubs, updateMe } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [hubs, setHubs] = useState([]);
  const [loading, setLoading] = useState(true);

  // Fetch hubs list on mount (public, no auth needed)
  useEffect(() => {
    getHubs().then(({ ok, data }) => {
      if (ok) setHubs(data);
    });
  }, []);

  // On mount, if a token exists in localStorage, validate it via GET /me
  useEffect(() => {
    if (!token) {
      setLoading(false);
      return;
    }

    getMe().then(({ ok, data }) => {
      if (ok) {
        setUser(data);
      } else {
        // Token invalid / expired — clear it
        localStorage.removeItem('token');
        setToken(null);
        setUser(null);
      }
      setLoading(false);
    });
  }, [token]);

  const loginUser = (newToken, userData) => {
    localStorage.setItem('token', newToken);
    setToken(newToken);
    setUser(userData);
  };

  const logoutUser = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
  };

  const updateHub = async (hub) => {
    const { ok, data } = await updateMe({ hub_id: hub.id });
    if (ok) setUser(data);
  };

  const value = {
    user,
    token,
    hubs,
    loading,
    isAuthenticated: !!token && !!user,
    loginUser,
    logoutUser,
    updateHub,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
