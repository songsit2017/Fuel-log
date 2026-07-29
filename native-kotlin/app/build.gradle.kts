import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

secrets {
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}

val secretsProps = Properties().apply {
    val defaultsFile = rootProject.file("local.defaults.properties")
    val localFile = rootProject.file("local.properties")
    if (defaultsFile.exists()) defaultsFile.inputStream().use { load(it) }
    if (localFile.exists()) localFile.inputStream().use { load(it) }
}

android {
    namespace = "com.songsit.fuellogpro"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.songsit.fuellogpro"
        minSdk = 26
        targetSdk = 37
        versionCode = providers.environmentVariable("FUELLOG_VERSION_CODE").orNull?.toIntOrNull() ?: 1
        versionName = providers.environmentVariable("FUELLOG_VERSION_NAME").orNull ?: "9.0.0-native-alpha.1"
        manifestPlaceholders["MAPS_API_KEY"] = secretsProps.getProperty("MAPS_API_KEY", "MISSING")
    }
    signingConfigs {
        create("release") {
            val keystoreFile = providers.environmentVariable("FUELLOG_KEYSTORE_FILE").orNull
            if (!keystoreFile.isNullOrBlank()) {
                storeFile = file(keystoreFile)
                storePassword = providers.environmentVariable("FUELLOG_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("FUELLOG_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("FUELLOG_KEY_PASSWORD").orNull
            }
        }
        getByName("debug") {
            val debugKeystore = file("debug.keystore")
            if (debugKeystore.exists()) {
                storeFile = debugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }
    buildTypes {
        getByName("release") {
            val keystoreFile = providers.environmentVariable("FUELLOG_KEYSTORE_FILE").orNull
            if (!keystoreFile.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    val firebaseBom = platform("com.google.firebase:firebase-bom:34.16.0")
    implementation(composeBom)
    implementation(firebaseBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Settings font picker: downloads the selected typeface at runtime via Google Play
    // Services' Downloadable Fonts (no .ttf assets bundled in the app).
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.work:work-runtime:2.11.2")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    // Calls the existing `scanReceipt` Cloud Function (functions/index.js) that already backs
    // V8's Claude OCR — the Anthropic key stays server-side as a Secret, never in the app.
    implementation("com.google.firebase:firebase-functions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    // Google Drive backup: Identity Services' Authorization API (Identity.getAuthorizationClient)
    // for incremental drive.file scope consent. The actual Drive REST calls are hand-rolled
    // HttpURLConnection (see GoogleDriveBackupRepository), matching this project's existing
    // lightweight *Repository style, so no google-api-services-drive client SDK is pulled in.
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:19.1.0")
    implementation("com.google.maps.android:maps-compose:6.4.1")
    // Reports charts (monthly cost bar, station-cost donut, odometer line) on StatsScreen.
    implementation("com.patrykandpatrick.vico:compose:3.2.3")
    implementation("com.patrykandpatrick.vico:compose-m3:3.2.3")
    implementation("androidx.core:core-ktx:1.15.0")
    // Vehicle photo loading (VehicleListScreen/VehicleEditScreen background images from imageUri).
    implementation("io.coil-kt:coil-compose:2.7.0")
    // Item 3 (receipt OCR): on-device text recognition to pre-fill the expense amount field,
    // mirroring V8's OCR auto-fill (Tesseract/Claude OCR, app.js recognizeReceipt()).
    implementation("com.google.mlkit:text-recognition:16.0.1")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

