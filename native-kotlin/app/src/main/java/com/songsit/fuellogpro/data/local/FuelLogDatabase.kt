package com.songsit.fuellogpro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FuelEntryEntity::class, VehicleEntity::class, ExpenseEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class FuelLogDatabase : RoomDatabase() {
    abstract fun fuelEntryDao(): FuelEntryDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile private var instance: FuelLogDatabase? = null
        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS vehicles (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        registration TEXT NOT NULL,
                        fuelType TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO vehicles(id, name, registration, fuelType, createdAt)
                    VALUES ('local-default', 'รถของฉัน', '', 'เบนซิน', 0)
                    """.trimIndent(),
                )
            }
        }
        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expenses (
                        id TEXT NOT NULL PRIMARY KEY,
                        vehicleId TEXT NOT NULL,
                        date TEXT NOT NULL,
                        category TEXT NOT NULL,
                        description TEXT NOT NULL,
                        amount REAL NOT NULL,
                        odometerKm REAL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        fun get(context: Context): FuelLogDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FuelLogDatabase::class.java,
                    "fuellog-native.db",
                ).addMigrations(migration1To2, migration2To3)
                    .build()
                    .also { instance = it }
            }
    }
}
