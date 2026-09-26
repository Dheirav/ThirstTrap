package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperimentTest {

    private val ist = 330

    @Test
    fun `the start day is day 1, not day 0`() {
        assertEquals(1, experimentDayNumber(T0, ist, T0))
        assertEquals(1, experimentDayNumber(T0, ist, T0 + DAY / 2))
    }

    @Test
    fun `days count in civil days, not 24h blocks`() {
        // Started 23:00 IST; two hours later it is the next civil day, so
        // "day 2" even though only two hours have passed.
        // Midnight of T0's IST civil day, in UTC millis, plus 23 IST hours.
        val elevenPmIst = T0 - ((T0 + ist * 60_000L) % DAY) + 23 * 3_600_000L
        assertEquals(2, experimentDayNumber(elevenPmIst, ist, elevenPmIst + 2 * 3_600_000L))
    }

    @Test
    fun `day five is day five`() {
        assertEquals(5, experimentDayNumber(T0, ist, T0 + 4 * DAY))
    }

    @Test
    fun `concluded is a state, not a date check by callers`() {
        val e = Experiment("e1", "flax", "banana vs plain", T0, ist)
        assertFalse(e.isConcluded)
        assertTrue(e.copy(concludedAtMillis = T0 + 5 * DAY, conclusion = "banana won").isConcluded)
    }
}
