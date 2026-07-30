package com.songsit.fuellogpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.songsit.fuellogpro.data.NearbyStation
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationSelectionScreen(
    onDismiss: () -> Unit,
    onStationSelected: (String) -> Unit,
    nearbyStations: List<NearbyStation>,
    stationVisitCounts: Map<String, Int>,
    isSearching: Boolean,
    onRunSearch: () -> Unit,
    errorMessage: String?,
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = stringArrayResource(com.songsit.fuellogpro.R.array.station_selection_tabs)

    LaunchedEffect(Unit) {
        if (nearbyStations.isEmpty() && !isSearching) {
            onRunSearch()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(com.songsit.fuellogpro.R.string.action_back))
                    }
                },
                title = {
                    Text(stringResource(com.songsit.fuellogpro.R.string.station_selection_title), fontWeight = FontWeight.SemiBold)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    // Nearby Stations
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            ListItem(
                                headlineContent = { Text(stringResource(com.songsit.fuellogpro.R.string.station_use_current_location)) },
                                supportingContent = { Text(stringResource(com.songsit.fuellogpro.R.string.station_use_gps_coordinates), style = MaterialTheme.typography.bodyMedium) },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                modifier = Modifier.clickable {
                                    onStationSelected("")
                                    onDismiss()
                                }
                            )
                            HorizontalDivider()
                        }

                        if (isSearching) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        } else if (errorMessage != null) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        } else {
                            item {
                                Text(
                                    text = stringResource(com.songsit.fuellogpro.R.string.section_nearby_stations),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                            items(nearbyStations.sortedBy { it.distanceMeters }) { station ->
                                val visits = stationVisitCounts[station.name] ?: 0
                                ListItem(
                                    headlineContent = { Text(station.name, fontWeight = FontWeight.SemiBold) },
                                    supportingContent = {
                                        Column {
                                            Text("${(station.distanceMeters / 1000).let { "%.0f".format(Locale.US, it) }}m")
                                            if (visits > 0) {
                                                Surface(
                                                    shape = MaterialTheme.shapes.small,
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(com.songsit.fuellogpro.R.string.station_your_visits, visits),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    leadingContent = {
                                        StationBadge(stationName = station.name, size = 48.dp)
                                    },
                                    modifier = Modifier.clickable {
                                        onStationSelected(station.name)
                                        onDismiss()
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
                1 -> {
                    // Favorites (Placeholder)
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(com.songsit.fuellogpro.R.string.station_favorites_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                2 -> {
                    // Map (Placeholder)
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(com.songsit.fuellogpro.R.string.station_map_coming_soon), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
