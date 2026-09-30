package com.example

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class StudyRoutesTest {
    private val day = LocalDate.of(2026, 3, 1)

    // --- clock injection ---

    @Test
    fun `the study routes use the injected clock`() = testApplication {
        testConfig(TestClock(day))
        val client = jsonClient()
        val user = client.newUser()
        val card = client.newCard(client.newDeck(), "hund")

        val state = client.review(user, card, "good").body<ReviewStateResponse>()
        assertEquals("2026-03-03", state.dueDate)
    }

    // --- next due card ---

    @Test
    fun `a new card is due and returned alone in an array`() = testApplication {
        testConfig(TestClock(day))
        val client = jsonClient()
        val user = client.newUser()
        val deck = client.newDeck()
        val first = client.newCard(deck, "a")
        client.newCard(deck, "b")

        val next = client.nextCards(user, deck)
        assertEquals(listOf(first), next.map { it.id })
        assertEquals("a", next.single().front)
    }

    @Test
    fun `a due reviewed card comes before new cards`() = testApplication {
        val clock = TestClock(day)
        testConfig(clock)
        val client = jsonClient()
        val user = client.newUser()
        val deck = client.newDeck()
        client.newCard(deck, "new")
        val reviewed = client.newCard(deck, "reviewed")
        client.review(user, reviewed, "good") // due in 2 days

        clock.date = day.plusDays(2)
        assertEquals(listOf(reviewed), client.nextCards(user, deck).map { it.id })
    }

    @Test
    fun `among reviewed cards the earliest due date comes first`() = testApplication {
        val clock = TestClock(day)
        testConfig(clock)
        val client = jsonClient()
        val user = client.newUser()
        val deck = client.newDeck()
        val later = client.newCard(deck, "later")
        val sooner = client.newCard(deck, "sooner")
        client.review(user, later, "good") // due day+2
        client.review(user, sooner, "again") // due day+1

        clock.date = day.plusDays(5)
        assertEquals(listOf(sooner), client.nextCards(user, deck).map { it.id })
    }

    @Test
    fun `a reviewed card is skipped until it is due`() = testApplication {
        val clock = TestClock(day)
        testConfig(clock)
        val client = jsonClient()
        val user = client.newUser()
        val deck = client.newDeck()
        val card = client.newCard(deck, "a")
        client.review(user, card, "good") // due day+2

        assertEquals(emptyList(), client.nextCards(user, deck))
        clock.date = day.plusDays(1)
        assertEquals(emptyList(), client.nextCards(user, deck))
        clock.date = day.plusDays(2)
        assertEquals(listOf(card), client.nextCards(user, deck).map { it.id })
    }

    @Test
    fun `nothing due returns 200 with an empty array`() = testApplication {
        testConfig(TestClock(day))
        val client = jsonClient()
        val user = client.newUser()
        val deck = client.newDeck()

        val response = client.get("/users/$user/decks/$deck/next")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(emptyList(), response.body<List<CardResponse>>())
    }

    @Test
    fun `unknown user or deck returns 404`() = testApplication {
        testConfig(TestClock(day))
        val client = jsonClient()
        val user = client.newUser()
        val deck = client.newDeck()

        assertEquals(HttpStatusCode.NotFound, client.get("/users/999/decks/$deck/next").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/users/$user/decks/999/next").status)
    }

    @Test
    fun `two users have independent schedules for the same card`() = testApplication {
        testConfig(TestClock(day))
        val client = jsonClient()
        val ada = client.newUser("Ada")
        val grace = client.newUser("Grace")
        val deck = client.newDeck()
        val card = client.newCard(deck, "a")

        client.review(ada, card, "good")

        assertEquals(emptyList(), client.nextCards(ada, deck))
        assertEquals(listOf(card), client.nextCards(grace, deck).map { it.id })
    }

    // --- submit review ---

    @Test
    fun `a first review creates the state and returns it`() = testApplication {
        testConfig(TestClock(day))
        val client = jsonClient()
        val user = client.newUser()
        val card = client.newCard(client.newDeck(), "a")

        val response = client.review(user, card, "good")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ReviewStateResponse(user, card, "2026-03-03", 2), response.body())
        assertEquals(1, transaction { ReviewStates.selectAll().count() }.toInt())
    }

    @Test
    fun `a later review updates the same state row`() = testApplication {
        testConfig(TestClock(day))
        val client = jsonClient()
        val user = client.newUser()
        val card = client.newCard(client.newDeck(), "a")

        assertEquals(2, client.review(user, card, "good").body<ReviewStateResponse>().intervalDays)
        assertEquals(4, client.review(user, card, "good").body<ReviewStateResponse>().intervalDays)
        assertEquals(1, transaction { ReviewStates.selectAll().count() }.toInt())
    }

    @Test
    fun `each grade gives the specified interval from a 4 day interval`() = testApplication {
        testConfig(TestClock(day))
        val client = jsonClient()
        val deck = client.newDeck()
        val expected = mapOf("again" to 1, "hard" to 4, "good" to 8, "easy" to 16)

        for ((grade, interval) in expected) {
            val user = client.newUser(grade)
            val card = client.newCard(deck, grade)
            client.review(user, card, "good") // 2 days
            client.review(user, card, "good") // 4 days
            val state = client.review(user, card, grade).body<ReviewStateResponse>()
            assertEquals(interval, state.intervalDays, "grade $grade")
            assertEquals(day.plusDays(interval.toLong()).toString(), state.dueDate, "grade $grade")
        }
    }

    @Test
    fun `an invalid or missing grade returns 400 and changes nothing`() = testApplication {
        testConfig(TestClock(day))
        val client = jsonClient()
        val user = client.newUser()
        val card = client.newCard(client.newDeck(), "a")
        val path = "/users/$user/cards/$card/review"

        assertEquals(HttpStatusCode.BadRequest, client.postRawJson(path, """{"grade": "banana"}""").status)
        assertEquals(HttpStatusCode.BadRequest, client.postRawJson(path, """{"grade": "GOOD"}""").status)
        assertEquals(HttpStatusCode.BadRequest, client.postRawJson(path, """{}""").status)
        assertNoReviewData()
    }

    @Test
    fun `an unknown user or card returns 404 and changes nothing`() = testApplication {
        testConfig(TestClock(day))
        val client = jsonClient()
        val user = client.newUser()
        val card = client.newCard(client.newDeck(), "a")

        assertEquals(HttpStatusCode.NotFound, client.review(999, card, "good").status)
        assertEquals(HttpStatusCode.NotFound, client.review(user, 999, "good").status)
        assertNoReviewData()
    }

    private fun assertNoReviewData() {
        transaction {
            assertEquals(0, ReviewStates.selectAll().count())
            assertEquals(0, Reviews.selectAll().count())
        }
    }
}
