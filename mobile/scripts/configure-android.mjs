import { readFile, writeFile, access } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const here=dirname(fileURLToPath(import.meta.url));
const mobile=resolve(here,'..');
const gradleFile=resolve(mobile,'android','app','build.gradle');
const variablesFile=resolve(mobile,'android','variables.gradle');
await access(gradleFile);

const versionName=process.env.FUELLOG_VERSION_NAME||'8.0.0-beta.1';
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
console.log(`Android configured: versionName=${versionName}, versionCode=${versionCode}, signed=${!!process.env.FUELLOG_KEYSTORE_FILE}`);
