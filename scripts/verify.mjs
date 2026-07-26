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
console.log(`FuelLog Pro V7 verification passed (${Object.keys(checks).length} feature groups).`);
