export const CURRENCIES = Object.freeze({
  THB: { label: 'บาทไทย (THB)', locale: 'th-TH' },
  USD: { label: 'ดอลลาร์สหรัฐ (USD)', locale: 'en-US' },
  EUR: { label: 'ยูโร (EUR)', locale: 'de-DE' },
  GBP: { label: 'ปอนด์อังกฤษ (GBP)', locale: 'en-GB' },
  JPY: { label: 'เยนญี่ปุ่น (JPY)', locale: 'ja-JP' }
});

export const DEFAULT_SETTINGS = Object.freeze({
  currency: 'THB',
  decimals: 2,
  weatherEnabled: true,
  autoOcrEnabled: false
});

export function migrateSettings(state) {
  const settings = { ...DEFAULT_SETTINGS, ...(state.settings || {}) };
  if (!CURRENCIES[settings.currency]) settings.currency = DEFAULT_SETTINGS.currency;
  settings.decimals = Math.min(3, Math.max(0, Number(settings.decimals) || 0));
  settings.weatherEnabled = settings.weatherEnabled !== false;
  settings.autoOcrEnabled = settings.autoOcrEnabled === true;
  state.settings = settings;
  // Until V7, "auto" meant "follow the operating system".
  if (state.theme === 'auto' && !state.themeMigratedV7) state.theme = 'system';
  if (!['light', 'dark', 'system', 'auto'].includes(state.theme)) state.theme = 'system';
  state.themeMigratedV7 = true;
  delete state.anthropicApiKey;
  return state;
}

export function formatCurrency(value, settings = DEFAULT_SETTINGS) {
  const currency = CURRENCIES[settings.currency] ? settings.currency : 'THB';
  const decimals = Math.min(3, Math.max(0, Number(settings.decimals) || 0));
  return new Intl.NumberFormat(CURRENCIES[currency].locale, {
    style: 'currency',
    currency,
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  }).format(Number(value) || 0);
}

export function resolveLightTheme(theme, systemPrefersLight, date = new Date()) {
  if (theme === 'light') return true;
  if (theme === 'dark') return false;
  if (theme === 'auto') {
    const hour = date.getHours();
    return hour >= 6 && hour < 18;
  }
  return Boolean(systemPrefersLight);
}
