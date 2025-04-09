package com.example.assignment2.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.assignment2.data.model.Flight
import com.example.assignment2.data.model.FlightTime
import com.example.assignment2.data.model.StatusChangeRecord

@Database(
    entities = [Flight::class, FlightTime::class, StatusChangeRecord::class],
    version = 4,
    exportSchema = false
)
abstract class FlightDatabase : RoomDatabase() {
    abstract fun flightDao(): FlightDao
    abstract fun flightTimeDao(): FlightTimeDao
    abstract fun statusChangeDao(): StatusChangeDao

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Check if the old table has the expected columns
                val columnsQuery = "PRAGMA table_info(flights)"
                val cursor = database.query(columnsQuery)
                val columnNames = mutableListOf<String>()
                
                if (cursor.moveToFirst()) {
                    do {
                        val columnName = cursor.getString(cursor.getColumnIndex("name"))
                        columnNames.add(columnName)
                    } while (cursor.moveToNext())
                }
                cursor.close()
                
                // Create a new table that matches the current Flight model
                database.execSQL(
                    """
                    CREATE TABLE flights_new (
                        flightNumber TEXT NOT NULL PRIMARY KEY,
                        airline TEXT NOT NULL,
                        departureAirport TEXT NOT NULL,
                        arrivalAirport TEXT NOT NULL,
                        scheduledDeparture TEXT NOT NULL,
                        scheduledArrival TEXT NOT NULL,
                        actualDeparture TEXT,
                        actualArrival TEXT,
                        status TEXT NOT NULL,
                        delay INTEGER,
                        averageTime INTEGER,
                        latitude REAL,
                        longitude REAL,
                        altitude INTEGER,
                        direction INTEGER,
                        speed INTEGER,
                        lastUpdated TEXT
                    )
                    """
                )
                
                // Build the SELECT part of the query based on available columns
                val selectPart = StringBuilder("""
                    SELECT 
                        flightNumber, airline, departureAirport, arrivalAirport,
                        scheduledDeparture, scheduledArrival, actualDeparture,
                        actualArrival, status, delay, 
                        NULL -- averageTime
                """)
                
                // Add latitude if it exists
                if (columnNames.contains("latitude")) {
                    selectPart.append(", latitude")
                } else {
                    selectPart.append(", NULL")
                }
                
                // Add longitude if it exists
                if (columnNames.contains("longitude")) {
                    selectPart.append(", longitude")
                } else {
                    selectPart.append(", NULL")
                }
                
                // Add altitude if it exists
                if (columnNames.contains("altitude")) {
                    selectPart.append(", CAST(altitude AS INTEGER)")
                } else {
                    selectPart.append(", NULL")
                }
                
                // Add direction if it exists
                if (columnNames.contains("direction")) {
                    selectPart.append(", CAST(direction AS INTEGER)")
                } else {
                    selectPart.append(", NULL")
                }
                
                // Add speed from speedHorizontal if it exists
                if (columnNames.contains("speedHorizontal")) {
                    selectPart.append(", CAST(speedHorizontal AS INTEGER)")
                } else {
                    selectPart.append(", NULL")
                }
                
                // Add lastUpdated if it exists
                if (columnNames.contains("lastUpdated")) {
                    selectPart.append(", lastUpdated")
                } else {
                    selectPart.append(", NULL")
                }
                
                // Copy data from old table to new table, handling field name changes
                database.execSQL(
                    """
                    INSERT OR REPLACE INTO flights_new (
                        flightNumber, airline, departureAirport, arrivalAirport,
                        scheduledDeparture, scheduledArrival, actualDeparture,
                        actualArrival, status, delay, averageTime, latitude,
                        longitude, altitude, direction, speed, lastUpdated
                    )
                    ${selectPart}
                    FROM flights
                    """
                )
                
                // Drop the old table
                database.execSQL("DROP TABLE flights")
                
                // Rename the new table to the correct name
                database.execSQL("ALTER TABLE flights_new RENAME TO flights")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a backup of the current table
                database.execSQL("ALTER TABLE flights RENAME TO flights_backup")
                
                // Create a new table with the correct schema
                database.execSQL(
                    """
                    CREATE TABLE flights (
                        flightNumber TEXT NOT NULL PRIMARY KEY,
                        airline TEXT NOT NULL,
                        departureAirport TEXT NOT NULL,
                        arrivalAirport TEXT NOT NULL,
                        scheduledDeparture TEXT NOT NULL,
                        scheduledArrival TEXT NOT NULL,
                        actualDeparture TEXT,
                        actualArrival TEXT,
                        status TEXT NOT NULL,
                        delay INTEGER,
                        averageTime INTEGER,
                        latitude REAL,
                        longitude REAL,
                        altitude INTEGER,
                        direction INTEGER,
                        speed INTEGER,
                        lastUpdated TEXT
                    )
                    """
                )
                
                // Copy data from backup to new table
                database.execSQL(
                    """
                    INSERT OR REPLACE INTO flights (
                        flightNumber, airline, departureAirport, arrivalAirport,
                        scheduledDeparture, scheduledArrival, actualDeparture,
                        actualArrival, status, delay, averageTime, latitude,
                        longitude, altitude, direction, speed, lastUpdated
                    )
                    SELECT 
                        flightNumber, airline, departureAirport, arrivalAirport,
                        scheduledDeparture, scheduledArrival, actualDeparture,
                        actualArrival, status, delay, averageTime, latitude,
                        longitude, altitude, direction, speed, lastUpdated
                    FROM flights_backup
                    """
                )
                
                // Drop the backup table
                database.execSQL("DROP TABLE flights_backup")
            }
        }

        @Volatile
        private var INSTANCE: FlightDatabase? = null

        fun getDatabase(context: Context): FlightDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlightDatabase::class.java,
                    "flight_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}