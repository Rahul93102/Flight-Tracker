package com.example.assignment2.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.assignment2.data.api.AviationStackApi
import com.example.assignment2.data.api.OpenSkyApi
import com.example.assignment2.data.db.FlightDatabase
import com.example.assignment2.data.model.Flight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

class FlightViewModel(application: Application) : AndroidViewModel(application) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val aviationStackApi = Retrofit.Builder()
        .baseUrl("https://api.aviationstack.com/v1/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AviationStackApi::class.java)
        
    private val openSkyApi = Retrofit.Builder()
        .baseUrl("https://opensky-network.org/api/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenSkyApi::class.java)

    private val database = FlightDatabase.getDatabase(application)
    private val flightDao = database.flightDao()

    private val _uiState = MutableStateFlow<FlightUiState>(FlightUiState.Initial)
    val uiState: StateFlow<FlightUiState> = _uiState.asStateFlow()

    private val _trackedFlights = MutableStateFlow<List<Flight>>(emptyList())
    val trackedFlights: StateFlow<List<Flight>> = _trackedFlights.asStateFlow()

    private val knownAircraftIcao = mapOf(
        "BA112" to "400942",
        "AA100" to "a0f1bb",
        "DL1234" to "a25cb1"
    )

    init {
        loadTrackedFlights()
    }

    private fun loadTrackedFlights() {
        viewModelScope.launch {
            try {
                flightDao.getAllFlights().collect { flights ->
                    _trackedFlights.value = flights
                }
            } catch (e: Exception) {
                _uiState.value = FlightUiState.Error(e.message ?: "Failed to load tracked flights")
            }
        }
    }

    fun searchFlight(flightNumber: String) {
        viewModelScope.launch {
            try {
                _uiState.value = FlightUiState.Loading
                
                val response = aviationStackApi.getFlightInfo(
                    apiKey = "7b40884ee26bc0c70da50c36506cd8e4",
                    flight_iata = flightNumber
                )
                
                if (response.data.isNotEmpty()) {
                    val flightData = response.data[0]
                    
                    val flight = Flight(
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
                        lastUpdated = flightData.live?.updated
                    )
                    
                    try {
                        val icao24 = knownAircraftIcao[flightNumber]
                        if (icao24 != null) {
                            val currentTime = Instant.now().epochSecond
                            val openSkyResponse = openSkyApi.getAircraftStates(
                                icao24 = icao24,
                                time = currentTime
                            )
                            
                            openSkyResponse.states?.firstOrNull()?.let { state ->
                                if (state.size >= 8) {
                                    // Update with more accurate position from OpenSky
                                    val updatedFlight = flight.copy(
                                        latitude = (state[6] as? Double) ?: flight.latitude,
                                        longitude = (state[5] as? Double) ?: flight.longitude,
                                        altitude = ((state[7] as? Double)?.toInt()) ?: flight.altitude,
                                        direction = ((state[10] as? Double)?.toInt()) ?: flight.direction,
                                        speed = (((state[9] as? Double)?.times(3.6))?.toInt()) ?: flight.speed, // Convert from m/s to km/h
                                        lastUpdated = Instant.now().toString()
                                    )
                                    _uiState.value = FlightUiState.Success(updatedFlight)
                                    flightDao.insertFlight(updatedFlight)
                                    return@launch
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FlightViewModel", "Error getting OpenSky data", e)
                    }
                    
                    _uiState.value = FlightUiState.Success(flight)
                    flightDao.insertFlight(flight)
                } else {
                    val icao24 = knownAircraftIcao[flightNumber]
                    if (icao24 != null) {
                        try {
                            val currentTime = Instant.now().epochSecond
                            val openSkyResponse = openSkyApi.getAircraftStates(
                                icao24 = icao24,
                                time = currentTime
                            )
                            
                            openSkyResponse.states?.firstOrNull()?.let { state ->
                                if (state.size >= 8) {
                                    val callsign = (state[1] as? String)?.trim() ?: flightNumber
                                    val flight = Flight(
                                        flightNumber = flightNumber,
                                        airline = "Found via OpenSky",
                                        departureAirport = "---",
                                        arrivalAirport = "---",
                                        scheduledDeparture = Instant.now().toString(),
                                        scheduledArrival = Instant.now().plusSeconds(3600).toString(),
                                        actualDeparture = null,
                                        actualArrival = null,
                                        status = "active",
                                        delay = null,
                                        latitude = state[6] as? Double,
                                        longitude = state[5] as? Double,
                                        altitude = (state[7] as? Double)?.toInt(),
                                        direction = (state[10] as? Double)?.toInt(),
                                        speed = ((state[9] as? Double)?.times(3.6))?.toInt(), // Convert from m/s to km/h
                                        lastUpdated = Instant.now().toString()
                                    )
                                    _uiState.value = FlightUiState.Success(flight)
                                    flightDao.insertFlight(flight)
                                    return@launch
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("FlightViewModel", "Error getting OpenSky data only", e)
                        }
                    }
                    
                    _uiState.value = FlightUiState.Error("No route found. Please check the flight number and try again.")
                }
            } catch (e: IOException) {
                _uiState.value = FlightUiState.Error("Network error: Please check your internet connection")
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 429) {
                    _uiState.value = FlightUiState.Error("Rate limit exceeded (HTTP 429). Please try again later.")
                } else {
                    _uiState.value = FlightUiState.Error("HTTP Error: ${e.code()}. Please try again later.")
                }
            } catch (e: Exception) {
                _uiState.value = FlightUiState.Error("Error: ${e.message ?: "Unknown error occurred"}")
            }
        }
    }

    fun getAverageFlightTime(departure: String, arrival: String) {
        viewModelScope.launch {
            try {
                _uiState.value = FlightUiState.Loading
                val averageTime = flightDao.getAverageFlightTime(departure, arrival)
                _uiState.value = FlightUiState.AverageTime(averageTime)
            } catch (e: Exception) {
                _uiState.value = FlightUiState.Error("Error calculating average time: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun removeFlight(flightNumber: String) {
        viewModelScope.launch {
            try {
                flightDao.deleteFlight(flightNumber)
            } catch (e: Exception) {
                _uiState.value = FlightUiState.Error(e.message ?: "Failed to remove flight")
            }
        }
    }

    fun getStatusChanges() {
        viewModelScope.launch {
            try {
                _uiState.value = FlightUiState.Loading
                val changes = flightDao.getRecentStatusChanges()
                _uiState.value = if (changes.isNotEmpty()) {
                    val statusChanges = changes.map { change ->
                        StatusChange(
                            flightNumber = change.flightNumber,
                            airline = change.airline ?: "Unknown",
                            previousStatus = change.previousStatus ?: "Unknown",
                            newStatus = change.newStatus ?: "Unknown",
                            timestamp = change.timestamp.toString()
                        )
                    }
                    FlightUiState.StatusChanges(statusChanges)
                } else {
                    FlightUiState.Error("No status changes found")
                }
            } catch (e: Exception) {
                _uiState.value = FlightUiState.Error("Error loading status changes: ${e.message ?: "Unknown error"}")
            }
        }
    }
    
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            if (modelClass.isAssignableFrom(FlightViewModel::class.java)) {
                return FlightViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

sealed class FlightUiState {
    object Initial : FlightUiState()
    object Loading : FlightUiState()
    data class Success(val flight: Flight) : FlightUiState()
    data class Error(val message: String) : FlightUiState()
    data class AverageTime(val time: Long?) : FlightUiState()
    data class StatusChanges(val changes: List<StatusChange>) : FlightUiState()
}

data class StatusChange(
    val flightNumber: String,
    val airline: String,
    val previousStatus: String,
    val newStatus: String,
    val timestamp: String
)