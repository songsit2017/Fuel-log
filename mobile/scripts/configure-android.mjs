import { readFile, writeFile, access, cp, mkdir } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const here=dirname(fileURLToPath(import.meta.url));
const mobile=resolve(here,'..');
const gradleFile=resolve(mobile,'android','app','build.gradle');
const variablesFile=resolve(mobile,'android','variables.gradle');
const manifestFile=resolve(mobile,'android','app','src','main','AndroidManifest.xml');
const resources=resolve(mobile,'android','app','src','main','res');
await access(gradleFile);

const versionName=process.env.FUELLOG_VERSION_NAME||'8.0.0-beta.3';
const versionCode=Math.max(1,Number.parseInt(process.env.FUELLOG_VERSION_CODE||'1',10)||1);
let gradle=await readFile(gradleFile,'utf8');

gradle=gradle
  .replace(/versionCode\s+\d+/,`versionCode ${versionCode}`)
  .replace(/versionName\s+"[^"]+"/,`versionName "${versionName}"`);

if(process.env.FUELLOG_KEYSTORE_FILE){
  const signing=`
    signingConfigs {
        release {
            storeFile file(System.getenv("FUELLOG_KEYSTORE_FILE"))
            storePassword System.getenv("FUELLOG_KEYSTORE_PASSWORD")
            keyAlias System.getenv("FUELLOG_KEY_ALIAS")
            keyPassword System.getenv("FUELLOG_KEY_PASSWORD")
        }
    }
`;
  if(!gradle.includes('signingConfigs {'))gradle=gradle.replace(/(\n\s*buildTypes\s*\{)/,`${signing}$1`);
  gradle=gradle.replace(/(buildTypes\s*\{\s*release\s*\{)/,'$1\n            signingConfig signingConfigs.release');
}

await writeFile(gradleFile,gradle,'utf8');

let variables=await readFile(variablesFile,'utf8');
if(!variables.includes('rgcfaIncludeGoogle')){
  variables=variables.replace(
    /ext\s*\{/,
    `ext {\n    rgcfaIncludeGoogle = true\n    androidxCredentialsVersion = '1.3.0'`
  );
  await writeFile(variablesFile,variables,'utf8');
}

const sourceIcon=resolve(mobile,'www','icon-512.png');
const drawable=resolve(resources,'drawable');
const drawableNoDpi=resolve(resources,'drawable-nodpi');
const adaptiveIcons=resolve(resources,'mipmap-anydpi-v26');
await Promise.all([
  mkdir(drawable,{recursive:true}),
  mkdir(drawableNoDpi,{recursive:true}),
  mkdir(adaptiveIcons,{recursive:true}),
]);
await cp(sourceIcon,resolve(drawableNoDpi,'fuellog_icon.png'));
await writeFile(resolve(drawable,'fuellog_launcher_foreground.xml'),`<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:drawable="@drawable/fuellog_icon"
    android:inset="10%" />
`,'utf8');
const adaptiveIcon=`<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@android:color/black" />
    <foreground android:drawable="@drawable/fuellog_launcher_foreground" />
</adaptive-icon>
`;
await Promise.all([
  writeFile(resolve(adaptiveIcons,'fuellog_launcher.xml'),adaptiveIcon,'utf8'),
  writeFile(resolve(adaptiveIcons,'fuellog_launcher_round.xml'),adaptiveIcon,'utf8'),
  ...['mipmap-hdpi','mipmap-mdpi','mipmap-xhdpi','mipmap-xxhdpi','mipmap-xxxhdpi'].flatMap(folder => {
    const target=resolve(resources,folder);
    return [
      mkdir(target,{recursive:true}).then(()=>cp(sourceIcon,resolve(target,'fuellog_launcher.png'))),
      mkdir(target,{recursive:true}).then(()=>cp(sourceIcon,resolve(target,'fuellog_launcher_round.png'))),
    ];
  }),
]);
let manifest=await readFile(manifestFile,'utf8');
manifest=manifest
  .replace(/android:icon="@mipmap\/[^"]+"/, 'android:icon="@mipmap/fuellog_launcher"')
  .replace(/android:roundIcon="@mipmap\/[^"]+"/, 'android:roundIcon="@mipmap/fuellog_launcher_round"');
await writeFile(manifestFile,manifest,'utf8');
console.log(`Android configured: versionName=${versionName}, versionCode=${versionCode}, signed=${!!process.env.FUELLOG_KEYSTORE_FILE}`);
