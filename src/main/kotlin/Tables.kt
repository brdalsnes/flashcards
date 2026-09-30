package com.example

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestamp

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    override val primaryKey = PrimaryKey(id)
}

object Decks : Table("decks") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    override val primaryKey = PrimaryKey(id)
}

object Cards : Table("cards") {
    val id = integer("id").autoIncrement()
    val deckId = reference("deck_id", Decks.id)
    val front = text("front")
    val back = text("back")
    override val primaryKey = PrimaryKey(id)
}

object ReviewStates : Table("review_states") {
    val userId = reference("user_id", Users.id)
    val cardId = reference("card_id", Cards.id)
    val dueDate = date("due_date")
    val intervalDays = integer("interval_days")
    override val primaryKey = PrimaryKey(userId, cardId)
}

object Reviews : Table("reviews") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id)
    val cardId = reference("card_id", Cards.id)
    val grade = enumerationByName<Grade>("grade", 16)
    val reviewedAt = timestamp("reviewed_at")
    val resultingIntervalDays = integer("resulting_interval_days")
    override val primaryKey = PrimaryKey(id)
}
