package com.example

import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReviewLogTest {
    private val day = LocalDate.of(2026, 3, 1)

    @Test
    fun `every review appends an entry with its grade and resulting interval`() = testApplication {
        testConfig(TestClock(day))
        val client = jsonClient()
        val user = client.newUser()
        val card = client.newCard(client.newDeck(), "a")

        client.review(user, card, "good") // 2
        client.review(user, card, "easy") // 8
        client.review(user, card, "again") // 1

        val log = transaction {
            Reviews.selectAll()
                .where { (Reviews.userId eq user) and (Reviews.cardId eq card) }
                .orderBy(Reviews.id, SortOrder.ASC)
                .map { it[Reviews.grade] to it[Reviews.resultingIntervalDays] }
        }
        assertEquals(listOf(Grade.GOOD to 2, Grade.EASY to 8, Grade.AGAIN to 1), log)
    }

    @Test
    fun `a failure after the state write rolls back both the state and the log`() = testApplication {
        testConfig(TestClock(day))
        val client = jsonClient()
        val user = client.newUser()
        val card = client.newCard(client.newDeck(), "a")
        val now = Instant.parse("2026-03-01T10:00:00Z")

        // New card: nothing may persist.
        assertFailsWith<IllegalStateException> {
            recordReview(user, card, Grade.GOOD, now, day, afterStateWrite = { error("boom") })
        }
        transaction {
            assertEquals(0, ReviewStates.selectAll().count())
            assertEquals(0, Reviews.selectAll().count())
        }

        // Existing state: it must keep its previous value and the log must not grow.
        recordReview(user, card, Grade.GOOD, now, day) // interval 2
        assertFailsWith<IllegalStateException> {
            recordReview(user, card, Grade.EASY, now, day, afterStateWrite = { error("boom") })
        }
        transaction {
            assertEquals(2, ReviewStates.selectAll().single()[ReviewStates.intervalDays])
            assertEquals(1, Reviews.selectAll().count())
        }
    }
}
