package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnloggedWateringTest {

    private val now = T0 + 30 * DAY

    @Test
    fun `post-water reading with no watering ever implies one`() {
        assertTrue(impliesUnloggedWatering(ReadingContext.POST_WATER, null, now))
    }

    @Test
    fun `post-water reading days after the last logged watering implies one`() {
        assertTrue(impliesUnloggedWatering(ReadingContext.POST_WATER, now - 5 * DAY, now))
    }

    @Test
    fun `post-water reading just after a logged watering implies nothing`() {
        // The normal flow: log the watering, weigh right after.
        assertFalse(impliesUnloggedWatering(ReadingContext.POST_WATER, now - 60_000L, now))
    }

    @Test
    fun `the window edge matches the anchor rule`() {
        assertFalse(
            impliesUnloggedWatering(ReadingContext.POST_WATER, now - POST_WATER_WINDOW_MILLIS, now),
        )
        assertTrue(
            impliesUnloggedWatering(
                ReadingContext.POST_WATER,
                now - POST_WATER_WINDOW_MILLIS - 1,
                now,
            ),
        )
    }

    @Test
    fun `other contexts never imply a watering`() {
        for (c in listOf(ReadingContext.ROUTINE, ReadingContext.PRE_WATER, ReadingContext.CALIBRATION)) {
            assertFalse(impliesUnloggedWatering(c, null, now))
            assertFalse(impliesUnloggedWatering(c, now - 5 * DAY, now))
        }
    }
}
