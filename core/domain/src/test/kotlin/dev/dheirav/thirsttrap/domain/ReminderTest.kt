package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderTest {

    private fun reminder(
        due: Long,
        enabled: Boolean = true,
        snoozed: Long? = null,
    ) = Reminder(
        id = "r1", plantId = "p1", kind = ReminderKind.CHECK,
        nextDueAtMillis = due, enabled = enabled, snoozedUntilMillis = snoozed,
    )

    @Test
    fun `an explicit interval wins over everything`() {
        assertEquals(
            3,
            resolveIntervalDays(3, Prediction.Eta(9.0, Confidence.HIGH, false), 12.0),
        )
    }

    @Test
    fun `a measured prediction beats the logged average`() {
        assertEquals(
            9,
            resolveIntervalDays(null, Prediction.Eta(9.4, Confidence.HIGH, false), 12.0),
        )
    }

    @Test
    fun `a capped prediction is not trusted as an interval`() {
        // ">14 days" is a refusal to extrapolate, not a measurement.
        assertEquals(12, resolveIntervalDays(null, Prediction.Eta(14.0, Confidence.LOW, true), 12.0))
    }

    @Test
    fun `the logged average is used when there is no prediction`() {
        assertEquals(8, resolveIntervalDays(null, null, 8.6))
    }

    @Test
    fun `a brand-new plant falls back to a week`() {
        assertEquals(DEFAULT_CHECK_INTERVAL_DAYS, resolveIntervalDays(null, null, null))
    }

    @Test
    fun `an interval is never zero or negative`() {
        assertEquals(1, resolveIntervalDays(0, null, null))
        assertEquals(1, resolveIntervalDays(null, null, 0.2))
    }

    @Test
    fun `next due counts from the last assessment`() {
        val last = T0
        assertEquals(last + 5 * DAY, computeNextDue(last, 5, T0 + DAY))
    }

    @Test
    fun `a plant never assessed is due one interval from now`() {
        assertEquals(T0 + 7 * DAY, computeNextDue(null, 7, T0))
    }

    @Test
    fun `due means enabled, past its time, and not snoozed`() {
        assertTrue(reminder(T0).isDue(T0 + DAY))
        assertFalse(reminder(T0 + DAY).isDue(T0))
        assertFalse(reminder(T0, enabled = false).isDue(T0 + DAY))
        assertFalse(reminder(T0, snoozed = T0 + 2 * DAY).isDue(T0 + DAY))
        assertTrue(reminder(T0, snoozed = T0 + DAY).isDue(T0 + 2 * DAY))
    }
}
