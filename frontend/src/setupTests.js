/**
 * Jest setup file — runs after the test framework is initialised.
 *
 * 1. Polyfills TextEncoder/TextDecoder — react-router-dom v7 uses the
 *    Fetch/Streams API internally which requires these globals in jsdom.
 * 2. Sets IS_REACT_ACT_ENVIRONMENT so React 18+ knows act() is available.
 * 3. Imports @testing-library/jest-dom for DOM-specific matchers.
 */
const { TextEncoder, TextDecoder } = require('util');
global.TextEncoder = TextEncoder;
global.TextDecoder = TextDecoder;

global.IS_REACT_ACT_ENVIRONMENT = true;

import '@testing-library/jest-dom';
