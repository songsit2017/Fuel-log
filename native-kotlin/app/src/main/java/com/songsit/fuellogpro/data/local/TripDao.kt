package com.songsit.fuellogpro.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips WHERE vehicleId = :vehicleId ORDER BY date DESC, createdAt DESC")
    fun observeForVehicle(vehicleId: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips")
    suspend fun getAll(): List<TripEntity>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: String): TripEntity?

    @Upsert
    suspend fun upsert(trip: TripEntity)

    @Upsert
    suspend fun upsertAll(trips: List<TripEntity>)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM trips WHERE vehicleId = :vehicleId")
    suspend fun deleteForVehicle(vehicleId: String)
}
