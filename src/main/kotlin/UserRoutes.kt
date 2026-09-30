package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configureUserRoutes() {
    routing {
        post("/users") {
            val name = call.receive<CreateUserRequest>().name
            if (name.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            val id = transaction { Users.insert { it[Users.name] = name }[Users.id] }
            call.respond(HttpStatusCode.Created, UserResponse(id, name))
        }
    }
}

/** Returns the `userId` path parameter if that user exists, otherwise responds 404 and returns null. */
suspend fun RoutingCall.existingUserId(): Int? {
    val id = parameters["userId"]?.toIntOrNull()
    val exists = id != null && transaction { Users.selectAll().where { Users.id eq id }.any() }
    if (!exists) {
        respond(HttpStatusCode.NotFound)
        return null
    }
    return id
}
