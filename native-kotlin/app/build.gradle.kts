plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
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
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
