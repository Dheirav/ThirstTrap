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

    // --- The weight-jump rule, added 2026-10-20 after a real diary lost two
    // --- waterings: the chip said ROUTINE because nothing was logged, and the
    // --- old rule only ever looked at the chip.

    @Test
    fun `a pot that gained a lot of weight is a watering, whatever the chip says`() {
        // The fig: 223 g to 271 g on a range of about 73 g, so a 48 g rise
        // against a threshold near 6 g.
        assertTrue(
            impliesUnloggedWatering(
                ReadingContext.ROUTINE, null, now,
                grams = 271.0, previousGrams = 223.0, rangeGrams = 73.0,
            ),
        )
    }

    @Test
    fun `ordinary drying is not a watering`() {
        // The pot gets lighter, not heavier. The sign matters.
        assertFalse(
            impliesUnloggedWatering(
                ReadingContext.ROUTINE, null, now,
                grams = 223.0, previousGrams = 228.0, rangeGrams = 73.0,
            ),
        )
    }

    @Test
    fun `a rise smaller than the segmentation threshold is left alone`() {
        // Under 8 percent of the range. Scale drift, a wet saucer, a different
        // time of day. Inventing a watering here would be worse than missing
        // one, because the diary is the thing being trusted.
        assertFalse(
            impliesUnloggedWatering(
                ReadingContext.ROUTINE, null, now,
                grams = 228.0, previousGrams = 223.0, rangeGrams = 73.0,
            ),
        )
    }

    @Test
    fun `a watering already on the log explains any rise`() {
        // Logged five minutes ago, so this is the post-water weigh and the
        // event exists. Backfilling here would double the watering.
        assertFalse(
            impliesUnloggedWatering(
                ReadingContext.ROUTINE, now - 300_000L, now,
                grams = 271.0, previousGrams = 223.0, rangeGrams = 73.0,
            ),
        )
    }

    @Test
    fun `an uncalibrated pot says nothing`() {
        // No range means no way to know whether 50 g is a drink or a draught.
        assertFalse(
            impliesUnloggedWatering(
                ReadingContext.ROUTINE, null, now,
                grams = 271.0, previousGrams = 223.0, rangeGrams = null,
            ),
        )
        assertFalse(
            impliesUnloggedWatering(
                ReadingContext.ROUTINE, null, now,
                grams = 271.0, previousGrams = null, rangeGrams = 73.0,
            ),
        )
    }

    @Test
    fun `the first reading of a plant is not a watering`() {
        assertFalse(risesLikeAWatering(271.0, previousGrams = null, rangeGrams = 73.0))
    }

    @Test
    fun `the threshold is exactly segmentation's, so the two halves agree`() {
        // A jump that starts a new drying cycle is a jump that implies a
        // watering. If these ever diverge, the model and the diary disagree
        // about the same reading.
        val range = 100.0
        val exactly = SEGMENT_JUMP_FRACTION * range
        assertTrue(risesLikeAWatering(200.0 + exactly, 200.0, range))
        assertFalse(risesLikeAWatering(200.0 + exactly - 0.01, 200.0, range))
    }
}
