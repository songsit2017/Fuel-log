package com.songsit.fuellogpro.ui.pro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.songsit.fuellogpro.R

enum class ProMoreDestination { EXPENSES, MAINTENANCE, VEHICLES, CALCULATOR, MAP, SETTINGS }

/**
 * The mockup's "More" hub — a tile grid fanning out to the sections that don't have their own
 * bottom-nav slot in the simplified Pro shell (Expenses/Maintenance/Vehicles/Map/Settings). Pure
 * navigation surface: each tile just reports which destination was tapped, ProAppShell owns what
 * screen actually renders (the existing ExpenseList/MaintenanceList/VehiclesListScreen/
 * NearbyStationsMapScreen/SettingsScreen — reused as-is, not rebuilt).
 *
 * Tiles alternate between the two brand accents (primary/secondary) as a soft duo-tone icon tint
 * instead of a six-color rainbow — the card itself stays neutral surface + outline either way.
 */
@Composable
fun ProMoreScreen(onSelect: (ProMoreDestination) -> Unit, modifier: Modifier = Modifier) {
    val tiles = listOf(
        Triple(ProMoreDestination.EXPENSES, stringResource(R.string.more_hub_expenses), Icons.Filled.ReceiptLong),
        Triple(ProMoreDestination.MAINTENANCE, stringResource(R.string.more_hub_maintenance), Icons.Filled.Build),
        Triple(ProMoreDestination.VEHICLES, stringResource(R.string.more_hub_vehicles), Icons.Filled.DirectionsCar),
        Triple(ProMoreDestination.CALCULATOR, stringResource(R.string.nav_calculator), Icons.Filled.Calculate),
        Triple(ProMoreDestination.MAP, stringResource(R.string.more_hub_map), Icons.Filled.Map),
        Triple(ProMoreDestination.SETTINGS, stringResource(R.string.more_hub_settings), Icons.Filled.Settings),
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(tiles, key = { it.first.name }) { (destination, label, icon) ->
            val useSecondary = tiles.indexOfFirst { it.first == destination } % 2 == 1
            val accent = if (useSecondary) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            MoreTile(label, icon, accent) { onSelect(destination) }
        }
    }
}

@Composable
private fun MoreTile(label: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
