package com.example.assignment2.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "status_changes")
data class StatusChangeRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val flightNumber: String,
    val airline: String,
    val previousStatus: String,
    val newStatus: String,
    val timestamp: Long = System.currentTimeMillis()
) 