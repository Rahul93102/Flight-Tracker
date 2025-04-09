package com.example.assignment2.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.assignment2.data.model.StatusChangeRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusChangeDao {
    @Insert
    suspend fun insertStatusChange(statusChange: StatusChangeRecord)
    
    @Query("SELECT * FROM status_changes ORDER BY timestamp DESC")
    fun getAllStatusChanges(): Flow<List<StatusChangeRecord>>
    
    @Query("SELECT * FROM status_changes WHERE flightNumber = :flightNumber ORDER BY timestamp DESC")
    fun getStatusChangesForFlight(flightNumber: String): Flow<List<StatusChangeRecord>>
    
    @Query("DELETE FROM status_changes WHERE flightNumber = :flightNumber")
    suspend fun deleteStatusChangesForFlight(flightNumber: String)
    
    @Query("DELETE FROM status_changes")
    suspend fun deleteAllStatusChanges()
} 