package com.songsit.fuellogpro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deletion_tombstones")
data class DeletionTombstoneEntity(
    @PrimaryKey val key: String,
    val vehicleId: String,
    val collectionName: String,
    val recordId: String,
    val deletedAt: Long,
)

fun deletionTombstoneKey(collectionName: String, vehicleId: String, recordId: String): String =
    "$collectionName:$vehicleId:$recordId"
