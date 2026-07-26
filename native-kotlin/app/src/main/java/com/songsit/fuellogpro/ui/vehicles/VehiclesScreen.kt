package com.songsit.fuellogpro.ui.vehicles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VehiclesScreen(
    state: VehiclesUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("ยานพาหนะ", style = MaterialTheme.typography.headlineMedium)
        if (state.loading) CircularProgressIndicator()
        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (!state.loading && state.vehicles.isEmpty()) {
            Text("ยังไม่พบรถในบัญชีนี้")
        }
        LazyColumn {
            items(state.vehicles, key = { it.id }) { vehicle ->
                ListItem(
                    headlineContent = { Text(vehicle.name) },
                    supportingContent = { Text(vehicle.registration) },
                )
            }
        }
    }
}
