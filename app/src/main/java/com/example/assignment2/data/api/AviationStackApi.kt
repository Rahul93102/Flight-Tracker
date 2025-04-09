package com.example.assignment2.data.api

import retrofit2.http.GET
import retrofit2.http.Query
//import com.example.assignment2.data.model.FlightResponse
import com.google.gson.JsonElement

interface AviationStackApi {

    @GET("flights")
    suspend fun getFlightInfoRaw(
        @Query("access_key") apiKey: String,
        @Query("flight_iata") flight_iata: String
    ): JsonElement


    @GET("flights")
    suspend fun getFlightInfo(
        @Query("access_key") apiKey: String,
        @Query("flight_iata") flight_iata: String
    ): FlightResponse


    @GET("flights")
    suspend fun getFlightsByRouteRaw(
        @Query("access_key") apiKey: String,
        @Query("dep_iata") departureAirport: String,
        @Query("arr_iata") arrivalAirport: String
    ): JsonElement

    @GET("flights")
    suspend fun getFlightsByRoute(
        @Query("access_key") apiKey: String,
        @Query("dep_iata") departureAirport: String,
        @Query("arr_iata") arrivalAirport: String
    ): FlightResponse
}

// OpenSky API interface for better location data
interface OpenSkyApi {
    @GET("states/all")
    suspend fun getAircraftStates(
        @Query("icao24") icao24: String? = null,
        @Query("time") time: Long? = null
    ): OpenSkyResponse
}

data class OpenSkyResponse(
    val time: Long = 0,
    val states: List<List<Any>>? = null
)

data class FlightResponse(
    val data: List<FlightData> = emptyList()
)

data class FlightData(
    val flight_status: String = "",
    val departure: DepartureInfo = DepartureInfo(),
    val arrival: ArrivalInfo = ArrivalInfo(),
    val airline: AirlineInfo = AirlineInfo(),
    val flight: FlightInfo = FlightInfo(),
    val live: LiveInfo? = null
)

data class DepartureInfo(
    val iata: String = "",
    val scheduled: String = "",
    val actual: String? = null,
    val delay: Int? = null
)

data class ArrivalInfo(
    val iata: String = "",
    val scheduled: String = "",
    val actual: String? = null,
    val delay: Int? = null
)

data class AirlineInfo(
    val name: String = "",
    val iata: String = ""
)

data class FlightInfo(
    val number: String = ""
)

data class LiveInfo(
    val updated: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val direction: Double? = null,
    val speed_horizontal: Double? = null,
    val speed_vertical: Double? = null
)

data class Pagination(
    val limit: Int,
    val offset: Int,
    val count: Int,
    val total: Int
)
