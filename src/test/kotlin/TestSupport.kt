package com.example

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.*
import io.ktor.server.testing.*
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

/** A clock whose date tests can move forward between requests. */
class TestClock(var date: LocalDate) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = date.atStartOfDay(ZoneOffset.UTC).toInstant()
}

/**
 * Loads application.yaml but points the database at a fresh in-memory H2 instance.
 * Pass [clock] to control what "today" is for the study routes.
 */
fun ApplicationTestBuilder.testConfig(clock: Clock? = null) {
    environment {
        config = ApplicationConfig("application.yaml").mergeWith(
            MapApplicationConfig("flashcards.db.url" to "jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1"),
        )
    }
    if (clock != null) {
        application { attributes.put(ClockKey, clock) }
    }
}

/** A client that sends and receives JSON bodies. */
fun ApplicationTestBuilder.jsonClient(): HttpClient = createClient {
    install(ContentNegotiation) { json() }
}

/** Posts a raw JSON string, for bodies that don't match the request data classes. */
suspend fun HttpClient.postRawJson(path: String, body: String) = post(path) {
    contentType(ContentType.Application.Json)
    setBody(body)
}

suspend fun HttpClient.newUser(name: String = "Ada"): Int =
    postRawJson("/users", """{"name": "$name"}""").body<UserResponse>().id

suspend fun HttpClient.newDeck(name: String = "Deck"): Int =
    postRawJson("/decks", """{"name": "$name"}""").body<DeckResponse>().id

suspend fun HttpClient.newCard(deckId: Int, front: String, back: String = "back"): Int =
    postRawJson("/decks/$deckId/cards", """{"front": "$front", "back": "$back"}""").body<CardResponse>().id

suspend fun HttpClient.review(userId: Int, cardId: Int, grade: String) =
    postRawJson("/users/$userId/cards/$cardId/review", """{"grade": "$grade"}""")

suspend fun HttpClient.nextCards(userId: Int, deckId: Int): List<CardResponse> =
    get("/users/$userId/decks/$deckId/next").body()
