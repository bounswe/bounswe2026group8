import { useHub } from '../context/HubContext';

export default function HubSelector() {
  const { hubs, selectedHub, selectHub, loading } = useHub();

  if (loading || !selectedHub) return null;

  const handleChange = (e) => {
    const hub = hubs.find((h) => String(h.id) === e.target.value);
    if (hub) selectHub(hub);
  };

  return (
    <div className="hub-selector">
      <select value={String(selectedHub.id)} onChange={handleChange}>
        {hubs.map((h) => (
          <option key={h.id} value={String(h.id)}>
            {h.name}
          </option>
        ))}
      </select>
    </div>
  );
}
