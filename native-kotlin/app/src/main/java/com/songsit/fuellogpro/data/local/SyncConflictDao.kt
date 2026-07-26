package com.songsit.fuellogpro.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncConflictDao {
    @Query("SELECT * FROM sync_conflicts ORDER BY detectedAt DESC")
    fun observeAll(): Flow<List<SyncConflictEntity>>

    @Query("SELECT * FROM sync_conflicts WHERE `key` = :key")
    suspend fun getByKey(key: String): SyncConflictEntity?

    @Upsert
    suspend fun upsert(conflict: SyncConflictEntity)

    @Query("DELETE FROM sync_conflicts WHERE `key` = :key")
    suspend fun deleteByKey(key: String)
}
