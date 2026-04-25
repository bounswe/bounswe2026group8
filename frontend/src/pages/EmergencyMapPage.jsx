import { useState, useEffect, useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup, Circle, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { useAuth } from '../context/AuthContext';
import { useTranslation } from 'react-i18next';

const RADIUS_KM = 5;
const DEFAULT_LAT = 41.0105;
const DEFAULT_LON = 28.985;
const CACHE_KEY = 'ei_cache';
const CACHE_TTL_MS = 30 * 60 * 1000;

// Extract the labels out of this metadata object and will map them dynamically in the component
const TYPE_META = {
  hospital:     { icon: '🏥', color: '#f87171' },
  gathering:    { icon: '📍', color: '#38bdf8' },
  shelter:      { icon: '🏠', color: '#34d399' },
  fire_station: { icon: '🚒', color: '#fb923c' },
  police:       { icon: '🚔', color: '#a78bfa' },
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

// Pass 't' down to translate the 'Unknown' fallback
function parseOverpassResponse(json, t) {
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

          // Dynamically fetch label from JSON
          const typeLabel = t(`emergency_map.types.${type}`);
          return { name: name || typeLabel || t('emergency_map.states.unknown'), lat, lon, type };
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

// 4. Pass 't' down to translate the API errors
async function fetchGatheringPoints(lat, lon, t) {
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
      if (resp.status === 429) { lastError = new Error(t('emergency_map.states.rate_limited')); continue; }
      if (resp.status === 504) { lastError = new Error(t('emergency_map.states.timeout')); continue; }
      if (!resp.ok) throw new Error(`Overpass HTTP ${resp.status}`);
      const points = parseOverpassResponse(await resp.json(), t);
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

// 5. Pass 't' down to translate the card details
function PointCard({ point, userLat, userLon, t }) {
  const dist = haversineKm(userLat, userLon, point.lat, point.lon);
  const meta = TYPE_META[point.type] || { icon: '📌' };
  const mapsUrl = `https://www.google.com/maps/dir/?api=1&destination=${point.lat},${point.lon}`;
  return (
      <div className="ei-point-card">
        <span className="ei-point-icon">{meta.icon}</span>
        <div className="ei-point-body">
          <span className="ei-point-name">{point.name}</span>
          <span className="ei-point-dist">{t('emergency_map.card.away', { dist: dist.toFixed(2) })}</span>
        </div>
        <a href={mapsUrl} target="_blank" rel="noopener noreferrer" className="btn btn-sm btn-secondary">
          {t('emergency_map.card.directions')}
        </a>
      </div>
  );
}

export default function EmergencyMapPage() {
  const { user } = useAuth();
  const { t } = useTranslation(); // 6. Initialize hook

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

  // 7. Pass 't' into the load function
  const load = useCallback(async (lat, lon) => {
    setLoading(true);
    setError(null);
    try {
      const pts = await fetchGatheringPoints(lat, lon, t);
      pts.sort((a, b) => haversineKm(lat, lon, a.lat, a.lon) - haversineKm(lat, lon, b.lat, b.lon));
      setPoints(pts);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [t]);

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
    setLocationStatus(t('emergency_map.location.detecting'));
    navigator.geolocation.getCurrentPosition(
        (pos) => applyLocation(
            pos.coords.latitude, pos.coords.longitude,
            `${t('emergency_map.location.gps')} (${pos.coords.latitude.toFixed(4)}, ${pos.coords.longitude.toFixed(4)})`
        ),
        () => fallbackToHub(),
        { enableHighAccuracy: true, timeout: 10000 }
    );

    async function fallbackToHub() {
      if (user?.hub?.name) {
        setLocationStatus(t('emergency_map.location.resolving', { name: user.hub.name }));
        const coords = await geocodeHub(user.hub.name);
        if (coords) {
          applyLocation(coords.lat, coords.lon, `${user.hub.name} (${t('emergency_map.location.hub')})`);
          return;
        }
      }
      setLocationStatus(t('emergency_map.location.not_found'));
    }
  }, [applyLocation, user, t]);

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
    for (const type of Object.keys(TYPE_META)) icons[type] = makeIcon(type);
    return icons;
  }, []);

  return (
      <div className="page ei-page">
        <Link to="/dashboard" className="post-back-link">← {t('emergency_map.header.back')}</Link>
        <h1 className="ei-title">{t('emergency_map.header.title')}</h1>
        <p className="ei-subtitle">{t('emergency_map.header.subtitle', { radius: RADIUS_KM })}</p>

        <div className="ei-location-bar">
          <span className="ei-location-icon">📍</span>
          <span className="ei-location-text">{locationStatus || t('emergency_map.location.detecting')}</span>
          <button className="btn btn-sm btn-secondary" onClick={tryGps}>{t('emergency_map.location.refresh')}</button>
        </div>

        {loading && (
            <div className="ei-loading">
              <div className="ei-spinner" />
              <p>{t('emergency_map.states.fetching')}</p>
            </div>
        )}

        {error && <div className="alert alert-error">{error}</div>}

        {!loading && points.length > 0 && (
            <>
              <div className="ei-nearest-grid">
                {Object.entries(nearestByType).map(([type, p]) => {
                  const meta = TYPE_META[type] || { icon: '📌' };
                  const dist = haversineKm(userLat, userLon, p.lat, p.lon);
                  // 8. Fetch label dynamically
                  const typeLabel = t(`emergency_map.types.${type}`);
                  return (
                      <div key={type} className="ei-nearest-card">
                        <span className="ei-nearest-icon">{meta.icon}</span>
                        <div className="ei-nearest-body">
                          <span className="ei-nearest-label">{t('emergency_map.card.nearest', { type: typeLabel })}</span>
                          <span className="ei-nearest-name">{p.name}</span>
                          <span className="ei-nearest-dist">{t('emergency_map.card.distance', { dist: dist.toFixed(2) })}</span>
                        </div>
                      </div>
                  );
                })}
              </div>

              <div className="ei-view-toggles">
                <button className={`ei-view-btn ${showMap ? 'ei-view-btn--active' : ''}`}
                        onClick={() => setShowMap(!showMap)}>
                  🗺️ {showMap ? t('emergency_map.toggles.hide_map') : t('emergency_map.toggles.show_map')}
                </button>
                <button className={`ei-view-btn ${showList ? 'ei-view-btn--active' : ''}`}
                        onClick={() => setShowList(!showList)}>
                  📋 {showList ? t('emergency_map.toggles.hide_list') : t('emergency_map.toggles.show_list')}
                </button>
              </div>

              <div className="ei-tabs">
                {types.map((typeKey) => {
                  const label = typeKey === 'all' ? t('emergency_map.toggles.all') : t(`emergency_map.types.${typeKey}`);
                  const count = typeKey === 'all' ? points.length : points.filter((p) => p.type === typeKey).length;
                  return (
                      <button key={typeKey} className={`ei-tab ${activeType === typeKey ? 'ei-tab--active' : ''}`}
                              onClick={() => setActiveType(typeKey)}>
                        {typeKey !== 'all' && <span>{TYPE_META[typeKey]?.icon}</span>} {label} ({count})
                      </button>
                  );
                })}
              </div>

              {showMap && (
                  <div className="ei-map-wrapper">
                    <MapContainer center={[userLat, userLon]} zoom={14} className="ei-map" scrollWheelZoom>
                      <TileLayer
                          attribution='© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                      />
                      <FlyTo lat={userLat} lon={userLon} />
                      <Marker position={[userLat, userLon]} icon={userIcon}>
                        <Popup>{t('emergency_map.map_popup.your_location')}</Popup>
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
                              {t(`emergency_map.types.${p.type}`)} — {haversineKm(userLat, userLon, p.lat, p.lon).toFixed(2)} km<br />
                              <a href={`https://www.google.com/maps/dir/?api=1&destination=${p.lat},${p.lon}`}
                                 target="_blank" rel="noopener noreferrer">{t('emergency_map.map_popup.get_directions')}</a>
                            </Popup>
                          </Marker>
                      ))}
                    </MapContainer>
                  </div>
              )}

              {showList && filtered.length > 0 && (
                  <div className="ei-list">
                    {filtered.map((p, i) => (
                        <PointCard key={`${p.lat}-${p.lon}-${i}`} point={p} userLat={userLat} userLon={userLon} t={t} />
                    ))}
                  </div>
              )}

              {filtered.length === 0 && (
                  <div className="ei-empty">
                    <span className="ei-empty-icon">🔍</span>
                    <h3>{t('emergency_map.empty.no_filter_match_title')}</h3>
                    <p>{t('emergency_map.empty.no_filter_match_desc')}</p>
                  </div>
              )}
            </>
        )}

        {locationSet && !loading && !error && points.length === 0 && (
            <div className="ei-empty">
              <span className="ei-empty-icon">🔍</span>
              <h3>{t('emergency_map.empty.no_facilities_title')}</h3>
              <p>{t('emergency_map.empty.no_facilities_desc', { radius: RADIUS_KM })}</p>
            </div>
        )}
      </div>
  );
}