package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** Tests put a fixed clock here; without one the system clock is used. */
val ClockKey = AttributeKey<Clock>("Clock")

fun Application.clock(): Clock = attributes.getOrNull(ClockKey) ?: Clock.systemDefaultZone()

fun Application.configureStudyRoutes() {
    routing {
        get("/users/{userId}/decks/{id}/next") {
            val userId = call.existingUserId() ?: return@get
            val deckId = call.existingDeckId() ?: return@get
            val today = LocalDate.now(call.application.clock())
            val next = transaction { nextDueCard(userId, deckId, today) }
            call.respond(listOfNotNull(next))
        }

        post("/users/{userId}/cards/{cardId}/review") {
            val userId = call.existingUserId() ?: return@post
            val cardId = call.existingCardId() ?: return@post
            val grade = runCatching { call.receive<ReviewRequest>() }.getOrNull()?.grade
            if (grade == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            val clock = call.application.clock()
            val state = recordReview(userId, cardId, grade, Instant.now(clock), LocalDate.now(clock))
            call.respond(state)
        }
    }
}

/** Due cards the user has reviewed come first (earliest due date), then new cards in the order they were added. */
private fun nextDueCard(userId: Int, deckId: Int, today: LocalDate): CardResponse? {
    val reviewed = Cards.join(
        ReviewStates, JoinType.INNER, Cards.id, ReviewStates.cardId,
        additionalConstraint = { ReviewStates.userId eq userId },
    )
        .selectAll()
        .where { (Cards.deckId eq deckId) and (ReviewStates.dueDate lessEq today) }
        .orderBy(ReviewStates.dueDate to SortOrder.ASC, Cards.id to SortOrder.ASC)
        .limit(1)
        .firstOrNull()
    if (reviewed != null) return reviewed.toCardResponse()

    return Cards.join(
        ReviewStates, JoinType.LEFT, Cards.id, ReviewStates.cardId,
        additionalConstraint = { ReviewStates.userId eq userId },
    )
        .selectAll()
        .where { (Cards.deckId eq deckId) and ReviewStates.cardId.isNull() }
        .orderBy(Cards.id, SortOrder.ASC)
        .limit(1)
        .firstOrNull()
        ?.toCardResponse()
}

/**
 * Updates the user's review state and appends to the review log in one transaction.
 * [afterStateWrite] runs between the two writes so tests can prove a failure rolls both back.
 */
fun recordReview(
    userId: Int,
    cardId: Int,
    grade: Grade,
    now: Instant,
    today: LocalDate,
    afterStateWrite: () -> Unit = {},
): ReviewStateResponse = transaction {
    val previous = ReviewStates.selectAll()
        .where { (ReviewStates.userId eq userId) and (ReviewStates.cardId eq cardId) }
        .firstOrNull()
        ?.get(ReviewStates.intervalDays)
    val next = schedule(previous, grade, today)

    if (previous == null) {
        ReviewStates.insert {
            it[ReviewStates.userId] = userId
            it[ReviewStates.cardId] = cardId
            it[dueDate] = next.dueDate
            it[intervalDays] = next.intervalDays
        }
    } else {
        ReviewStates.update({ (ReviewStates.userId eq userId) and (ReviewStates.cardId eq cardId) }) {
            it[dueDate] = next.dueDate
            it[intervalDays] = next.intervalDays
        }
    }
    afterStateWrite()
    Reviews.insert {
        it[Reviews.userId] = userId
        it[Reviews.cardId] = cardId
        it[Reviews.grade] = grade
        it[reviewedAt] = now
        it[resultingIntervalDays] = next.intervalDays
    }
    ReviewStateResponse(userId, cardId, next.dueDate.toString(), next.intervalDays)
}

/** Returns the `cardId` path parameter if that card exists, otherwise responds 404 and returns null. */
suspend fun RoutingCall.existingCardId(): Int? {
    val id = parameters["cardId"]?.toIntOrNull()
    val exists = id != null && transaction { Cards.selectAll().where { Cards.id eq id }.any() }
    if (!exists) {
        respond(HttpStatusCode.NotFound)
        return null
    }
    return id
}
