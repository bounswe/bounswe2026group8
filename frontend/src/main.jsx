import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import { applyStoredTheme } from './utils/theme.js';
import './index.css';
import './i18n.js'

applyStoredTheme();

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
