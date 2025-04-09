package com.example.assignment2.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.assignment2.ui.screen.AverageTimeScreen
import com.example.assignment2.ui.screen.FlightHistoryScreen
import com.example.assignment2.ui.screen.FlightTrackerScreen
import com.example.assignment2.ui.viewmodel.FlightViewModel

// Define the routes as sealed class for type safety
sealed class Screen(val route: String) {
    object FlightTracker : Screen("flight_tracker")
    object AverageTime : Screen("average_time")
    object TrackingHistory : Screen("tracking_history")
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: FlightViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.FlightTracker.route
    ) {
        composable(Screen.FlightTracker.route) {
            FlightTrackerScreen(
                viewModel = viewModel,
                onNavigateToAverageTime = {
                    navController.navigate(Screen.AverageTime.route) {
                        // Pop up to the start destination before navigating
                        // to avoid building up a large stack of destinations
                        popUpTo(Screen.FlightTracker.route) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when navigating back
                        restoreState = true
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.TrackingHistory.route) {
                        // Pop up to the start destination before navigating
                        popUpTo(Screen.FlightTracker.route) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when navigating back
                        restoreState = true
                    }
                }
            )
        }
        composable(Screen.AverageTime.route) {
            AverageTimeScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
        composable(Screen.TrackingHistory.route) {
            FlightHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
    }
}