import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');
const sw = await readFile(new URL('../sw.js', import.meta.url), 'utf8');
const weather = await readFile(new URL('../modules/weather.js', import.meta.url), 'utf8');
const functions = await readFile(new URL('../functions/index.js', import.meta.url), 'utf8');
const index = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const styles = await readFile(new URL('../styles.css', import.meta.url), 'utf8');
const androidWorkflow = await readFile(new URL('../.github/workflows/build-android.yml', import.meta.url), 'utf8');
const androidConfigScript = await readFile(new URL('../mobile/scripts/configure-android.mjs', import.meta.url), 'utf8');
const mobilePackage = await readFile(new URL('../mobile/package.json', import.meta.url), 'utf8');
const capacitorConfig = await readFile(new URL('../mobile/capacitor.config.json', import.meta.url), 'utf8');
const nativeVehiclesViewModel = await readFile(new URL('../native-kotlin/app/src/main/java/com/songsit/fuellogpro/ui/vehicles/VehiclesViewModel.kt', import.meta.url), 'utf8');
const nativeVehicleRepository = await readFile(new URL('../native-kotlin/app/src/main/java/com/songsit/fuellogpro/data/VehicleRepository.kt', import.meta.url), 'utf8');
const kotlinPolicy = await readFile(new URL('../native-kotlin/KOTLIN-POLICY.md', import.meta.url), 'utf8');
const firestoreRules = await readFile(new URL('../firestore.rules', import.meta.url), 'utf8');
const firebaseJson = await readFile(new URL('../firebase.json', import.meta.url), 'utf8');
const nativeBuild = await readFile(new URL('../native-kotlin/app/build.gradle.kts', import.meta.url), 'utf8');
const nativeMain = await readFile(new URL('../native-kotlin/app/src/main/java/com/songsit/fuellogpro/MainActivity.kt', import.meta.url), 'utf8');
const nativeAuth = await readFile(new URL('../native-kotlin/app/src/main/java/com/songsit/fuellogpro/auth/GoogleAuthRepository.kt', import.meta.url), 'utf8');

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
  assert.match(sw, /const VERSION = '7\.8\.5'/);
  for (const asset of ['styles.css','app.js','firebase-config.js'])
    assert.ok(sw.includes('`./'+asset+'?v=${VERSION}`'),`missing versioned cached ${asset}`);
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

test('primary record pages use a mobile app shell and floating add actions', () => {
  assert.equal((index.match(/class="primary small app-fab"/g)||[]).length,3);
  assert.match(index, /class="page native-list-page" data-page="fuel"/);
  assert.match(styles, /\/\* V7\.8 mobile app shell \*\//);
  assert.match(styles, /\.app-fab\{[^}]*position:fixed/);
  assert.match(styles, /\.native-list-page \.record\{[^}]*border-radius:0/);
  assert.match(styles, /\.bottom-nav button\.active::before/);
});

test('fuel records render recognizable offline station brand pictograms', () => {
  for (const brand of ['ptt','bangchak','shell','esso','caltex','pt','susco','pure']) {
    assert.match(app, new RegExp(`${brand}:\\x60<svg|key:'${brand}'`), `missing ${brand} brand icon`);
  }
  assert.match(app, /BRAND_ICONS\[brand\.key\]/);
  assert.match(styles, /\.record \.ico\.brand-badge svg/);
  assert.doesNotMatch(app, /<img[^>]+(?:logo|brand)/i);
});

test('generic fuel pump icon is a consistent vector rather than a colorful emoji', () => {
  assert.match(app, /const FUEL_PUMP_ICON=`<svg class="fuel-pump-icon"/);
  assert.match(app, /generic-fuel-badge/);
  assert.match(index, /data-nav="fuel"><span><svg class="fuel-pump-icon"/);
  assert.doesNotMatch(index, /data-nav="fuel"><span>⛽/);
  assert.match(styles, /\.fuel-pump-icon\{[^}]*stroke:currentColor/);
});

test('Android installed PWA actively replaces stale application caches', () => {
  assert.match(sw, /const VERSION = '7\.8\.5'/);
  assert.match(sw, /cache:'reload'/);
  assert.match(sw, /cache:'no-store'/);
  assert.match(sw, /self\.skipWaiting\(\)/);
  assert.match(sw, /self\.clients\.claim\(\)/);
  assert.match(app, /updateViaCache:'none'/);
  assert.match(app, /controllerchange/);
  assert.match(app, /function refreshInstalledApp\(\)/);
  assert.match(app, /CLEAR_APP_CACHE/);
});

test('GitHub builds downloadable signed APK and future Play Store AAB safely', () => {
  assert.match(androidWorkflow, /branches:\s*\n\s*- main/);
  assert.match(androidWorkflow, /working-directory: mobile/);
  assert.match(androidWorkflow, /\.\/gradlew assembleRelease bundleRelease/);
  assert.match(androidWorkflow, /npx cap add android/);
  assert.match(androidWorkflow, /APK_KEYSTORE_BASE64/);
  assert.match(androidWorkflow, /gh release create/);
  assert.match(androidWorkflow, /--generate-notes/);
  assert.match(androidWorkflow, /FuelLog-Pro-\*\.apk/);
  assert.match(androidWorkflow, /actions\/upload-artifact@v4/);
  assert.doesNotMatch(androidWorkflow, /cache:\s*gradle/);
  assert.doesNotMatch(androidWorkflow, /storePassword\s+["'][^$]/);
  assert.match(androidConfigScript, /System\.getenv\("FUELLOG_KEYSTORE_PASSWORD"\)/);
  assert.match(androidConfigScript, /versionCode/);
  assert.match(androidConfigScript, /versionName/);
  assert.match(androidConfigScript, /fuellog_icon/);
  assert.match(androidConfigScript, /android:icon="@mipmap\/fuellog_launcher"/);
  assert.match(androidConfigScript, /android:roundIcon="@mipmap\/fuellog_launcher_round"/);
  assert.match(androidWorkflow, /format\('8\.0\.0-beta\.\{0\}', github\.run_number\)/);
  assert.match(androidWorkflow, /FuelLog-Pro-\$\{FUELLOG_VERSION_NAME\}\.apk/);
});

test('Android APK uses native Google authentication without a browser redirect', () => {
  assert.match(mobilePackage, /"@capacitor-firebase\/authentication": "7\.3\.0"/);
  assert.match(capacitorConfig, /"skipNativeAuth": true/);
  assert.match(capacitorConfig, /"google\.com"/);
  assert.match(app, /nativeAuth\.signInWithGoogle/);
  assert.match(app, /signInWithCredential\(auth,GoogleAuthProvider\.credential/);
  assert.match(nativeAuth, /CredentialManager/);
  assert.match(nativeAuth, /GoogleAuthProvider\.getCredential/);
  assert.match(androidWorkflow, /FIREBASE_ANDROID_CONFIG_BASE64/);
  assert.match(androidWorkflow, /android\/app\/google-services\.json/);
  assert.match(androidWorkflow, /SHA1:\|SHA256:/);
});

test('vehicle sync discovers Cloud vehicles before creating or uploading a local placeholder', () => {
  assert.match(app, /async function discoverCloudVehicles\(\)/);
  assert.match(app, /where\('ownerUid','==',user\.uid\)/);
  assert.match(app, /await pullVehicleById\(cloudVehicle\.id\)/);
  assert.match(app, /if\(cloudVehicles\.length\)/);
  assert.match(app, /memberUids:\[user\.uid\]/);
  assert.match(firestoreRules, /resource\.data\.ownerUid == request\.auth\.uid/);
  assert.match(firestoreRules, /request\.auth\.uid in resource\.data\.memberUids/);
  assert.match(firebaseJson, /"rules": "firestore\.rules"/);
});

test('first Google session restores Cloud vehicles automatically', () => {
  assert.match(app, /await autoRestoreCloudVehicles\(\)/);
  assert.match(app, /const autoRestoredUsers = new Set\(\)/);
  assert.match(app, /await restoreDiscoveredCloudVehicles\(cloudVehicles\)/);
});

test('parallel Kotlin migration starts with repository and ViewModel boundaries', () => {
  assert.match(nativeVehicleRepository, /interface VehicleRepository/);
  assert.match(nativeVehicleRepository, /suspend fun restoreOwnedVehicles/);
  assert.match(nativeVehiclesViewModel, /class VehiclesViewModel/);
  assert.match(nativeVehiclesViewModel, /fun onSignedIn\(uid: String\)/);
  assert.match(nativeVehiclesViewModel, /StateFlow<VehiclesUiState>/);
  assert.match(kotlinPolicy, /Kotlin-first and Kotlin-only/);
  assert.match(kotlinPolicy, /Do not add application-owned `\.java` files/);
  assert.match(nativeBuild, /applicationId = "com\.songsit\.fuellogpro"/);
  assert.match(nativeBuild, /firebase-bom:34\.16\.0/);
  assert.match(nativeMain, /class MainActivity : ComponentActivity/);
  assert.match(nativeAuth, /CredentialManager/);
  assert.match(nativeAuth, /GoogleAuthProvider\.getCredential/);
});

test('home has one vehicle selector', () => {
  assert.doesNotMatch(index, /id="vehicleStrip"/);
  assert.equal((index.match(/id="globalVehicleSelect"/g)||[]).length,1);
});

test('vehicle management platform covers documents, service parts and travel costs', () => {
  for (const template of ['tax','act','insurance','engine-oil','tire-rotation','battery'])
    assert.match(app, new RegExp(`key:'${template}'`), `missing reminder template ${template}`);
  for (const category of ['ค่าเดินทาง','ค่าโรงแรม','ค่าทางด่วน'])
    assert.match(app, new RegExp(category), `missing expense category ${category}`);
  assert.match(app, /warningDays=Number\(r\.warningDays\)\|\|30/);
  assert.match(app, /warningOdo=Number\(r\.warningOdo\)\|\|1000/);
  assert.match(app, /data-reminder-template/);
  assert.match(app, /policyNumber/);
});

test('fuel metrics module is cached offline and full tank control is near fuel inputs', () => {
  assert.match(sw, /\.\/modules\/fuel-metrics\.js/);
  assert.match(app, /class="field full fuel-toggle-row"/);
  assert.match(app, /calculateFuelIntervals\(entries\(\)/);
  assert.match(app, /deleteDoc\(doc\(db,'vehicles'/);
});
