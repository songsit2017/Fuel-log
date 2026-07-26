package com.songsit.fuellogpro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FuelEntryEntity::class,
        VehicleEntity::class,
        ExpenseEntity::class,
        MaintenanceEntity::class,
        TripEntity::class,
        SyncConflictEntity::class,
        DeletionTombstoneEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
abstract class FuelLogDatabase : RoomDatabase() {
    abstract fun fuelEntryDao(): FuelEntryDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun tripDao(): TripDao
    abstract fun syncConflictDao(): SyncConflictDao
    abstract fun deletionTombstoneDao(): DeletionTombstoneDao

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
        private val migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS maintenance_tasks (
                        id TEXT NOT NULL PRIMARY KEY,
                        vehicleId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        nextDate TEXT,
                        nextOdometerKm REAL,
                        warningDays INTEGER NOT NULL,
                        warningOdometerKm REAL NOT NULL,
                        repeatMonths INTEGER,
                        repeatOdometerKm REAL,
                        provider TEXT NOT NULL,
                        referenceNumber TEXT NOT NULL,
                        note TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }
        private val migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN income INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE expenses ADD COLUMN recurring INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE expenses ADD COLUMN reminderDate TEXT")
            }
        }
        private val migration5To6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trips (
                        id TEXT NOT NULL PRIMARY KEY,
                        vehicleId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        date TEXT NOT NULL,
                        distanceKm REAL NOT NULL,
                        fuelCost REAL NOT NULL,
                        tollCost REAL NOT NULL,
                        parkingCost REAL NOT NULL,
                        foodCost REAL NOT NULL,
                        otherCost REAL NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }
        private val migration6To7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_conflicts (
                        `key` TEXT NOT NULL PRIMARY KEY,
                        vehicleId TEXT NOT NULL,
                        collectionName TEXT NOT NULL,
                        recordId TEXT NOT NULL,
                        detectedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }
        private val migration7To8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS deletion_tombstones (
                        `key` TEXT NOT NULL PRIMARY KEY,
                        vehicleId TEXT NOT NULL,
                        collectionName TEXT NOT NULL,
                        recordId TEXT NOT NULL,
                        deletedAt INTEGER NOT NULL
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
                ).addMigrations(
                    migration1To2,
                    migration2To3,
                    migration3To4,
                    migration4To5,
                    migration5To6,
                    migration6To7,
                    migration7To8,
                )
                    .build()
                    .also { instance = it }
            }
    }
}
