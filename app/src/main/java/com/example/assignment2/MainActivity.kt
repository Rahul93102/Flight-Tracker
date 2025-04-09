package com.example.assignment2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.assignment2.navigation.NavGraph
import com.example.assignment2.ui.theme.Assignment2Theme
import com.example.assignment2.ui.viewmodel.FlightViewModel
import com.example.assignment2.worker.FlightDataWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up background worker for flight data collection
        setupBackgroundWorker()
        
        setContent {
            val darkTheme = isSystemInDarkTheme()
            
            Assignment2Theme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Create the view model
                    val viewModel: FlightViewModel = viewModel(factory = FlightViewModel.Factory(application))
                    
                    // Set up navigation
                    val navController = rememberNavController()
                    NavGraph(navController = navController, viewModel = viewModel)
                }
            }
        }
    }
    
    private fun setupBackgroundWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val workRequest = PeriodicWorkRequestBuilder<FlightDataWorker>(
            15, TimeUnit.MINUTES
        )
        .setConstraints(constraints)
        .build()
        
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "flight_data_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}