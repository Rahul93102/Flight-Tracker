package com.example.assignment2.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

object RetrofitService {
    private const val BASE_URL = "http://api.aviationstack.com/v1/"

    fun createAviationStackApi(): AviationStackApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AviationStackApi::class.java)
    }
}



data class FlightIdentifier(
    val iata: String,
    val icao: String,
    val number: String
)

data class Airline(
    val name: String,
    val iata: String,
    val icao: String
)

data class FlightLocation(
    val airport: String,
    val iata: String,
    val icao: String,
    val scheduled: String,
    val actual: String?,
    val delay: Int?
)