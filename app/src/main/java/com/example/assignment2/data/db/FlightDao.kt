package com.example.assignment2.data.db

import androidx.room.*
import com.example.assignment2.data.model.Flight
import com.example.assignment2.data.model.StatusChangeRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlight(flight: Flight)

    @Query("SELECT * FROM flights ORDER BY lastUpdated DESC")
    fun getAllFlights(): Flow<List<Flight>>

    @Query("SELECT * FROM flights WHERE flightNumber = :flightNumber LIMIT 1")
    suspend fun getFlightByNumber(flightNumber: String): Flight?

    @Query("DELETE FROM flights WHERE flightNumber = :flightNumber")
    suspend fun deleteFlight(flightNumber: String)

    @Query("SELECT AVG(averageTime) FROM flights WHERE departureAirport = :departureAirport AND arrivalAirport = :arrivalAirport AND averageTime IS NOT NULL")
    suspend fun getAverageFlightTime(departureAirport: String, arrivalAirport: String): Long?

    @Query("SELECT * FROM flights WHERE departureAirport = :departureAirport AND arrivalAirport = :arrivalAirport ORDER BY lastUpdated DESC")
    fun getFlightsByRoute(departureAirport: String, arrivalAirport: String): Flow<List<Flight>>

    @Query("SELECT DISTINCT departureAirport FROM flights")
    suspend fun getAllDepartureAirports(): List<String>

    @Query("SELECT DISTINCT arrivalAirport FROM flights")
    suspend fun getAllArrivalAirports(): List<String>
    
    @Query("SELECT * FROM status_changes ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentStatusChanges(): List<StatusChangeRecord>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatusChange(statusChange: StatusChangeRecord)
}