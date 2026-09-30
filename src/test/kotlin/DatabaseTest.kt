package com.example

import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DatabaseTest {

    @Test
    fun `tables are created on startup`() = testApplication {
        testConfig()
        startApplication()
        transaction {
            assertEquals(0, Users.selectAll().count())
            assertEquals(0, Decks.selectAll().count())
            assertEquals(0, Cards.selectAll().count())
            assertEquals(0, ReviewStates.selectAll().count())
            assertEquals(0, Reviews.selectAll().count())
        }
    }

    @Test
    fun `a user has at most one review state per card`() = testApplication {
        testConfig()
        startApplication()
        transaction {
            val userId = Users.insert { it[name] = "Ada" }[Users.id]
            val deckId = Decks.insert { it[name] = "Deck" }[Decks.id]
            val cardId = Cards.insert {
                it[Cards.deckId] = deckId
                it[front] = "hund"
                it[back] = "dog"
            }[Cards.id]
            ReviewStates.insert {
                it[ReviewStates.userId] = userId
                it[ReviewStates.cardId] = cardId
                it[dueDate] = LocalDate.of(2026, 1, 1)
                it[intervalDays] = 1
            }
            assertFailsWith<Exception> {
                ReviewStates.insert {
                    it[ReviewStates.userId] = userId
                    it[ReviewStates.cardId] = cardId
                    it[dueDate] = LocalDate.of(2026, 1, 2)
                    it[intervalDays] = 2
                }
            }
        }
    }
}
