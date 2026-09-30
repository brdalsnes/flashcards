package com.example

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeckRoutesTest {

    private suspend fun HttpClient.createDeck(name: String = "Norwegian verbs"): DeckResponse =
        postRawJson("/decks", """{"name": "$name"}""").body()

    @Test
    fun `creating a deck returns 201 with a generated id`() = testApplication {
        testConfig()
        val client = jsonClient()

        val response = client.postRawJson("/decks", """{"name": "Norwegian verbs"}""")
        assertEquals(HttpStatusCode.Created, response.status)
        val deck = response.body<DeckResponse>()
        assertEquals("Norwegian verbs", deck.name)
        assertTrue(deck.id > 0)
    }

    @Test
    fun `blank or missing deck name is rejected`() = testApplication {
        testConfig()
        val client = jsonClient()

        assertEquals(HttpStatusCode.BadRequest, client.postRawJson("/decks", """{"name": " "}""").status)
        assertEquals(HttpStatusCode.BadRequest, client.postRawJson("/decks", """{}""").status)
    }

    @Test
    fun `adding a card returns 201 with its id and deck id`() = testApplication {
        testConfig()
        val client = jsonClient()
        val deck = client.createDeck()

        val response = client.postRawJson("/decks/${deck.id}/cards", """{"front": "hund", "back": "dog"}""")
        assertEquals(HttpStatusCode.Created, response.status)
        val card = response.body<CardResponse>()
        assertEquals(deck.id, card.deckId)
        assertEquals("hund", card.front)
        assertEquals("dog", card.back)
    }

    @Test
    fun `adding a card to an unknown deck returns 404 and creates nothing`() = testApplication {
        testConfig()
        val client = jsonClient()

        assertEquals(
            HttpStatusCode.NotFound,
            client.postRawJson("/decks/999/cards", """{"front": "hund", "back": "dog"}""").status,
        )
        assertEquals(HttpStatusCode.NotFound, client.get("/decks/999/cards").status)
    }

    @Test
    fun `a blank card side is rejected`() = testApplication {
        testConfig()
        val client = jsonClient()
        val deck = client.createDeck()

        assertEquals(
            HttpStatusCode.BadRequest,
            client.postRawJson("/decks/${deck.id}/cards", """{"front": "", "back": "dog"}""").status,
        )
        assertEquals(
            HttpStatusCode.BadRequest,
            client.postRawJson("/decks/${deck.id}/cards", """{"front": "hund"}""").status,
        )
        assertEquals(emptyList(), client.get("/decks/${deck.id}/cards").body<List<CardResponse>>())
    }

    @Test
    fun `an empty deck lists no cards`() = testApplication {
        testConfig()
        val client = jsonClient()
        val deck = client.createDeck()

        val response = client.get("/decks/${deck.id}/cards")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(emptyList(), response.body<List<CardResponse>>())
    }

    @Test
    fun `cards are listed oldest first and only for their own deck`() = testApplication {
        testConfig()
        val client = jsonClient()
        val deck = client.createDeck("Deck one")
        val other = client.createDeck("Deck two")
        client.postRawJson("/decks/${deck.id}/cards", """{"front": "a", "back": "1"}""")
        client.postRawJson("/decks/${other.id}/cards", """{"front": "x", "back": "9"}""")
        client.postRawJson("/decks/${deck.id}/cards", """{"front": "b", "back": "2"}""")
        client.postRawJson("/decks/${deck.id}/cards", """{"front": "c", "back": "3"}""")

        val cards = client.get("/decks/${deck.id}/cards").body<List<CardResponse>>()
        assertEquals(listOf("a", "b", "c"), cards.map { it.front })
        assertTrue(cards.all { it.deckId == deck.id })
    }
}
