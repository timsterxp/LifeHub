package com.example.personal_secretary_backend

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.reactivestreams.KMongo
import java.util.Properties
import java.io.FileInputStream
import io.ktor.server.request.*
import kotlinx.serialization.Serializable

@Serializable
data class Notes(
    val id: Int,
    val date: String,
    val location: String? = null,
    val description: String
)

val props = Properties().apply {
    FileInputStream("src/main/resources/config.properties").use { load(it) }
}
val mongoUri = props.getProperty("mongodb_uri")
val client = KMongo.createClient(mongoUri).coroutine
val database = client.getDatabase("notesdb")
val notesCollection: CoroutineCollection<Notes> = database.getCollection("notes")
fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    runBlocking {
        println("=== Notes in database ===")
        notesCollection.find().toList().forEach { println(it) }
    }

    routing {
        get("/") {
            call.respondText("Ktor backend is running!")
        }

        get("/notes") {
            val notes = notesCollection.find().toList()
            call.respond(notes)
        }
        post("/notes") {
            val note = call.receive<Notes>()
            notesCollection.insertOne(note)
            call.respondText("Note added")
        }
    }
}

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}
