import { readFile, access } from 'node:fs/promises';

const requiredFiles = [
  'index.html', 'app.js', 'styles.css', 'firebase-config.js', 'firestore.rules',
  'storage.rules', 'oil-prices.json', 'modules/settings.js', 'modules/weather.js',
  'modules/ocr-client.js', 'functions/index.js'
];
await Promise.all(requiredFiles.map(file => access(new URL(`../${file}`, import.meta.url))));

const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');
const checks = {
  googleLogin: ['GoogleAuthProvider', 'signInWithPopup', 'signInWithRedirect'],
  familySharing: ['ensureCloudVehicle', 'invite', 'join', 'loadMembers'],
  fuelioImport: ['parseFuelioCSV', 'importFuelioArchive', 'applyFuelioVehicle'],
  ocr: ['scanReceiptWithClaude', 'loadTesseract', 'scanWithSecureBackend'],
  oilPrices: ['loadTodayPrices', 'oil-prices.json'],
  sync: ['syncVehicle', 'pullVehicle', 'writeBatch'],
  export: ['exportJSON', 'exportCSV'],
  weather: ['captureWeather', 'weatherEnabled'],
  settings: ['currency', 'decimals', 'themeMode', 'autoOcrEnabled']
};
const failures = Object.entries(checks)
  .filter(([, needles]) => needles.some(needle => !app.includes(needle)))
  .map(([name]) => name);
if (failures.length) throw new Error(`Regression checks failed: ${failures.join(', ')}`);

const forbidden = ['anthropic-dangerous-direct-browser-access', "fetch('https://api.anthropic.com", 'firebase-dev'];
const found = forbidden.filter(needle => app.includes(needle));
if (found.length) throw new Error(`Forbidden client/branch references: ${found.join(', ')}`);
for (const required of ['loadExistingLogPhotos', 'data.photos=[...(oldRecord?.photos||[])', "unavailableGradeRow('ปตท.'", "unavailableGradeRow('เชลล์'"]) {
  if (!app.includes(required)) throw new Error(`V7.0.1 regression check failed: ${required}`);
}
for (const legacyPhotoKey of ['photo.entryId','photo.targetId','photo.target_id','/fuel/${alias}/']) {
  if (!app.includes(legacyPhotoKey)) throw new Error(`V7.0.2 legacy photo check failed: ${legacyPhotoKey}`);
}
for (const required of ['data-page="panel"', 'findMatchingFuelEntry', 'mergeFuelioEntry', 'loadFamilyDriverOptions', 'driverOptions']) {
  const source = required === 'data-page="panel"' ? await readFile(new URL('../index.html', import.meta.url), 'utf8') : app;
  if (!source.includes(required)) throw new Error(`V7.1.0 feature check failed: ${required}`);
}
for (const auditNeedle of ['fmtCount(r.count)', 'firebase-config.js?v=${APP_VERSION}', 'dispVolVal(parsed.liters)', 'requestWindows.delete(key)']) {
  const source = auditNeedle === 'requestWindows.delete(key)'
    ? await readFile(new URL('../functions/index.js', import.meta.url), 'utf8')
    : app;
  if (!source.includes(auditNeedle)) throw new Error(`V7.1.1 audit check failed: ${auditNeedle}`);
}
if (app.includes("$('#panelDialog')") || app.includes('.showModal();}\nfunction renderPanel')) {
  throw new Error('V7.1.0 full-page navigation regressed to a panel dialog');
}
console.log(`FuelLog Pro V7 verification passed (${Object.keys(checks).length} feature groups).`);
