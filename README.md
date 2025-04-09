# Flight Tracker Application

## Overview

The Flight Tracker Application is a modern Android app designed to provide real-time flight tracking and historical data analysis. Built with Jetpack Compose, the app offers a sleek and intuitive user interface, leveraging the latest Android development practices.

## Features

- **Real-time Flight Tracking**: Monitor flights in real-time with live updates on position, altitude, speed, and direction using AviationStack and OpenSky APIs.
- **Dual API Integration**: Uses both AviationStack for flight details and OpenSky Network for enhanced live position tracking.
- **Live Location Data**: View precise latitude, longitude, altitude, direction, and speed information directly from aviation APIs.
- **Historical Data**: Access historical flight data and track your flight history.
- **Average Flight Time Calculation**: Calculate the average flight time between two airports.
- **Modern UI**: Enjoy a beautiful and responsive user interface with a purple and light blue theme.
- **Dark Mode Support**: Fully customized dark theme that automatically adapts to system settings.
- **Background Data Collection**: Automatically collect flight data in the background using WorkManager every 15 minutes.

## API Integration

The application leverages two powerful aviation APIs to provide comprehensive flight data:

1. **AviationStack API**: Primary source for flight details including:

   - Flight schedules
   - Airline information
   - Departure and arrival details
   - Flight status
   - Basic tracking data

2. **OpenSky Network API**: Enhanced location tracking with:
   - Precise real-time coordinates
   - Accurate altitude measurements
   - Current speed and direction
   - Frequent position updates
   - Fallback tracking when AviationStack data is unavailable

This dual API approach ensures reliable flight tracking even when one service has limited data.

## Database Schema

The app uses Room Database to store flight information. Below is the schema for the `flights` table:

| Column Name          | Type    | Description                                          |
| -------------------- | ------- | ---------------------------------------------------- |
| `flightNumber`       | TEXT    | Unique identifier for the flight (Primary Key).      |
| `airline`            | TEXT    | Name of the airline operating the flight.            |
| `departureAirport`   | TEXT    | IATA code of the departure airport.                  |
| `arrivalAirport`     | TEXT    | IATA code of the arrival airport.                    |
| `scheduledDeparture` | TEXT    | Scheduled departure time in ISO format.              |
| `scheduledArrival`   | TEXT    | Scheduled arrival time in ISO format.                |
| `actualDeparture`    | TEXT    | Actual departure time in ISO format (nullable).      |
| `actualArrival`      | TEXT    | Actual arrival time in ISO format (nullable).        |
| `status`             | TEXT    | Current status of the flight (e.g., active, landed). |
| `delay`              | INTEGER | Delay in minutes (nullable).                         |
| `averageTime`        | INTEGER | Average flight time in minutes (nullable).           |
| `latitude`           | REAL    | Current latitude of the flight (nullable).           |
| `longitude`          | REAL    | Current longitude of the flight (nullable).          |
| `altitude`           | INTEGER | Current altitude in meters (nullable).               |
| `direction`          | INTEGER | Current direction in degrees (nullable).             |
| `speed`              | INTEGER | Current speed in km/h (nullable).                    |
| `lastUpdated`        | TEXT    | Timestamp of the last update in ISO format.          |

## Technical Details

- **Database Management**: Uses Room Database with migration support up to version 4.
- **Network Requests**: Retrofit with OkHttpClient for API requests with proper timeout handling.
- **API Integration**: Seamless integration of both AviationStack and OpenSky Network APIs.
- **Background Processing**: WorkManager for periodic data collection.
- **Architecture**: MVVM architecture with ViewModel and LiveData.
- **UI**: Jetpack Compose with Material 3 design components.
- **Theming**: Dynamic theme with light and dark mode support that adapts to system preferences.

## User Interface

The application features a carefully designed user interface with attention to detail:

- **Light/Dark Themes**: Fully implemented light and dark themes that automatically switch based on system settings.
- **Live Tracking Card**: Dedicated card component with real-time location data from OpenSky and visually distinct design.
- **Color Scheme**: Beautiful purple and light blue color palette that remains consistent across both themes.
- **Accessibility**: High contrast ratios and readable typography for improved accessibility.

## Using the App

To search for flights:

1. Enter a flight number (e.g., BA112, AA100, DL1234)
2. The app will search using both APIs to find the most accurate data
3. View detailed flight information including position data from OpenSky
4. Historical flights are stored for offline viewing

The app will automatically fallback to OpenSky data when AviationStack doesn't return results, ensuring you always get location data when available.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please fork the repository and submit a pull request for any improvements or bug fixes.

## Contact

For any inquiries or support, please contact [Your Name] at [your.email@example.com].
# Flight-Tracker
