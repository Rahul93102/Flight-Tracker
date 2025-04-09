package com.example.assignment2.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey


//data class Flight(
//    @PrimaryKey
//    val flightNumber: String,
//    val airline: String = "",
//    val departureAirport: String = "",
//    val arrivalAirport: String = "",
//    val scheduledDeparture: String = "",
//    val scheduledArrival: String = "",
//    val actualDeparture: String? = null,
//    val actualArrival: String? = null,
//    val status: String = "",
//    val delay: Int? = null,
//    val flightDuration: Long? = null,
//    val lastUpdated: Long = 0
//)

@Entity(tableName = "flights")
data class Flight(
    @PrimaryKey
    val flightNumber: String,
    val airline: String,
    val departureAirport: String,
    val arrivalAirport: String,
    val scheduledDeparture: String,
    val scheduledArrival: String,
    val actualDeparture: String?,
    val actualArrival: String?,
    val status: String,
    val delay: Int?,
    val averageTime: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Int? = null,
    val direction: Int? = null,
    val speed: Int? = null,
    val lastUpdated: String? = null
)