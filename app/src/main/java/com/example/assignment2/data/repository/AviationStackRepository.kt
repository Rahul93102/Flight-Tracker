package com.example.assignment2.data.repository

import android.content.Context
import android.util.Log
import com.example.assignment2.data.api.AviationStackApi
import com.example.assignment2.data.api.FlightResponse
import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AviationStackRepository(
    private val api: AviationStackApi,
    private val context: Context,
    private val apiKey: String
) {
    private val gson = Gson()

    private val jsonDirectory by lazy {
        File(context.filesDir, "flight_json").apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun getFlightInfo(flightIata: String): FlightResponse {
        return withContext(Dispatchers.IO) {
            try {
                val jsonResponse = api.getFlightInfoRaw(apiKey, flightIata)

                val fileName = "flight_${flightIata}_${getTimestamp()}.json"
                saveJsonToFile(fileName, jsonResponse.toString())

                api.getFlightInfo(apiKey, flightIata)
            } catch (e: Exception) {
                Log.e("AviationStackRepository", "Error getting flight info", e)
                throw e
            }
        }
    }

    suspend fun getFlightsByRoute(depIata: String, arrIata: String): FlightResponse {
        return withContext(Dispatchers.IO) {
            try {
                val jsonResponse = api.getFlightsByRouteRaw(apiKey, depIata, arrIata)

                val fileName = "route_${depIata}_${arrIata}_${getTimestamp()}.json"
                saveJsonToFile(fileName, jsonResponse.toString())

                api.getFlightsByRoute(apiKey, depIata, arrIata)
            } catch (e: Exception) {
                Log.e("AviationStackRepository", "Error getting flights by route", e)
                throw e
            }
        }
    }

    private fun saveJsonToFile(fileName: String, jsonContent: String) {
        try {
            val file = File(jsonDirectory, fileName)
            FileWriter(file).use { writer ->
                writer.write(jsonContent)
            }
            Log.d("AviationStackRepository", "JSON saved to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("AviationStackRepository", "Error saving JSON file", e)
        }
    }

    fun loadJsonFile(fileName: String): FlightResponse? {
        try {
            val file = File(jsonDirectory, fileName)
            if (!file.exists()) {
                Log.e("AviationStackRepository", "File not found: ${file.absolutePath}")
                return null
            }

            val jsonContent = file.readText()
            return gson.fromJson(jsonContent, FlightResponse::class.java)
        } catch (e: Exception) {
            Log.e("AviationStackRepository", "Error loading JSON file", e)
            return null
        }
    }

    fun getSavedJsonFiles(): List<String> {
        return jsonDirectory.listFiles()
            ?.filter { it.name.endsWith(".json") }
            ?.map { it.name }
            ?.sortedByDescending { it }
            ?: emptyList()
    }

    fun deleteJsonFile(fileName: String): Boolean {
        val file = File(jsonDirectory, fileName)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    private fun getTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return dateFormat.format(Date())
    }
}