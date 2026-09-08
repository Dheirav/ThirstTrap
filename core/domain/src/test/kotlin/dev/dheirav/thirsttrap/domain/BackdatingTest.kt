package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackdatingTest {

    /** IST, since that is where this is used. */
    private val ist = 330

    private fun localHourOf(millis: Long, offsetMinutes: Int): Int =
        (((millis + offsetMinutes * 60_000L) % 86_400_000L) / 3_600_000L).toInt()

    @Test
    fun `midday is midday where the user is, not midday UTC`() {
        val t = middayOf(T0, ist)
        assertEquals(12, localHourOf(t, ist))
    }

    @Test
    fun `midday lands far from both day boundaries`() {
        // The whole point: not close enough to midnight for an offset to move
        // it onto the wrong day.
        val hour = localHourOf(middayOf(T0, ist), ist)
        assertTrue(hour in 6..18)
    }

    @Test
    fun `yesterday is a full day before today`() {
        val today = middayOf(T0, ist)
        val yesterday = resolveLoggedAt(WhenLogged.YESTERDAY, T0, ist)
        assertEquals(DAY, today - yesterday)
    }

    @Test
    fun `earlier today is never in the future`() {
        // Logging at 09:00 must not be filed at midday, which has not happened.
        val nineAm = middayOf(T0, ist) - 3 * 3_600_000L
        assertEquals(nineAm, resolveLoggedAt(WhenLogged.EARLIER_TODAY, nineAm, ist))
    }

    @Test
    fun `earlier today is midday once midday has passed`() {
        val fivePm = middayOf(T0, ist) + 5 * 3_600_000L
        assertEquals(middayOf(T0, ist), resolveLoggedAt(WhenLogged.EARLIER_TODAY, fivePm, ist))
    }

    @Test
    fun `a picked day cannot be in the future`() {
        val tomorrow = T0 + DAY
        assertEquals(T0, resolveLoggedAt(WhenLogged.PICK, T0, ist, pickedDateMillis = tomorrow))
    }

    @Test
    fun `now is left exactly alone`() {
        assertEquals(T0, resolveLoggedAt(WhenLogged.NOW, T0, ist))
    }

    @Test
    fun `a backdated entry sorts after something logged that morning`() {
        // The reason for midday rather than midnight.
        val eightAm = middayOf(T0, ist) - 4 * 3_600_000L
        val backdatedToday = resolveLoggedAt(WhenLogged.EARLIER_TODAY, middayOf(T0, ist) + 6 * 3_600_000L, ist)
        assertTrue(backdatedToday > eightAm)
    }
}
