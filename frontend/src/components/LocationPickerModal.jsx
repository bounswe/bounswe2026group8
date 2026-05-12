import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import locations from '../data/locations.json';

const STEP_COUNTRY = 0;
const STEP_CITY = 1;
const STEP_DISTRICT = 2;

export default function LocationPickerModal({ open, initial, onClose, onSelect }) {
  const { t } = useTranslation();
  const countries = useMemo(
    () => [...locations.countries].sort((a, b) => a.name.localeCompare(b.name)),
    [],
  );

  const [step, setStep] = useState(STEP_COUNTRY);
  const [country, setCountry] = useState(initial?.country || '');
  const [city, setCity] = useState(initial?.city || '');
  const [district, setDistrict] = useState(initial?.district || '');
  const [query, setQuery] = useState('');

  useEffect(() => {
    if (open) {
      setCountry(initial?.country || '');
      setCity(initial?.city || '');
      setDistrict(initial?.district || '');
      setQuery('');
      setStep(STEP_COUNTRY);
    }
  }, [open, initial]);

  if (!open) return null;

  const selectedCountry = countries.find((c) => c.name === country);
  const sortedCities = selectedCountry
    ? [...selectedCountry.cities].sort((a, b) => a.name.localeCompare(b.name))
    : [];
  const selectedCity = sortedCities.find((c) => c.name === city);
  const sortedDistricts = selectedCity?.districts
    ? [...selectedCity.districts].sort((a, b) => a.localeCompare(b))
    : [];

  const handleCountrySelect = (c) => {
    setCountry(c.name);
    setCity('');
    setDistrict('');
    setQuery('');
    setStep(STEP_CITY);
  };

  const handleCitySelect = (c) => {
    setCity(c.name);
    setDistrict('');
    setQuery('');
    if (c.districts && c.districts.length > 0) {
      setStep(STEP_DISTRICT);
    } else {
      onSelect({ country, city: c.name, district: '' });
    }
  };

  const handleDistrictSelect = (d) => {
    setDistrict(d);
    onSelect({ country, city, district: d });
  };

  const handleBack = () => {
    if (step === STEP_CITY) setStep(STEP_COUNTRY);
    else if (step === STEP_DISTRICT) setStep(STEP_CITY);
  };

  const lowerQuery = query.trim().toLowerCase();
  const filteredCountries = lowerQuery
    ? countries.filter((c) => c.name.toLowerCase().includes(lowerQuery))
    : countries;
  const filteredCities = lowerQuery
    ? sortedCities.filter((c) => c.name.toLowerCase().includes(lowerQuery))
    : sortedCities;
  const filteredDistricts = lowerQuery
    ? sortedDistricts.filter((d) => d.toLowerCase().includes(lowerQuery))
    : sortedDistricts;

  let title;
  if (step === STEP_COUNTRY) title = t('location_picker.choose_country', 'Choose country');
  else if (step === STEP_CITY) title = t('location_picker.choose_city', 'Choose city in {{country}}', { country });
  else title = t('location_picker.choose_district', 'Choose district in {{city}}', { city });

  return (
    <div className="location-picker-overlay" onClick={onClose}>
      <div className="location-picker-modal" onClick={(e) => e.stopPropagation()}>
        <div className="location-picker-header">
          {step > STEP_COUNTRY && (
            <button type="button" className="location-picker-back" onClick={handleBack}>
              &larr;
            </button>
          )}
          <h3>{title}</h3>
          <button type="button" className="location-picker-close" onClick={onClose}>
            ×
          </button>
        </div>

        <input
          type="text"
          className="location-picker-search"
          placeholder={t('location_picker.search', 'Search...')}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          autoFocus
        />

        <div className="location-picker-list">
          {step === STEP_COUNTRY &&
            filteredCountries.map((c) => (
              <button
                key={c.code}
                type="button"
                className={`location-picker-item ${country === c.name ? 'is-selected' : ''}`}
                onClick={() => handleCountrySelect(c)}
              >
                {c.name}
              </button>
            ))}
          {step === STEP_CITY &&
            filteredCities.map((c) => (
              <button
                key={c.name}
                type="button"
                className={`location-picker-item ${city === c.name ? 'is-selected' : ''}`}
                onClick={() => handleCitySelect(c)}
              >
                {c.name}
                {c.districts && c.districts.length > 0 && (
                  <span className="location-picker-arrow">›</span>
                )}
              </button>
            ))}
          {step === STEP_DISTRICT &&
            filteredDistricts.map((d) => (
              <button
                key={d}
                type="button"
                className={`location-picker-item ${district === d ? 'is-selected' : ''}`}
                onClick={() => handleDistrictSelect(d)}
              >
                {d}
              </button>
            ))}
          {step === STEP_COUNTRY && filteredCountries.length === 0 && (
            <p className="location-picker-empty">{t('location_picker.no_results', 'No results')}</p>
          )}
          {step === STEP_CITY && filteredCities.length === 0 && (
            <p className="location-picker-empty">{t('location_picker.no_results', 'No results')}</p>
          )}
          {step === STEP_DISTRICT && filteredDistricts.length === 0 && (
            <p className="location-picker-empty">{t('location_picker.no_results', 'No results')}</p>
          )}
        </div>
      </div>
    </div>
  );
}
