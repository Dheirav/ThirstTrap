package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeighAsAssessmentTest {

    @Test
    fun `a weigh-in newer than every event wins`() {
        assertEquals(
            java.lang.Long.valueOf(T0 + 5 * DAY),
            latestAssessmentMillis(listOf(T0, T0 + 2 * DAY), listOf(T0 + 5 * DAY)),
        )
    }

    @Test
    fun `an event newer than every weigh-in wins`() {
        assertEquals(
            java.lang.Long.valueOf(T0 + 7 * DAY),
            latestAssessmentMillis(listOf(T0 + 7 * DAY), listOf(T0 + 3 * DAY)),
        )
    }

    @Test
    fun `weigh-ins alone are enough - the friction-report case`() {
        // Daily weighing, no watered or checked events at all: the clock must
        // count from the weigh, not sit overdue forever.
        assertEquals(
            java.lang.Long.valueOf(T0 + 18 * DAY),
            latestAssessmentMillis(emptyList(), listOf(T0 + 17 * DAY, T0 + 18 * DAY)),
        )
    }

    @Test
    fun `nothing at all means null, not zero`() {
        assertNull(latestAssessmentMillis(emptyList(), emptyList()))
    }
}
