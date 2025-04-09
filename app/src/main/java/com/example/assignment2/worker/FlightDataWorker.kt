package com.example.assignment2.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.assignment2.data.api.AviationStackApi
import com.example.assignment2.data.api.OpenSkyApi
import com.example.assignment2.data.db.FlightDatabase
import com.example.assignment2.data.model.Flight
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

class FlightDataWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val TAG = "FlightDataWorker"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder().create()

    private val aviationStackApi = Retrofit.Builder()
        .baseUrl("https://api.aviationstack.com/v1/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(AviationStackApi::class.java)
        
    private val openSkyApi = Retrofit.Builder()
        .baseUrl("https://opensky-network.org/api/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(OpenSkyApi::class.java)

    private val database = FlightDatabase.getDatabase(context)
    private val flightDao = database.flightDao()
    
    private val knownAircraftIcao = mapOf(
        "BA112" to "400942",
        "AA100" to "a0f1bb",
        "DL1234" to "a25cb1",
        "UA201" to "a1f932",
        "DL303" to "abd124",
        "EK203" to "896ab3"
    )

    override suspend fun doWork(): Result {
        return try {
            // Get the list of flights to track from the database
            val trackedFlights = flightDao.getAllTrackedFlights()
            
            // Process tracked flights from database
            for (flight in trackedFlights) {
                try {
                    // Try to get data from AviationStack
                    val response = aviationStackApi.getFlightInfo(
                        apiKey = "527e31422b4157cd7eba13fce52760f1",
                        flight_iata = flight.flightNumber
                    )
                    
                    if (response.data.isNotEmpty()) {
                        val flightData = response.data[0]
                        val updatedFlight = Flight(
                            flightNumber = "${flightData.airline.iata}${flightData.flight.number}",
                            airline = flightData.airline.name,
                            departureAirport = flightData.departure.iata,
                            arrivalAirport = flightData.arrival.iata,
                            scheduledDeparture = flightData.departure.scheduled,
                            scheduledArrival = flightData.arrival.scheduled,
                            actualDeparture = flightData.departure.actual,
                            actualArrival = flightData.arrival.actual,
                            status = flightData.flight_status,
                            delay = flightData.departure.delay,
                            latitude = flightData.live?.latitude,
                            longitude = flightData.live?.longitude,
                            altitude = flightData.live?.altitude?.toInt(),
                            direction = flightData.live?.direction?.toInt(),
                            speed = flightData.live?.speed_horizontal?.toInt(),
                            lastUpdated = flightData.live?.updated,
                            isTracked = true
                        )
                        
                        // Try to get OpenSky data to enhance position data
                        val icao24 = knownAircraftIcao[updatedFlight.flightNumber]
                        if (icao24 != null) {
                            try {
                                val currentTime = Instant.now().epochSecond
                                val openSkyResponse = openSkyApi.getAircraftStates(
                                    icao24 = icao24,
                                    time = currentTime
                                )
                                
                                openSkyResponse.states?.firstOrNull()?.let { state ->
                                    if (state.size >= 8) {
                                        // Update with more accurate position from OpenSky
                                        updatedFlight.latitude = (state[6] as? Double) ?: updatedFlight.latitude
                                        updatedFlight.longitude = (state[5] as? Double) ?: updatedFlight.longitude
                                        updatedFlight.altitude = ((state[7] as? Double)?.toInt()) ?: updatedFlight.altitude
                                        updatedFlight.direction = ((state[10] as? Double)?.toInt()) ?: updatedFlight.direction
                                        updatedFlight.speed = (((state[9] as? Double)?.times(3.6))?.toInt()) ?: updatedFlight.speed // Convert from m/s to km/h
                                        updatedFlight.lastUpdated = Instant.now().toString()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error getting OpenSky data for ${updatedFlight.flightNumber}", e)
                            }
                        }
                        
                        flightDao.upsertFlight(updatedFlight)
                    } else {
                        // If no data from AviationStack, try OpenSky as fallback
                        val icao24 = knownAircraftIcao[flight.flightNumber]
                        if (icao24 != null) {
                            try {
                                val currentTime = Instant.now().epochSecond
                                val openSkyResponse = openSkyApi.getAircraftStates(
                                    icao24 = icao24,
                                    time = currentTime
                                )
                                
                                openSkyResponse.states?.firstOrNull()?.let { state ->
                                    if (state.size >= 8) {
                                        val updatedFlight = flight.copy(
                                            latitude = (state[6] as? Double) ?: flight.latitude,
                                            longitude = (state[5] as? Double) ?: flight.longitude,
                                            altitude = ((state[7] as? Double)?.toInt()) ?: flight.altitude,
                                            direction = ((state[10] as? Double)?.toInt()) ?: flight.direction,
                                            speed = (((state[9] as? Double)?.times(3.6))?.toInt()) ?: flight.speed,
                                            status = "active",
                                            lastUpdated = Instant.now().toString()
                                        )
                                        flightDao.upsertFlight(updatedFlight)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error getting OpenSky data only for ${flight.flightNumber}", e)
                                // Create fallback data with error status
                                val now = Instant.now()
                                val updatedFlight = flight.copy(
                                    status = flight.status ?: "scheduled", 
                                    lastUpdated = now.toString()
                                )
                                flightDao.upsertFlight(updatedFlight)
                            }
                        }
                    }
                } catch (e: HttpException) {
                    if (e.code() == 429) {
                        Log.w(TAG, "Rate limit exceeded for flight ${flight.flightNumber}")
                        // Create fallback data when rate limited
                        val now = Instant.now()
                        val updatedFlight = flight.copy(
                            status = "error",
                            lastUpdated = now.toString()
                        )
                        flightDao.upsertFlight(updatedFlight)
                    } else {
                        Log.e(TAG, "HTTP error for flight ${flight.flightNumber}: ${e.message()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing flight ${flight.flightNumber}", e)
                }
            }
            
            // If no tracked flights, add test flights for demonstration
            if (trackedFlights.isEmpty()) {
                withContext(Dispatchers.IO) {
                    // Add test flights with realistic data
                    addTestFlights()
                }
            }
            
            // Schedule next update
            scheduleNextUpdate()
            
            Result.success()
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            Result.failure()
        }
    }
    
    private suspend fun addTestFlights() {
        val testFlights = listOf(
            Flight(
                flightNumber = "AA100",
                airline = "American Airlines",
                departureAirport = "JFK",
                arrivalAirport = "LAX",
                scheduledDeparture = Instant.now().plusSeconds(3600).toString(), // 1 hour from now
                scheduledArrival = Instant.now().plusSeconds(3600 * 7).toString(), // 7 hours from now
                actualDeparture = null,
                actualArrival = null,
                status = "scheduled",
                delay = null,
                latitude = 40.6413,
                longitude = -73.7781,
                altitude = 0,
                direction = 270,
                speed = 0,
                lastUpdated = Instant.now().toString(),
                isTracked = true
            ),
            Flight(
                flightNumber = "UA201",
                airline = "United Airlines",
                departureAirport = "SFO",
                arrivalAirport = "ORD",
                scheduledDeparture = Instant.now().plusSeconds(1800).toString(), // 30 minutes from now
                scheduledArrival = Instant.now().plusSeconds(1800 + 3600 * 4).toString(), // 4.5 hours from now
                actualDeparture = null,
                actualArrival = null,
                status = "scheduled",
                delay = 15,
                latitude = 37.6188,
                longitude = -122.3759,
                altitude = 0,
                direction = 90,
                speed = 0,
                lastUpdated = Instant.now().toString(),
                isTracked = true
            ),
            Flight(
                flightNumber = "DL303",
                airline = "Delta Air Lines",
                departureAirport = "ATL",
                arrivalAirport = "SEA",
                scheduledDeparture = Instant.now().minusSeconds(1800).toString(), // 30 minutes ago
                scheduledArrival = Instant.now().plusSeconds(3600 * 5 - 1800).toString(), // 4.5 hours from scheduled departure
                actualDeparture = Instant.now().minusSeconds(1800).toString(),
                actualArrival = null,
                status = "active",
                delay = 0,
                latitude = 36.9265,
                longitude = -89.4966,
                altitude = 10668,
                direction = 315,
                speed = 850,
                lastUpdated = Instant.now().toString(),
                isTracked = true
            ),
            Flight(
                flightNumber = "BA112",
                airline = "British Airways",
                departureAirport = "LHR",
                arrivalAirport = "JFK",
                scheduledDeparture = Instant.now().minusSeconds(3600 * 7).toString(), // 7 hours ago
                scheduledArrival = Instant.now().minusSeconds(3600).toString(), // 1 hour ago
                actualDeparture = Instant.now().minusSeconds(3600 * 7).toString(),
                actualArrival = Instant.now().minusSeconds(3600 + 1500).toString(), // Arrived 25 minutes late
                status = "landed",
                delay = 25,
                latitude = 40.6413,
                longitude = -73.7781,
                altitude = 0,
                direction = 0,
                speed = 0,
                lastUpdated = Instant.now().toString(),
                isTracked = true
            ),
            Flight(
                flightNumber = "EK203",
                airline = "Emirates",
                departureAirport = "DXB",
                arrivalAirport = "JFK",
                scheduledDeparture = Instant.now().plusSeconds(3600 * 2).toString(), // 2 hours from now
                scheduledArrival = Instant.now().plusSeconds(3600 * 16).toString(), // 16 hours from now
                actualDeparture = null,
                actualArrival = null,
                status = "cancelled",
                delay = null,
                latitude = null,
                longitude = null,
                altitude = null,
                direction = null,
                speed = null,
                lastUpdated = Instant.now().toString(),
                isTracked = true
            ),
            Flight(
                flightNumber = "AF1180",
                airline = "Air France",
                departureAirport = "CDG",
                arrivalAirport = "FCO",
                scheduledDeparture = Instant.now().minusSeconds(7200).toString(), // 2 hours ago
                scheduledArrival = Instant.now().plusSeconds(1800).toString(), // 30 minutes from now
                actualDeparture = Instant.now().minusSeconds(7200).toString(),
                actualArrival = null,
                status = "active",
                delay = 0,
                latitude = 44.4056,
                longitude = 8.8463,
                altitude = 11582,
                direction = 135,
                speed = 780,
                lastUpdated = Instant.now().toString(),
                isTracked = true
            ),
            Flight(
                flightNumber = "LH438",
                airline = "Lufthansa",
                departureAirport = "MUC",
                arrivalAirport = "BOS",
                scheduledDeparture = Instant.now().minusSeconds(14400).toString(), // 4 hours ago
                scheduledArrival = Instant.now().plusSeconds(10800).toString(), // 3 hours from now
                actualDeparture = Instant.now().minusSeconds(14400+1200).toString(), // 20 minutes late
                actualArrival = null,
                status = "active",
                delay = 20,
                latitude = 52.3105,
                longitude = -32.7684,
                altitude = 12192,
                direction = 290,
                speed = 870,
                lastUpdated = Instant.now().toString(),
                isTracked = true
            ),
            Flight(
                flightNumber = "QF8",
                airline = "Qantas",
                departureAirport = "SYD",
                arrivalAirport = "DFW",
                scheduledDeparture = Instant.now().minusSeconds(43200).toString(), // 12 hours ago
                scheduledArrival = Instant.now().plusSeconds(3600).toString(), // 1 hour from now
                actualDeparture = Instant.now().minusSeconds(43200).toString(),
                actualArrival = null,
                status = "active",
                delay = 0,
                latitude = 33.1843,
                longitude = -118.3852,
                altitude = 9144,
                direction = 65,
                speed = 830,
                lastUpdated = Instant.now().toString(),
                isTracked = true
            ),
            Flight(
                flightNumber = "SQ321",
                airline = "Singapore Airlines",
                departureAirport = "SIN",
                arrivalAirport = "LHR",
                scheduledDeparture = Instant.now().minusSeconds(36000).toString(), // 10 hours ago
                scheduledArrival = Instant.now().plusSeconds(7200).toString(), // 2 hours from now
                actualDeparture = Instant.now().minusSeconds(36000-900).toString(), // 15 minutes early
                actualArrival = null,
                status = "active",
                delay = -15,
                latitude = 52.5123,
                longitude = 14.3875,
                altitude = 11277,
                direction = 290,
                speed = 910,
                lastUpdated = Instant.now().toString(),
                isTracked = true
            )
        )
        
        for (flight in testFlights) {
            flightDao.upsertFlight(flight)
        }
        
        // Also add some historical flights for average time calculations
        addHistoricalFlights()
    }

    private suspend fun addHistoricalFlights() {
        // Popular routes with historical flight data for average time calculation
        val historicalFlights = listOf(
            // JFK to LAX flights
            Flight(
                flightNumber = "AA102",
                airline = "American Airlines",
                departureAirport = "JFK",
                arrivalAirport = "LAX",
                scheduledDeparture = Instant.now().minusSeconds(3600 * 24 * 10).toString(), // 10 days ago
                scheduledArrival = Instant.now().minusSeconds(3600 * 24 * 10 - 3600 * 6).toString(), // 6 hours after departure
                actualDeparture = Instant.now().minusSeconds(3600 * 24 * 10).toString(),
                actualArrival = Instant.now().minusSeconds(3600 * 24 * 10 - 3600 * 6 - 600).toString(), // 10 minutes late
                status = "landed",
                delay = 10,
                latitude = null,
                longitude = null,
                altitude = null,
                direction = null,
                speed = null,
                lastUpdated = Instant.now().minusSeconds(3600 * 24 * 10).toString(),
                isTracked = false
            ),
            Flight(
                flightNumber = "AA104",
                airline = "American Airlines",
                departureAirport = "JFK",
                arrivalAirport = "LAX",
                scheduledDeparture = Instant.now().minusSeconds(3600 * 24 * 8).toString(), // 8 days ago
                scheduledArrival = Instant.now().minusSeconds(3600 * 24 * 8 - 3600 * 6 - 1200).toString(), // 6 hours 20 minutes after departure
                actualDeparture = Instant.now().minusSeconds(3600 * 24 * 8).toString(),
                actualArrival = Instant.now().minusSeconds(3600 * 24 * 8 - 3600 * 6 - 1200).toString(),
                status = "landed",
                delay = 0,
                latitude = null,
                longitude = null,
                altitude = null,
                direction = null,
                speed = null,
                lastUpdated = Instant.now().minusSeconds(3600 * 24 * 8).toString(),
                isTracked = false
            ),
            
            // LHR to JFK flights
            Flight(
                flightNumber = "BA178",
                airline = "British Airways",
                departureAirport = "LHR",
                arrivalAirport = "JFK",
                scheduledDeparture = Instant.now().minusSeconds(3600 * 24 * 12).toString(), // 12 days ago
                scheduledArrival = Instant.now().minusSeconds(3600 * 24 * 12 - 3600 * 8).toString(), // 8 hours after departure
                actualDeparture = Instant.now().minusSeconds(3600 * 24 * 12 - 1800).toString(), // 30 minutes late
                actualArrival = Instant.now().minusSeconds(3600 * 24 * 12 - 3600 * 8 - 1800).toString(), // 30 minutes late
                status = "landed",
                delay = 30,
                latitude = null,
                longitude = null,
                altitude = null,
                direction = null,
                speed = null,
                lastUpdated = Instant.now().minusSeconds(3600 * 24 * 12).toString(),
                isTracked = false
            ),
            Flight(
                flightNumber = "BA114",
                airline = "British Airways",
                departureAirport = "LHR",
                arrivalAirport = "JFK",
                scheduledDeparture = Instant.now().minusSeconds(3600 * 24 * 5).toString(), // 5 days ago
                scheduledArrival = Instant.now().minusSeconds(3600 * 24 * 5 - 3600 * 7 - 1800).toString(), // 7.5 hours after departure
                actualDeparture = Instant.now().minusSeconds(3600 * 24 * 5).toString(),
                actualArrival = Instant.now().minusSeconds(3600 * 24 * 5 - 3600 * 7 - 1800 - 900).toString(), // 15 minutes late
                status = "landed",
                delay = 15,
                latitude = null,
                longitude = null,
                altitude = null,
                direction = null,
                speed = null,
                lastUpdated = Instant.now().minusSeconds(3600 * 24 * 5).toString(),
                isTracked = false
            ),
            
            // SFO to ORD flights
            Flight(
                flightNumber = "UA456",
                airline = "United Airlines",
                departureAirport = "SFO",
                arrivalAirport = "ORD",
                scheduledDeparture = Instant.now().minusSeconds(3600 * 24 * 7).toString(), // 7 days ago
                scheduledArrival = Instant.now().minusSeconds(3600 * 24 * 7 - 3600 * 4).toString(), // 4 hours after departure
                actualDeparture = Instant.now().minusSeconds(3600 * 24 * 7).toString(),
                actualArrival = Instant.now().minusSeconds(3600 * 24 * 7 - 3600 * 4).toString(),
                status = "landed",
                delay = 0,
                latitude = null,
                longitude = null,
                altitude = null,
                direction = null,
                speed = null,
                lastUpdated = Instant.now().minusSeconds(3600 * 24 * 7).toString(),
                isTracked = false
            ),
            Flight(
                flightNumber = "UA789",
                airline = "United Airlines",
                departureAirport = "SFO",
                arrivalAirport = "ORD",
                scheduledDeparture = Instant.now().minusSeconds(3600 * 24 * 3).toString(), // 3 days ago
                scheduledArrival = Instant.now().minusSeconds(3600 * 24 * 3 - 3600 * 4 - 900).toString(), // 4.25 hours after departure
                actualDeparture = Instant.now().minusSeconds(3600 * 24 * 3 - 1200).toString(), // 20 minutes late
                actualArrival = Instant.now().minusSeconds(3600 * 24 * 3 - 3600 * 4 - 900 - 1200).toString(), // 20 minutes late
                status = "landed",
                delay = 20,
                latitude = null,
                longitude = null,
                altitude = null,
                direction = null,
                speed = null,
                lastUpdated = Instant.now().minusSeconds(3600 * 24 * 3).toString(),
                isTracked = false
            ),
            
            // CDG to FCO flights
            Flight(
                flightNumber = "AF1104",
                airline = "Air France",
                departureAirport = "CDG",
                arrivalAirport = "FCO",
                scheduledDeparture = Instant.now().minusSeconds(3600 * 24 * 9).toString(), // 9 days ago
                scheduledArrival = Instant.now().minusSeconds(3600 * 24 * 9 - 3600 * 2).toString(), // 2 hours after departure
                actualDeparture = Instant.now().minusSeconds(3600 * 24 * 9 + 900).toString(), // 15 minutes early
                actualArrival = Instant.now().minusSeconds(3600 * 24 * 9 - 3600 * 2 + 900).toString(), // 15 minutes early
                status = "landed",
                delay = -15,
                latitude = null,
                longitude = null,
                altitude = null,
                direction = null,
                speed = null,
                lastUpdated = Instant.now().minusSeconds(3600 * 24 * 9).toString(),
                isTracked = false
            ),
            Flight(
                flightNumber = "AZ609",
                airline = "ITA Airways",
                departureAirport = "CDG",
                arrivalAirport = "FCO",
                scheduledDeparture = Instant.now().minusSeconds(3600 * 24 * 4).toString(), // 4 days ago
                scheduledArrival = Instant.now().minusSeconds(3600 * 24 * 4 - 3600 * 2 - 300).toString(), // 2 hours 5 minutes after departure
                actualDeparture = Instant.now().minusSeconds(3600 * 24 * 4 + 1800).toString(), // 30 minutes early
                actualArrival = Instant.now().minusSeconds(3600 * 24 * 4 - 3600 * 2 - 300 + 1800).toString(), // 30 minutes early
                status = "landed",
                delay = -30,
                latitude = null,
                longitude = null,
                altitude = null,
                direction = null,
                speed = null,
                lastUpdated = Instant.now().minusSeconds(3600 * 24 * 4).toString(),
                isTracked = false
            )
        )
        
        for (flight in historicalFlights) {
            flightDao.upsertFlight(flight)
        }
    }
    
    private fun scheduleNextUpdate() {
        // This is handled by WorkManager's periodic work request
        // and doesn't need additional logic here
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<FlightDataWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "flight_data_collection",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}