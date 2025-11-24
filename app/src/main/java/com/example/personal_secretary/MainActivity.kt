package com.example.personal_secretary

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.personal_secretary.ui.theme.Personal_SecretaryTheme
import androidx.compose.ui.unit.dp
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable

class MainActivity : ComponentActivity() {

    private lateinit var requestPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register for permission request
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    // Microphone permission granted
                    println("🎤 Microphone access granted")
                } else {
                    // Permission denied
                    println("❌ Microphone access denied")
                }
            }

        enableEdgeToEdge()
        setContent {
            Personal_SecretaryTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Greeting(name = "Android")
                        Spacer(modifier = Modifier.height(16.dp))
                        MicButton { checkMicrophonePermission() }
                        OpenNotesPage()
                        OpenTasksPage()
                        OpenWeatherPage()

                    }
                }
            }
        }
    }

    private fun checkMicrophonePermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Already granted
                println("🎤 Microphone access already granted")
            }
            else -> {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun MicButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text("Enable Microphone")
    }
}

/**
 * Open the notes page
 */
@Composable
fun OpenNotesPage() {
    val context = LocalContext.current
    Button(onClick = {
        val intent = Intent(context, NotesActivity::class.java)
        context.startActivity(intent)
    }) {
        Text("Open Notes Page")
    }
}

/**
 * Open the tasks
 */
@Composable
fun OpenTasksPage() {
    val context = LocalContext.current
    Button(onClick = {
        val intent = Intent(context, TasksActivity::class.java)
        context.startActivity(intent)
    }) {
        Text("Open Tasks Page")
    }
}

@Composable
fun OpenWeatherPage() {
    val context = LocalContext.current
    Button(onClick = {
        val intent = Intent(context, WeatherActivity::class.java)
        context.startActivity(intent)
    }) {
        Text("Open Weather Page")
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Personal_SecretaryTheme {
        Column {
            Greeting("Android")
            MicButton {}
        }
    }
}