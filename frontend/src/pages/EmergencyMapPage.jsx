import { useState, useEffect, useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup, Circle, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { useAuth } from '../context/AuthContext';

const RADIUS_KM = 5;
const DEFAULT_LAT = 41.0105;
const DEFAULT_LON = 28.985;
const CACHE_KEY = 'ei_cache';
const CACHE_TTL_MS = 30 * 60 * 1000;

const TYPE_META = {
  hospital:     { icon: '🏥', label: 'Hospital',       color: '#f87171' },
  gathering:    { icon: '📍', label: 'Assembly Point',  color: '#38bdf8' },
  shelter:      { icon: '🏠', label: 'Shelter',         color: '#34d399' },
  fire_station: { icon: '🚒', label: 'Fire Station',    color: '#fb923c' },
  police:       { icon: '🚔', label: 'Police Station',  color: '#a78bfa' },
};

const hubCoordsCache = {};

async function geocodeHub(name) {
  if (hubCoordsCache[name]) return hubCoordsCache[name];
  try {
    const resp = await fetch(
      `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(name)}`,
      { headers: { 'User-Agent': 'EmergencyHub/1.0' } }
    );
    const data = await resp.json();
    if (data.length === 0) return null;
    const result = { lat: parseFloat(data[0].lat), lon: parseFloat(data[0].lon) };
    hubCoordsCache[name] = result;
    return result;
  } catch { return null; }
}

function makeIcon(type) {
  const meta = TYPE_META[type] || { color: '#94a3b8', icon: '📌' };
  return L.divIcon({
    className: 'ei-marker',
    html: `<div style="background:${meta.color};width:32px;height:32px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:16px;border:2px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,0.3);">${meta.icon}</div>`,
    iconSize: [32, 32],
    iconAnchor: [16, 16],
    popupAnchor: [0, -18],
  });
}

const userIcon = L.divIcon({
  className: 'ei-marker',
  html: '<div style="background:#38bdf8;width:16px;height:16px;border-radius:50%;border:3px solid #fff;box-shadow:0 0 8px rgba(56,189,248,0.6);"></div>',
  iconSize: [16, 16],
  iconAnchor: [8, 8],
});

function haversineKm(lat1, lon1, lat2, lon2) {
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function parseOverpassResponse(json) {
  return (json?.elements ?? [])
    .map((el) => {
      try {
        let lat, lon;
        if (el.lat != null) { lat = el.lat; lon = el.lon; }
        else if (el.center) { lat = el.center.lat; lon = el.center.lon; }
        else return null;
        const tags = el.tags || {};
        const name = tags.name || tags['name:en'];
        const amenity = tags.amenity;
        const emergency = tags.emergency;
        let type;
        if (emergency === 'assembly_point') type = 'gathering';
        else if (amenity === 'hospital' || amenity === 'clinic') type = 'hospital';
        else if (amenity === 'fire_station') type = 'fire_station';
        else if (amenity === 'police') type = 'police';
        else type = 'shelter';
        return { name: name || TYPE_META[type]?.label || 'Unknown', lat, lon, type };
      } catch { return null; }
    })
    .filter(Boolean);
}

function getCachedPoints(lat, lon) {
  try {
    const raw = localStorage.getItem(CACHE_KEY);
    if (!raw) return null;
    const cache = JSON.parse(raw);
    if (Date.now() - cache.time > CACHE_TTL_MS) return null;
    if (haversineKm(lat, lon, cache.lat, cache.lon) > 2) return null;
    return cache.points;
  } catch { return null; }
}

function setCachedPoints(lat, lon, points) {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify({ lat, lon, points, time: Date.now() }));
  } catch { /* quota exceeded */ }
}

const MAX_RETRIES = 2;
const RETRY_DELAY_MS = 3000;

async function fetchGatheringPoints(lat, lon) {
  const cached = getCachedPoints(lat, lon);
  if (cached) return cached;

  const radiusM = Math.round(RADIUS_KM * 1000);
  const query = `[out:json][timeout:25];
(
  node["emergency"="assembly_point"](around:${radiusM},${lat},${lon});
  node["amenity"="shelter"](around:${radiusM},${lat},${lon});
  node["amenity"="hospital"](around:${radiusM},${lat},${lon});
  node["amenity"="clinic"](around:${radiusM},${lat},${lon});
  node["amenity"="fire_station"](around:${radiusM},${lat},${lon});
  node["amenity"="police"](around:${radiusM},${lat},${lon});
  way["amenity"="hospital"](around:${radiusM},${lat},${lon});
  way["amenity"="fire_station"](around:${radiusM},${lat},${lon});
);
out center;`;

  let lastError;
  for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
    if (attempt > 0) await new Promise((r) => setTimeout(r, RETRY_DELAY_MS));
    try {
      const resp = await fetch('https://overpass-api.de/api/interpreter', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `data=${encodeURIComponent(query)}`,
      });
      if (resp.status === 429) { lastError = new Error('Rate limited by Overpass API. Try again in a minute.'); continue; }
      if (resp.status === 504) { lastError = new Error('Overpass API timed out (504). Retrying…'); continue; }
      if (!resp.ok) throw new Error(`Overpass HTTP ${resp.status}`);
      const points = parseOverpassResponse(await resp.json());
      setCachedPoints(lat, lon, points);
      return points;
    } catch (e) {
      lastError = e;
    }
  }
  throw lastError;
}


function FlyTo({ lat, lon }) {
  const map = useMap();
  useEffect(() => { map.flyTo([lat, lon], 14, { duration: 1 }); }, [lat, lon, map]);
  return null;
}

function PointCard({ point, userLat, userLon }) {
  const dist = haversineKm(userLat, userLon, point.lat, point.lon);
  const meta = TYPE_META[point.type] || { icon: '📌', label: point.type };
  const mapsUrl = `https://www.google.com/maps/dir/?api=1&destination=${point.lat},${point.lon}`;
  return (
    <div className="ei-point-card">
      <span className="ei-point-icon">{meta.icon}</span>
      <div className="ei-point-body">
        <span className="ei-point-name">{point.name}</span>
        <span className="ei-point-dist">{dist.toFixed(2)} km away</span>
      </div>
      <a href={mapsUrl} target="_blank" rel="noopener noreferrer" className="btn btn-sm btn-secondary">
        Directions
      </a>
    </div>
  );
}

export default function EmergencyMapPage() {
  const { user } = useAuth();
  const [points, setPoints] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [userLat, setUserLat] = useState(DEFAULT_LAT);
  const [userLon, setUserLon] = useState(DEFAULT_LON);
  const [locationSet, setLocationSet] = useState(false);
  const [locationStatus, setLocationStatus] = useState('');
  const [activeType, setActiveType] = useState('all');
  const [showList, setShowList] = useState(false);
  const [showMap, setShowMap] = useState(false);

  const load = useCallback(async (lat, lon) => {
    setLoading(true);
    setError(null);
    try {
      const pts = await fetchGatheringPoints(lat, lon);
      pts.sort((a, b) => haversineKm(lat, lon, a.lat, a.lon) - haversineKm(lat, lon, b.lat, b.lon));
      setPoints(pts);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  const applyLocation = useCallback((lat, lon, status) => {
    setUserLat(lat);
    setUserLon(lon);
    setLocationSet(true);
    setLocationStatus(status);
    load(lat, lon);
  }, [load]);

  const tryGps = useCallback(() => {
    if (!navigator.geolocation) {
      fallbackToHub();
      return;
    }
    setLocationStatus('Detecting location…');
    navigator.geolocation.getCurrentPosition(
      (pos) => applyLocation(
        pos.coords.latitude, pos.coords.longitude,
        `GPS (${pos.coords.latitude.toFixed(4)}, ${pos.coords.longitude.toFixed(4)})`
      ),
      () => fallbackToHub(),
      { enableHighAccuracy: true, timeout: 10000 }
    );

    async function fallbackToHub() {
      if (user?.hub?.name) {
        setLocationStatus(`Resolving ${user.hub.name}…`);
        const coords = await geocodeHub(user.hub.name);
        if (coords) {
          applyLocation(coords.lat, coords.lon, `${user.hub.name} (hub)`);
          return;
        }
      }
      setLocationStatus('Could not determine location');
    }
  }, [applyLocation]);

  useEffect(() => { tryGps(); }, [tryGps]);

  const types = ['all', ...Object.keys(TYPE_META)];
  const filtered = activeType === 'all' ? points : points.filter((p) => p.type === activeType);

  const nearestByType = useMemo(() => {
    const m = {};
    for (const p of points) { if (!m[p.type]) m[p.type] = p; }
    return m;
  }, [points]);

  const markerIcons = useMemo(() => {
    const icons = {};
    for (const t of Object.keys(TYPE_META)) icons[t] = makeIcon(t);
    return icons;
  }, []);

  return (
    <div className="page ei-page">
      <Link to="/dashboard" className="post-back-link">← Dashboard</Link>
      <h1 className="ei-title">Emergency Information</h1>
      <p className="ei-subtitle">Nearby emergency facilities within {RADIUS_KM} km of your location. Allow location access to see facilities near you.</p>

      {/* Location bar */}
      <div className="ei-location-bar">
        <span className="ei-location-icon">📍</span>
        <span className="ei-location-text">{locationStatus || 'Detecting location…'}</span>
        <button className="btn btn-sm btn-secondary" onClick={tryGps}>Refresh Location</button>
      </div>

      {/* Loading */}
      {loading && (
        <div className="ei-loading">
          <div className="ei-spinner" />
          <p>Fetching nearby emergency facilities…</p>
        </div>
      )}

      {error && <div className="alert alert-error">{error}</div>}

      {/* Content — after data loads */}
      {!loading && points.length > 0 && (
        <>
          {/* Nearest summary cards */}
          <div className="ei-nearest-grid">
            {Object.entries(nearestByType).map(([type, p]) => {
              const meta = TYPE_META[type] || { icon: '📌', label: type };
              const dist = haversineKm(userLat, userLon, p.lat, p.lon);
              return (
                <div key={type} className="ei-nearest-card">
                  <span className="ei-nearest-icon">{meta.icon}</span>
                  <div className="ei-nearest-body">
                    <span className="ei-nearest-label">Nearest {meta.label}</span>
                    <span className="ei-nearest-name">{p.name}</span>
                    <span className="ei-nearest-dist">{dist.toFixed(2)} km</span>
                  </div>
                </div>
              );
            })}
          </div>

          {/* View toggles */}
          <div className="ei-view-toggles">
            <button className={`ei-view-btn ${showMap ? 'ei-view-btn--active' : ''}`}
              onClick={() => setShowMap(!showMap)}>
              🗺️ {showMap ? 'Hide Map' : 'Show Map'}
            </button>
            <button className={`ei-view-btn ${showList ? 'ei-view-btn--active' : ''}`}
              onClick={() => setShowList(!showList)}>
              📋 {showList ? 'Hide List' : 'Show List'}
            </button>
          </div>

          {/* Filter tabs */}
          <div className="ei-tabs">
            {types.map((t) => {
              const label = t === 'all' ? 'All' : TYPE_META[t]?.label;
              const count = t === 'all' ? points.length : points.filter((p) => p.type === t).length;
              return (
                <button key={t} className={`ei-tab ${activeType === t ? 'ei-tab--active' : ''}`}
                  onClick={() => setActiveType(t)}>
                  {t !== 'all' && <span>{TYPE_META[t]?.icon}</span>} {label} ({count})
                </button>
              );
            })}
          </div>

          {/* Map — only rendered when toggled on */}
          {showMap && (
            <div className="ei-map-wrapper">
              <MapContainer center={[userLat, userLon]} zoom={14} className="ei-map" scrollWheelZoom>
                <TileLayer
                  attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                  url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                <FlyTo lat={userLat} lon={userLon} />
                <Marker position={[userLat, userLon]} icon={userIcon}>
                  <Popup>Your location</Popup>
                </Marker>
                <Circle
                  center={[userLat, userLon]}
                  radius={RADIUS_KM * 1000}
                  pathOptions={{ color: '#38bdf8', fillColor: '#38bdf8', fillOpacity: 0.06, weight: 1 }}
                />
                {filtered.map((p, i) => (
                  <Marker key={`${p.lat}-${p.lon}-${i}`} position={[p.lat, p.lon]}
                    icon={markerIcons[p.type] || markerIcons.shelter}>
                    <Popup>
                      <strong>{p.name}</strong><br />
                      {TYPE_META[p.type]?.label} — {haversineKm(userLat, userLon, p.lat, p.lon).toFixed(2)} km<br />
                      <a href={`https://www.google.com/maps/dir/?api=1&destination=${p.lat},${p.lon}`}
                        target="_blank" rel="noopener noreferrer">Get directions</a>
                    </Popup>
                  </Marker>
                ))}
              </MapContainer>
            </div>
          )}

          {/* List view */}
          {showList && filtered.length > 0 && (
            <div className="ei-list">
              {filtered.map((p, i) => (
                <PointCard key={`${p.lat}-${p.lon}-${i}`} point={p} userLat={userLat} userLon={userLon} />
              ))}
            </div>
          )}

          {filtered.length === 0 && (
            <div className="ei-empty">
              <span className="ei-empty-icon">🔍</span>
              <h3>No facilities of this type found</h3>
              <p>Try selecting a different category.</p>
            </div>
          )}
        </>
      )}

      {locationSet && !loading && !error && points.length === 0 && (
        <div className="ei-empty">
          <span className="ei-empty-icon">🔍</span>
          <h3>No facilities found nearby</h3>
          <p>No emergency facilities found within {RADIUS_KM} km of your location.</p>
        </div>
      )}
    </div>
  );
}
