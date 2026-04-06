import { useAuth } from '../context/AuthContext';

export default function HubSelector() {
  const { user, hubs, updateHub } = useAuth();

  if (!user || !hubs.length || !user.hub) return null;

  const handleChange = (e) => {
    const hub = hubs.find((h) => String(h.id) === e.target.value);
    if (hub) updateHub(hub);
  };

  return (
    <div className="hub-selector">
      <select value={String(user.hub.id)} onChange={handleChange}>
        {hubs.map((h) => (
          <option key={h.id} value={String(h.id)}>
            {h.name}
          </option>
        ))}
      </select>
    </div>
  );
}
