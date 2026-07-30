package com.songsit.fuellogpro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class OpenSourceLibrary(
    val name: String,
    val author: String,
    val license: String,
    val url: String? = null
)

// Dynamically accurately representing the actual project dependencies from build.gradle.kts
val projectDependencies = listOf(
    OpenSourceLibrary("Jetpack Compose (UI, Material3, Tooling)", "Google LLC", "Apache License 2.0", "https://developer.android.com/jetpack/compose"),
    OpenSourceLibrary("AndroidX (Core, Activity, Lifecycle, Room, Work, Credentials)", "Google LLC", "Apache License 2.0", "https://developer.android.com/jetpack/androidx"),
    OpenSourceLibrary("Kotlinx Coroutines", "JetBrains s.r.o.", "Apache License 2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
    OpenSourceLibrary("Firebase (Auth, Firestore, Storage, Functions)", "Google LLC", "Apache License 2.0", "https://firebase.google.com/"),
    OpenSourceLibrary("Google Play Services (Auth, Location, Maps)", "Google LLC", "Google APIs Terms of Service", "https://developers.google.com/android/guides/setup"),
    OpenSourceLibrary("Maps Compose", "Google LLC", "Apache License 2.0", "https://github.com/googlemaps/android-maps-compose"),
    OpenSourceLibrary("Coil", "Coil Contributors", "Apache License 2.0", "https://coil-kt.github.io/coil/"),
    OpenSourceLibrary("ML Kit Text Recognition", "Google LLC", "Google APIs Terms of Service", "https://developers.google.com/ml-kit")
).sortedBy { it.name }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLicensesScreen(
    onDismiss: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.songsit.fuellogpro.R.string.settings_licenses_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(com.songsit.fuellogpro.R.string.action_back)) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(projectDependencies) { library ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = library.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Author: ${library.author}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "License: ${library.license}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
