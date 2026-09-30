package com.example

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlin.test.*

@Serializable
private data class Ping(val message: String)

class SerializationTest {

    @Test
    fun `serializable responses are returned as json`() = testApplication {
        testConfig()
        application {
            routing {
                get("/test-ping") { call.respond(Ping("pong")) }
            }
        }
        val response = client.get("/test-ping")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
        assertEquals("""{"message":"pong"}""", response.bodyAsText())
    }
}
