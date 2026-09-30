package com.example

import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class UserRoutesTest {

    @Test
    fun `creating a user returns 201 with a generated id`() = testApplication {
        testConfig()
        val client = jsonClient()

        val first = client.postRawJson("/users", """{"name": "Ada"}""")
        assertEquals(HttpStatusCode.Created, first.status)
        val ada = first.body<UserResponse>()
        assertEquals("Ada", ada.name)

        val second = client.postRawJson("/users", """{"name": "Grace"}""").body<UserResponse>()
        assertNotEquals(ada.id, second.id)
    }

    @Test
    fun `blank or missing name is rejected`() = testApplication {
        testConfig()
        val client = jsonClient()

        assertEquals(HttpStatusCode.BadRequest, client.postRawJson("/users", """{"name": "   "}""").status)
        assertEquals(HttpStatusCode.BadRequest, client.postRawJson("/users", """{"name": ""}""").status)
        assertEquals(HttpStatusCode.BadRequest, client.postRawJson("/users", """{}""").status)
    }
}
