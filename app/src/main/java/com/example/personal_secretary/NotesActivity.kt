package com.example.personal_secretary

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personal_secretary.notes.NoteModel
import com.example.personal_secretary.ui.theme.Personal_SecretaryTheme
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import androidx.compose.material3.Surface

@Serializable
data class NoteRequest(
    val id: Int,
    val date: String,
    val location: String? = null,
    val description: String
)

class NotesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Personal_SecretaryTheme {
                Surface(modifier = Modifier) {
                    NotesScreen(
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    var notes by remember { mutableStateOf(listOf<NoteModel>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showForm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Fetch notes
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                notes = NetworkClient.client.get("http://10.0.2.2:8080/notes").body()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showForm = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { paddingValues ->

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            if (isLoading) {
                Text("Loading notes...")
            } else if (notes.isEmpty()) {
                Text("No notes found")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(notes) { note ->
                        NoteItem(note)
                    }
                }
            }
        }

        if (showForm) {
            AddNoteDialog(
                onDismiss = { showForm = false },
                onSave = { newNote ->
                    scope.launch {
                        try {
                            val response: HttpResponse =
                                NetworkClient.client.post("http://10.0.2.2:8080/notes") {
                                    setBody(newNote)
                                }

                            if (response.status.value in 200..299) {
                                notes =
                                    NetworkClient.client.get("http://10.0.2.2:8080/notes").body()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    showForm = false
                }
            )
        }
    }
}


@Composable
fun NoteItem(note: NoteModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = note.date)
                note.location?.let { loc ->
                    Text(text = loc, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = note.description)
        }
    }
}

@Composable
fun AddNoteDialog(onDismiss: () -> Unit, onSave: (NoteRequest) -> Unit) {
    var id by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Note") },
        text = {
            Column {
                OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("ID") })
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date") })
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (id.isNotBlank() && date.isNotBlank() && description.isNotBlank()) {
                    onSave(
                        NoteRequest(
                            id = id.toInt(),
                            date = date,
                            location = if (location.isBlank()) null else location,
                            description = description
                        )
                    )
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
