package com.songsit.fuellogpro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_conflicts")
data class SyncConflictEntity(
    @PrimaryKey val key: String,
    val vehicleId: String,
    val collectionName: String,
    val recordId: String,
    val detectedAt: Long,
)
