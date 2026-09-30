package com.example

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class SchedulerTest {
    private val today = LocalDate.of(2026, 1, 10)

    @Test
    fun `again resets to one day`() {
        assertEquals(Schedule(1, today.plusDays(1)), schedule(8, Grade.AGAIN, today))
    }

    @Test
    fun `hard keeps the interval`() {
        assertEquals(Schedule(4, today.plusDays(4)), schedule(4, Grade.HARD, today))
    }

    @Test
    fun `hard never goes below one day`() {
        assertEquals(Schedule(1, today.plusDays(1)), schedule(0, Grade.HARD, today))
    }

    @Test
    fun `good doubles`() {
        assertEquals(Schedule(8, today.plusDays(8)), schedule(4, Grade.GOOD, today))
    }

    @Test
    fun `easy quadruples`() {
        assertEquals(Schedule(16, today.plusDays(16)), schedule(4, Grade.EASY, today))
    }

    @Test
    fun `new card counts as a one day interval`() {
        assertEquals(Schedule(2, today.plusDays(2)), schedule(null, Grade.GOOD, today))
        assertEquals(Schedule(4, today.plusDays(4)), schedule(null, Grade.EASY, today))
        assertEquals(Schedule(1, today.plusDays(1)), schedule(null, Grade.HARD, today))
        assertEquals(Schedule(1, today.plusDays(1)), schedule(null, Grade.AGAIN, today))
    }
}
