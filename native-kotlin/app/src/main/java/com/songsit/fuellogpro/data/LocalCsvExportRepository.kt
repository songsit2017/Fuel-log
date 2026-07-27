package com.songsit.fuellogpro.data

import androidx.room.withTransaction
import com.songsit.fuellogpro.data.local.FuelLogDatabase
import java.util.Locale

class LocalCsvExportRepository(
    private val database: FuelLogDatabase,
) {
    suspend fun exportCsv(): String = database.withTransaction {
        val vehicleNames = database.vehicleDao().getAll().associate { it.id to it.name }
        val rows = mutableListOf<List<Any?>>()
        rows += listOf(
            "type", "vehicleId", "vehicleName", "date", "time", "odometerKm",
            "category", "description", "liters", "pricePerLiter", "amount", "income",
            "distanceKm", "fuelCost", "tollCost", "parkingCost", "foodCost", "otherCost",
            "nextDate", "nextOdometerKm",
        )
        database.fuelEntryDao().getAll().forEach { item ->
            rows += listOf(
                "fuel", item.vehicleId, vehicleNames[item.vehicleId], item.date, item.time,
                item.odometerKm, "fuel", item.station, item.liters, item.pricePerLiter,
                item.amount, false, null, null, null, null, null, null, null, null,
            )
        }
        database.expenseDao().getAll().forEach { item ->
            rows += listOf(
                "expense", item.vehicleId, vehicleNames[item.vehicleId], item.date, null,
                item.odometerKm, item.category, item.description, null, null, item.amount,
                item.income, null, null, null, null, null, null, item.reminderDate, null,
            )
        }
        database.maintenanceDao().getAll().forEach { item ->
            rows += listOf(
                "maintenance", item.vehicleId, vehicleNames[item.vehicleId], item.nextDate,
                null, item.nextOdometerKm, item.category, item.name, null, null, null,
                false, null, null, null, null, null, null, item.nextDate, item.nextOdometerKm,
            )
        }
        database.tripDao().getAll().forEach { item ->
            rows += listOf(
                "trip", item.vehicleId, vehicleNames[item.vehicleId], item.date, null, null,
                "trip", item.name, null, null, item.fuelCost + item.tollCost +
                    item.parkingCost + item.foodCost + item.otherCost,
                false, item.distanceKm, item.fuelCost, item.tollCost, item.parkingCost,
                item.foodCost, item.otherCost, null, null,
            )
        }
        "\uFEFF" + rows.joinToString("\r\n") { row -> row.joinToString(",") { csvCell(it) } }
    }
}

private fun csvCell(value: Any?): String {
    val raw = when (value) {
        null -> ""
        is Double -> String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
        else -> value.toString()
    }
    val first = raw.firstOrNull()
    val safe = if (first != null && first in setOf('=', '+', '-', '@')) "'$raw" else raw
    return "\"${safe.replace("\"", "\"\"")}\""
}
