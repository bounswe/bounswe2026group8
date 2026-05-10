const THEME_STORAGE_KEY = 'emergencyhub.theme';
const THEME_DARK = 'dark';
const THEME_LIGHT = 'light';

export function getStoredTheme() {
  if (typeof window === 'undefined') return THEME_DARK;
  return window.localStorage.getItem(THEME_STORAGE_KEY) === THEME_LIGHT ? THEME_LIGHT : THEME_DARK;
}

export function applyTheme(theme) {
  if (typeof document === 'undefined') return;
  const nextTheme = theme === THEME_LIGHT ? THEME_LIGHT : THEME_DARK;
  document.documentElement.dataset.theme = nextTheme;
  document.documentElement.style.colorScheme = nextTheme;
}

export function saveTheme(theme) {
  const nextTheme = theme === THEME_LIGHT ? THEME_LIGHT : THEME_DARK;
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(THEME_STORAGE_KEY, nextTheme);
  }
  applyTheme(nextTheme);
  return nextTheme;
}

export function applyStoredTheme() {
  applyTheme(getStoredTheme());
}

export function isDarkTheme(theme) {
  return theme !== THEME_LIGHT;
}
