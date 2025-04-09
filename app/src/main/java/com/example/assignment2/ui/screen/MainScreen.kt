package com.example.assignment2.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.assignment2.ui.viewmodel.FlightViewModel
import com.example.assignment2.ui.viewmodel.FlightUiState
import com.example.assignment2.data.model.Flight

private val DarkBlue = Color(0xFF0A192F)
private val MidnightBlue = Color(0xFF172A45)
private val LightBlue = Color(0xFF64FFDA)
private val DarkGray = Color(0xFF121212)
private val MidGray = Color(0xFF2D2D2D)
private val LightGray = Color(0xFFAAAAAA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: FlightViewModel) {
    var flightNumber by remember { mutableStateOf("") }
    var departureAirport by remember { mutableStateOf("") }
    var arrivalAirport by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()
    val trackedFlights by viewModel.trackedFlights.collectAsState()

    val customColorScheme = darkColorScheme(
        primary = LightBlue,
        onPrimary = DarkBlue,
        secondary = LightBlue,
        background = DarkBlue,
        surface = MidnightBlue,
        onBackground = Color.White,
        onSurface = Color.White,
        error = Color(0xFFCF6679)
    )

    MaterialTheme(colorScheme = customColorScheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBlue)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Flight Tracker",
                style = MaterialTheme.typography.headlineMedium,
                color = LightBlue
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MidnightBlue
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Search Flight",
                        style = MaterialTheme.typography.titleMedium,
                        color = LightBlue
                    )
                    OutlinedTextField(
                        value = flightNumber,
                        onValueChange = { newValue -> flightNumber = newValue },
                        label = { Text("Flight Number", color = LightGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.Gray,
                            cursorColor = Color(0xFF03A9F4),
                            focusedBorderColor = Color(0xFF03A9F4),
                            unfocusedBorderColor = Color.LightGray,
                            focusedLabelColor = Color(0xFF03A9F4),
                            unfocusedLabelColor = Color.LightGray,
                        ),
                    )
                    Button(
                        onClick = { viewModel.searchFlight(flightNumber) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LightBlue,
                            contentColor = DarkBlue
                        )
                    ) {
                        Text("Search")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MidnightBlue
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Calculate Average Time",
                        style = MaterialTheme.typography.titleMedium,
                        color = LightBlue
                    )
                    OutlinedTextField(
                        value = departureAirport,
                        onValueChange = { newValue -> departureAirport = newValue },
                        label = { Text("Departure Airport (IATA)", color = LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            cursorColor = LightBlue,
                            focusedBorderColor = LightBlue,
                            unfocusedBorderColor = LightGray
                        )
                    )
                    OutlinedTextField(
                        value = arrivalAirport,
                        onValueChange = { newValue -> arrivalAirport = newValue },
                        label = { Text("Arrival Airport (IATA)", color = LightGray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = LightBlue,
                            focusedBorderColor = LightBlue,
                            unfocusedBorderColor = LightGray
                        )
                    )
                    Button(
                        onClick = { viewModel.getAverageFlightTime(departureAirport, arrivalAirport) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LightBlue,
                            contentColor = DarkBlue
                        )
                    ) {
                        Text("Calculate")
                    }
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MidnightBlue
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Tracked Flights",
                        style = MaterialTheme.typography.titleMedium,
                        color = LightBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(trackedFlights) { flight ->
                            TrackedFlightCard(
                                flight = flight,
                                onDelete = { viewModel.removeFlight(flight.flightNumber) }
                            )
                        }
                    }
                }
            }

            when (uiState) {
                is FlightUiState.Loading -> {
                    CircularProgressIndicator(color = LightBlue)
                }
                is FlightUiState.Success -> {
                    val flight = (uiState as FlightUiState.Success).flight
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MidnightBlue
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Flight Details:", style = MaterialTheme.typography.titleMedium, color = LightBlue)
                            Text("Number: ${flight.flightNumber}", color = Color.White)
                            Text("Airline: ${flight.airline}", color = Color.White)
                            Text("From: ${flight.departureAirport}", color = Color.White)
                            Text("To: ${flight.arrivalAirport}", color = Color.White)
                            Text("Status: ${flight.status}", color = Color.White)
                            Text("Scheduled Departure: ${flight.scheduledDeparture}", color = Color.White)
                            Text("Scheduled Arrival: ${flight.scheduledArrival}", color = Color.White)
                            flight.direction?.let { dir ->
                                Text("Direction: ${"%.0f".format(dir)}°", color = Color.White)
                            }
                            flight.altitude?.let { alt ->
                                Text("Altitude: ${"%.2f".format(alt)} meters", color = Color.White)
                            }
                            flight.speed?.let { speed ->
                                Text("Speed: ${"%.0f".format(speed)} km/h", color = Color.White)
                            }
                            flight.actualDeparture?.let { Text("Actual Departure: $it", color = Color.White) }
                            flight.actualArrival?.let { Text("Actual Arrival: $it", color = Color.White) }
                            flight.delay?.let { delay ->
                                Text(
                                    text = "Delay: $delay minutes",
                                    color = if (delay > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
                is FlightUiState.Error -> {
                    Text(
                        text = (uiState as FlightUiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is FlightUiState.AverageTime -> {
                    val time = (uiState as FlightUiState.AverageTime).time
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MidnightBlue
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (time != null) {
                                Text(
                                    text = "Average Flight Time: ${time / 60} hours ${time % 60} minutes",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = LightBlue
                                )
                            } else {
                                Text(
                                    text = "No data available for this route",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = LightBlue
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackedFlightCard(flight: Flight, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MidGray
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Flight ${flight.flightNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    color = LightBlue
                )
                Text(text = flight.airline, color = Color.White)
                Text(text = "${flight.departureAirport} → ${flight.arrivalAirport}", color = Color.White)
                Text(text = "Status: ${flight.status}", color = Color.White)
                flight.delay?.let { delay ->
                    Text(
                        text = "Delay: $delay minutes",
                        color = if (delay > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove flight",
                    tint = LightBlue
                )
            }
        }
    }
}