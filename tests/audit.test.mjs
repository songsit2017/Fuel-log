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
  for (const asset of ['styles.css?v=7.7.0', 'app.js?v=7.7.0', 'firebase-config.js?v=7.7.0']) {
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

test('Fuelio import uploads every picture attached to a fuel record', () => {
  assert.match(app, /for\(let s=0;s<filenames\.length;s\+\+\)/);
  assert.match(app, /s===0\?'receipt':s===1\?'odometer':'attachment'/);
  assert.match(app, /existingPhotoKeys\.has\(photoKey\)/);
  assert.match(app, /existingPhotoKeys\.add\(photoKey\)/);
  assert.doesNotMatch(app, /Math\.min\(filenames\.length,slots\.length\)/);
});

test('Fuelio expenses retain details, pictures and update matching records', () => {
  for (const field of ['time:extractFuelioTime', 'income:', 'bookmarked:', 'recurrence:', 'reminderDate:']) {
    assert.ok(app.includes(field), `missing imported expense field ${field}`);
  }
  assert.match(app, /findMatchingExpense\(targetVehicleId,cost\)/);
  assert.match(app, /uploadImportedPhoto\(targetVehicleId,targetId,'attachment',blob,filename,'expense'\)/);
  assert.match(app, /loadExistingLogPhotos\(obj\.id,'expense'\)/);
  assert.match(app, /toLowerCase\(\)\.replace\(\/\[_\\s-\]\/g,''\)/);
  assert.match(app, /\['costtypeid','categoryid','id'\]\.includes\(h\)/);
  assert.doesNotMatch(app, /if\(\(cols\[tIdx\]\|\|''\)\.trim\(\)==='1'\)/);
});

test('vehicle selector is global and station explorer menu is removed', () => {
  assert.match(index, /id="globalVehicleSelect"/);
  assert.doesNotMatch(index, /data-panel="stations"/);
  assert.doesNotMatch(index, /leaflet@/);
  assert.match(app, /function switchVehicle\(vehicleId\)/);
  assert.match(app, /globalVehicleSelect.*addEventListener\('change'/);
});

test('today oil prices use a compact brand comparison table', () => {
  assert.match(app, /function oilComparisonTable\(brands\)/);
  assert.match(app, /class="oil-compare"/);
  for (const brand of ['บางจาก','PTT','Shell']) assert.match(app, new RegExp(`short:'${brand}'`));
  assert.match(styles, /\.oil-compare-row\{[^}]*grid-template-columns/);
});

test('fuel records render recognizable offline station brand pictograms', () => {
  for (const brand of ['ptt','bangchak','shell','esso','caltex','pt','susco','pure']) {
    assert.match(app, new RegExp(`${brand}:\\x60<svg|key:'${brand}'`), `missing ${brand} brand icon`);
  }
  assert.match(app, /BRAND_ICONS\[brand\.key\]/);
  assert.match(styles, /\.record \.ico\.brand-badge svg/);
  assert.doesNotMatch(app, /<img[^>]+(?:logo|brand)/i);
});

test('home has one vehicle selector', () => {
  assert.doesNotMatch(index, /id="vehicleStrip"/);
  assert.equal((index.match(/id="globalVehicleSelect"/g)||[]).length,1);
});

test('fuel metrics module is cached offline and full tank control is near fuel inputs', () => {
  assert.match(sw, /\.\/modules\/fuel-metrics\.js/);
  assert.match(app, /class="field full fuel-toggle-row"/);
  assert.match(app, /calculateFuelIntervals\(entries\(\)/);
  assert.match(app, /deleteDoc\(doc\(db,'vehicles'/);
});
