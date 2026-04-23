import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

// 1. Import your dictionaries
import enTranslations from './locales/en.json';
import trTranslations from './locales/tr.json';

// 2. Configure the engine
i18n
    .use(initReactI18next) // Passes the i18n instance to React
    .init({
        // Bundle the dictionaries into an object i18next can read
        resources: {
            en: { translation: enTranslations },
            tr: { translation: trTranslations }
        },

        lng: 'en', // The initial default language
        fallbackLng: 'en', // If a translation is missing in Turkish, fall back to English

        interpolation: {
            escapeValue: false // React already protects against XSS (cross-site scripting), so we disable this
        }
    });

export default i18n;