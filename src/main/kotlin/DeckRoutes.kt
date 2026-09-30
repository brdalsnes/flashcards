package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configureDeckRoutes() {
    routing {
        post("/decks") {
            val name = call.receive<CreateDeckRequest>().name
            if (name.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            val id = transaction { Decks.insert { it[Decks.name] = name }[Decks.id] }
            call.respond(HttpStatusCode.Created, DeckResponse(id, name))
        }

        post("/decks/{id}/cards") {
            val deckId = call.existingDeckId() ?: return@post
            val request = call.receive<CreateCardRequest>()
            val front = request.front
            val back = request.back
            if (front.isNullOrBlank() || back.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            val id = transaction {
                Cards.insert {
                    it[Cards.deckId] = deckId
                    it[Cards.front] = front
                    it[Cards.back] = back
                }[Cards.id]
            }
            call.respond(HttpStatusCode.Created, CardResponse(id, deckId, front, back))
        }

        get("/decks/{id}/cards") {
            val deckId = call.existingDeckId() ?: return@get
            val cards = transaction {
                Cards.selectAll()
                    .where { Cards.deckId eq deckId }
                    .orderBy(Cards.id, SortOrder.ASC)
                    .map { it.toCardResponse() }
            }
            call.respond(cards)
        }
    }
}

fun ResultRow.toCardResponse() = CardResponse(this[Cards.id], this[Cards.deckId], this[Cards.front], this[Cards.back])

/** Returns the `id` path parameter if that deck exists, otherwise responds 404 and returns null. */
suspend fun RoutingCall.existingDeckId(): Int? {
    val id = parameters["id"]?.toIntOrNull()
    val exists = id != null && transaction { Decks.selectAll().where { Decks.id eq id }.any() }
    if (!exists) {
        respond(HttpStatusCode.NotFound)
        return null
    }
    return id
}
