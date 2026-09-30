package com.example

import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configureDatabase() {
    val url = environment.config.propertyOrNull("flashcards.db.url")?.getString()
        ?: "jdbc:h2:mem:flashcards;DB_CLOSE_DELAY=-1"
    val database = Database.connect(url, driver = "org.h2.Driver")
    transaction(database) {
        SchemaUtils.create(Users, Decks, Cards, ReviewStates, Reviews)
    }
}
