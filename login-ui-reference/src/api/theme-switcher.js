/**
 * Theme Switcher — loads saved theme from localStorage on init
 * and exposes a simple API for switching themes.
 *
 * Usage (in any page's <script>):
 *   import { initTheme, setTheme, getTheme, THEMES } from '/ui/src/common/theme-switcher.js';
 *
 * Theme classes on <html>:
 *   theme-light  — default, no class needed
 *   theme-green  — <html class="theme-green">
 *
 * Drop-in UI: call mountThemeSwitcher(container) to append a theme picker button.
 */

const THEME_KEY = 'sentinel-theme';

/** Map of display name → class name */
export const THEMES = {
  light: '',
  green: 'theme-green',
};

let _current = THEMES.light;

/** Apply a theme by key name ('light' | 'green') */
export function setTheme(name) {
  const cls = THEMES[name] || '';
  const html = document.documentElement;

  // Remove all theme classes
  Object.values(THEMES).forEach(c => {
    if (c) html.classList.remove(c);
  });

  // Apply the new one (empty string = default/no class)
  if (cls) html.classList.add(cls);

  _current = name;
  localStorage.setItem(THEME_KEY, name);

  // Notify listeners (e.g. ECharts charts) that theme changed
  window.dispatchEvent(new CustomEvent('sentinel-theme-change', { detail: { theme: name } }));
}

/** Return the currently active theme key */
export function getTheme() {
  return _current;
}

/** Initialise: read localStorage and apply saved theme */
export function initTheme() {
  const saved = localStorage.getItem(THEME_KEY) || 'light';
  setTheme(saved);
  return saved;
}

/**
 * Mount a theme-switcher button into `container`.
 * The button cycles through light → green → light.
 * Accessible label updates on each click.
 */
export function mountThemeSwitcher(container) {
  const btn = document.createElement('button');
  btn.setAttribute('aria-label', '切换主题');
  btn.setAttribute('title', '切换主题');
  btn.className = 'theme-switcher-btn';

  const ICONS = { light: 'light_mode', green: 'eco' };
  const LABELS = { light: '浅色主题', green: '绿色主题' };

  function refresh() {
    const current = getTheme();
    btn.innerHTML = `<span class="material-symbols-outlined text-xl">${ICONS[current] || ICONS.light}</span>`;
    btn.setAttribute('aria-label', LABELS[current] || LABELS.light);
  }

  btn.addEventListener('click', () => {
    const order = ['light', 'green'];
    const idx = order.indexOf(getTheme());
    setTheme(order[(idx + 1) % order.length]);
    refresh();
  });

  refresh();
  container.appendChild(btn);
}
