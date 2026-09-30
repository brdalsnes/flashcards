package com.example

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(val name: String? = null)

@Serializable
data class UserResponse(val id: Int, val name: String)

@Serializable
data class CreateDeckRequest(val name: String? = null)

@Serializable
data class DeckResponse(val id: Int, val name: String)

@Serializable
data class CreateCardRequest(val front: String? = null, val back: String? = null)

@Serializable
data class CardResponse(val id: Int, val deckId: Int, val front: String, val back: String)

@Serializable
data class ReviewRequest(val grade: Grade? = null)

@Serializable
data class ReviewStateResponse(val userId: Int, val cardId: Int, val dueDate: String, val intervalDays: Int)
