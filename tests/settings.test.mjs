import test from 'node:test';
import assert from 'node:assert/strict';
import { formatCurrency, migrateSettings, resolveLightTheme } from '../modules/settings.js';

test('migrates V6 state without deleting records', () => {
  const state = { theme: 'auto', anthropicApiKey: 'must-not-survive', entries: [{ id: '1' }] };
  migrateSettings(state);
  assert.equal(state.theme, 'system');
  assert.equal(state.settings.currency, 'THB');
  assert.equal(state.entries.length, 1);
  assert.equal('anthropicApiKey' in state, false);
});

test('supports configurable currency and 0-3 decimals', () => {
  assert.match(formatCurrency(12.345, { currency: 'USD', decimals: 3 }), /12\.345/);
  assert.match(formatCurrency(12.9, { currency: 'JPY', decimals: 0 }), /13/);
});

test('supports four theme modes', () => {
  assert.equal(resolveLightTheme('light', false), true);
  assert.equal(resolveLightTheme('dark', true), false);
  assert.equal(resolveLightTheme('system', true), true);
  assert.equal(resolveLightTheme('auto', false, new Date('2026-01-01T12:00:00')), true);
  assert.equal(resolveLightTheme('auto', true, new Date('2026-01-01T23:00:00')), false);
});
