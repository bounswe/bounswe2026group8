/**
 * Jest setup file — runs after the test framework is initialised.
 *
 * 1. Polyfills TextEncoder/TextDecoder — react-router-dom v7 uses the
 *    Fetch/Streams API internally which requires these globals in jsdom.
 * 2. Sets IS_REACT_ACT_ENVIRONMENT so React 18+ knows act() is available.
 * 3. Imports @testing-library/jest-dom for DOM-specific matchers.
 */
/* global global, jest, require */
const { TextEncoder, TextDecoder } = require('util');
global.TextEncoder = TextEncoder;
global.TextDecoder = TextDecoder;

global.IS_REACT_ACT_ENVIRONMENT = true;

import '@testing-library/jest-dom';

jest.mock('react-i18next', () => {
  const enTranslations = require('./locales/en.json');

  const humanizeKey = (key) => {
    const last = String(key).split('.').pop();
    return last
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  };

  const lookupTranslation = (key) => {
    return String(key)
      .split('.')
      .reduce((value, segment) => (value && value[segment] != null ? value[segment] : undefined), enTranslations);
  };

  const translate = (key, options = {}) => {
    const value = lookupTranslation(key);
    if (typeof value !== 'string') return options.defaultValue || humanizeKey(key);
    return value.replace(/\{\{(\w+)\}\}/g, (_, name) => options[name] ?? '');
  };

  return {
    useTranslation: () => ({
      t: translate,
      i18n: {
        language: 'en',
        changeLanguage: jest.fn(),
      },
    }),
  };
});
