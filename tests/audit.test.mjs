import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');
const sw = await readFile(new URL('../sw.js', import.meta.url), 'utf8');
const weather = await readFile(new URL('../modules/weather.js', import.meta.url), 'utf8');
const functions = await readFile(new URL('../functions/index.js', import.meta.url), 'utf8');
const index = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const styles = await readFile(new URL('../styles.css', import.meta.url), 'utf8');

test('counts use integer formatting independent of price decimals', () => {
  assert.match(app, /fmtCount\(r\.count\)/);
  assert.match(app, /fmtCount\(r\.countThisYear\)/);
  assert.match(app, /label==='จำนวนทริป'\)val=fmtCount/);
});

test('user monetary values do not hard-code the baht sign', () => {
  assert.doesNotMatch(app, /พบยอด ฿/);
  assert.doesNotMatch(app, /value:'฿'\+fmt\(toDisplay/);
  assert.doesNotMatch(app, /freport-big">฿\$\{fmt\(toDisplay/);
});

test('Firebase config and offline cache share the build version', () => {
  assert.match(app, /import\(`\.\/firebase-config\.js\?v=\$\{APP_VERSION\}`\)/);
  for (const asset of ['styles.css?v=7.2.1', 'app.js?v=7.2.1', 'firebase-config.js?v=7.2.1']) {
    assert.ok(sw.includes(asset), `missing cached ${asset}`);
  }
});

test('OCR canonical liters and price per liter are converted for display', () => {
  assert.match(app, /value=dispVolVal\(parsed\.liters\)/);
  assert.match(app, /value=toDisplayPricePerVol\(parsed\.pricePerLiter\)/);
  assert.match(functions, /normalize fuel volume to liters and unit price to price per liter/);
});

test('all previously missing WMO weather codes are described', () => {
  for (const code of [56, 57, 66, 67, 77, 85, 86, 96, 99]) {
    assert.match(weather, new RegExp(`\\b${code}:`), `missing WMO code ${code}`);
  }
});

test('rate limiter periodically removes expired user windows', () => {
  assert.match(functions, /requestWindows\.delete\(key\)/);
  assert.match(functions, /now - lastRateLimitSweep >= 60_000/);
});

test('compact editor previews legacy images and uses a native driver select', () => {
  assert.match(app, /function isImageAttachment/);
  assert.match(app, /class="selected-photo-preview"/);
  assert.match(app, /<select name="driver" id="driverSelect"/);
  assert.doesNotMatch(app, /list="driverOptions"/);
});

test('top bar has no visible theme toggle button', () => {
  assert.doesNotMatch(index, /<button[^>]+id="themeBtn"/);
  assert.match(index, /id="themeBtn" hidden/);
});

test('report page follows the resolved app theme', () => {
  assert.match(styles, /\.freport\{--fr-bg:#0f1115/);
  assert.match(styles, /body\.light \.freport\{--fr-bg:#eef1fb/);
});

test('fuel metrics module is cached offline and full tank control is near fuel inputs', () => {
  assert.match(sw, /\.\/modules\/fuel-metrics\.js/);
  assert.match(app, /class="field full fuel-toggle-row"/);
  assert.match(app, /calculateFuelIntervals\(entries\(\)/);
  assert.match(app, /deleteDoc\(doc\(db,'vehicles'/);
});
