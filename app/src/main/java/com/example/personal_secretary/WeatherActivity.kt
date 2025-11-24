package com.example.personal_secretary

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.personal_secretary.ui.theme.Personal_SecretaryTheme
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.google.android.gms.location.LocationServices

class WeatherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Personal_SecretaryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WeatherPage()
                }
            }
        }
    }
}

@Composable
fun WeatherPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var weatherText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }


    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                scope.launch {
                    fetchWeather(context) { result ->
                        weatherText = result
                        isLoading = false
                    }
                }
            } else {
                weatherText = "Location permission denied"
                isLoading = false
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            val permission = Manifest.permission.ACCESS_FINE_LOCATION
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                isLoading = true
                scope.launch {
                    fetchWeather(context) { result ->
                        weatherText = result
                        isLoading = false
                    }
                }
            } else {
                requestPermissionLauncher.launch(permission)
            }
        }) {
            Text("Click here to show weather data")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) CircularProgressIndicator()
        else weatherText?.let { Text(it) }
    }
}


fun Context.getApiKey(key: String): String? {
    return try {
        val props = java.util.Properties()
        val inputStream = resources.openRawResource(R.raw.config) // config.properties
        props.load(inputStream)
        inputStream.close()
        val value = props.getProperty(key)
        if (value.isNullOrEmpty()) {
            println("⚠️ API key '$key' not found in config.properties")
        }
        value
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}



private suspend fun fetchWeather(context: Context, onResult: (String) -> Unit) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val location = fusedLocationClient.lastLocation.await()

        if (location != null) {
            val lat = location.latitude
            val lon = location.longitude

            val apiKey = context.getApiKey("OPENWEATHERMAP_API_KEY") ?: ""
            val client = HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            try {
                // Use today's date
                val today = LocalDate.now()
                val dateStr = today.format(DateTimeFormatter.ISO_DATE)

                val url =
                    "https://api.openweathermap.org/data/3.0/onecall/day_summary?lat=$lat&lon=$lon&date=$dateStr&appid=$apiKey&units=imperial"

                // Print the full URL so you can test it
                println("🌐 OpenWeather URL: $url")

                val response: DaySummaryResponse = client.get(url).body()

                val tempMin = response.temperature.min
                val tempMax = response.temperature.max
                val humidityAfternoon = response.humidity.afternoon
                val cloudAfternoon = response.cloud_cover.afternoon

                onResult(
                    "Min Temperature: $tempMin F\n" +
                            "Max Temperature: $tempMax F\n" +
                            "Afternoon Humidity: $humidityAfternoon%\n" +
                            "Afternoon Cloud Cover: $cloudAfternoon%"
                )
            } finally {
                client.close()
            }
        } else {
            onResult("Unable to get location (emulator may not provide one)")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onResult("Error fetching weather: ${e.localizedMessage}")
    }
}



// Onecall day summary prints in format of:

/**
 * {
 * lat
 * lon
 * tz
 * date
 * units
 * cloud cover
 *  afternoon
 *  }
 *    humidity
 *      afternoon
 *    precipitation
 *      total
 *    temperature
 *      min
 *      max
 *      afternoon
 *      night
 *      evening
 *      morning
 *    pressure
 *     afternoon
 *    wind
 *     max
 *       speed
 *       direction
 */
@Serializable
data class DaySummaryResponse(
    val lat: Double,
    val lon: Double,
    val date: String,
    val temperature: Temperature,
    val humidity: Humidity,
    val cloud_cover: CloudCover
)

@Serializable
data class Temperature(
    val min: Float,
    val max: Float,
    val afternoon: Float,
    val night: Float,
    val evening: Float,
    val morning: Float
)

@Serializable
data class Humidity(
    val afternoon: Float
)

@Serializable
data class CloudCover(
    val afternoon: Float
)
