package com.songsit.fuellogpro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FuelEntryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class FuelLogDatabase : RoomDatabase() {
    abstract fun fuelEntryDao(): FuelEntryDao

    companion object {
        @Volatile private var instance: FuelLogDatabase? = null

        fun get(context: Context): FuelLogDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FuelLogDatabase::class.java,
                    "fuellog-native.db",
                ).build().also { instance = it }
            }
    }
}
