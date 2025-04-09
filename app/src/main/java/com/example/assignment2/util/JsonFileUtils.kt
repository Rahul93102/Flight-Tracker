package com.example.assignment2.util

import android.content.Context
import android.util.Log
import com.example.assignment2.data.api.FlightResponse
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.File
import java.io.FileWriter
import java.io.IOException

object JsonFileUtils {
    private const val TAG = "JsonFileUtils"
    private val gson = Gson()

    fun formatJson(jsonString: String): String {
        return try {
            val jsonElement = JsonParser().parse(jsonString)
            gson.toJson(jsonElement)
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting JSON", e)
            jsonString
        }
    }

    fun <T> saveObjectAsJson(context: Context, fileName: String, obj: T) {
        try {
            val jsonString = gson.toJson(obj)
            val directory = File(context.filesDir, "flight_json").apply {
                if (!exists()) mkdirs()
            }
            val file = File(directory, fileName)

            FileWriter(file).use { writer ->
                writer.write(jsonString)
                writer.flush()
            }
            Log.d(TAG, "Saved JSON to ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving object as JSON", e)
        }
    }

    fun loadFlightResponseFromJson(context: Context, fileName: String): FlightResponse? {
        try {
            val directory = File(context.filesDir, "flight_json")
            val file = File(directory, fileName)

            if (!file.exists()) {
                Log.e(TAG, "File not found: ${file.absolutePath}")
                return null
            }

            val jsonContent = file.readText()
            return gson.fromJson(jsonContent, FlightResponse::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading FlightResponse from JSON", e)
            return null
        }
    }

    fun extractInfoFromFilename(fileName: String): String {
        return when {
            fileName.startsWith("flight_") -> {
                val parts = fileName.removePrefix("flight_").split("_")
                if (parts.isNotEmpty()) "Flight: ${parts[0]}" else fileName
            }
            fileName.startsWith("route_") -> {
                val parts = fileName.removePrefix("route_").split("_")
                if (parts.size >= 2) "Route: ${parts[0]} → ${parts[1]}" else fileName
            }
            else -> fileName
        }
    }
}