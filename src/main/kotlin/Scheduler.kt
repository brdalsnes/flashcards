package com.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
enum class Grade {
    @SerialName("again") AGAIN,
    @SerialName("hard") HARD,
    @SerialName("good") GOOD,
    @SerialName("easy") EASY,
}

data class Schedule(val intervalDays: Int, val dueDate: LocalDate)

/** A card with no review state (null previous interval) counts as having a 1 day interval. */
fun schedule(previousIntervalDays: Int?, grade: Grade, today: LocalDate): Schedule {
    val previous = previousIntervalDays ?: 1
    val interval = when (grade) {
        Grade.AGAIN -> 1
        Grade.HARD -> maxOf(1, previous)
        Grade.GOOD -> previous * 2
        Grade.EASY -> previous * 4
    }
    return Schedule(interval, today.plusDays(interval.toLong()))
}
