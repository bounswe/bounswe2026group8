/** @type {import('jest').Config} */
module.exports = {
  testEnvironment: 'jsdom',

  // Run @testing-library/jest-dom matchers after the test framework loads
  setupFilesAfterEnv: ['<rootDir>/src/setupTests.js'],

  // Transform JS/JSX/TS/TSX with Babel (handles import.meta and JSX)
  transform: {
    '^.+\\.[jt]sx?$': 'babel-jest',
  },

  // Stub out CSS imports — styles are irrelevant in unit/integration tests
  moduleNameMapper: {
    '\\.(css|less|scss|sass)$': '<rootDir>/src/__mocks__/styleMock.cjs',
  },

  // Only pick up test files — ignore Vite build output
  testMatch: [
    '**/__tests__/**/*.[jt]s?(x)',
    '**/?(*.)+(spec|test).[jt]s?(x)',
  ],

  // Do not transform node_modules (except packages that ship ESM only)
  transformIgnorePatterns: ['/node_modules/(?!(.*\\.mjs$))'],

  // Playwright e2e specs live under tests/ — Jest must not pick them up
  testPathIgnorePatterns: ['/node_modules/', '<rootDir>/tests/'],
};
