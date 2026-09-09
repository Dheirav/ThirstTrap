package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Watering and weighing are one moment. These pin the rule that decides which
 * chip the keypad opens on, because getting it wrong is silent: the reading is
 * saved, nothing looks broken, and the wet anchor just never gets recaptured.
 */
class ReadingContextSuggestionTest {

    private fun at(days: Double) = T0 + (days * DAY).toLong()

    @Test
    fun `watered and not yet weighed makes this reading the anchor`() {
        val s = assembleWeightState(
            plant(),
            listOf(reading(0.0, 1400.0, ReadingContext.CALIBRATION), reading(5.0, 1220.0)),
            listOf(at(6.0)),
            emptyList(),
            at(6.02),
        )
        assertEquals(ReadingContext.POST_WATER, suggestReadingContext(s, at(6.02)))
    }

    @Test
    fun `a reading already taken since the watering does not claim the anchor twice`() {
        val s = assembleWeightState(
            plant(),
            listOf(
                reading(0.0, 1400.0, ReadingContext.CALIBRATION),
                reading(6.1, 1450.0, ReadingContext.POST_WATER),
            ),
            listOf(at(6.0)),
            emptyList(),
            at(6.2),
        )
        assertEquals(ReadingContext.ROUTINE, suggestReadingContext(s, at(6.2)))
    }

    @Test
    fun `a watering days ago is too old to be the full mark`() {
        // The pot has visibly dried by now, so calling this reading "full"
        // would poison the scale everything else is measured against.
        assertEquals(
            ReadingContext.ROUTINE,
            suggestReadingContext(
                prediction = Prediction.NeedAnotherReading(SuppressionReason.NOT_CALIBRATED),
                lastWateredMillis = at(0.0),
                lastReadingMillis = null,
                nowMillis = at(3.0),
            ),
        )
    }

    @Test
    fun `a pot past its trigger is about to be watered`() {
        assertEquals(
            ReadingContext.PRE_WATER,
            suggestReadingContext(
                prediction = Prediction.WaterNow,
                lastWateredMillis = null,
                lastReadingMillis = null,
                nowMillis = T0,
            ),
        )
    }

    @Test
    fun `a fresh watering outranks being past the trigger`() {
        // Both are true right after watering a thirsty plant, and the reading
        // in your hand is the wet one.
        assertEquals(
            ReadingContext.POST_WATER,
            suggestReadingContext(
                prediction = Prediction.WaterNow,
                lastWateredMillis = at(0.0),
                lastReadingMillis = at(-1.0),
                nowMillis = at(0.01),
            ),
        )
    }

    @Test
    fun `the assembled state carries the watering the readings cannot show`() {
        val s = assembleWeightState(
            plant(), listOf(reading(0.0, 1400.0)), listOf(at(2.0), at(9.0)), emptyList(), at(9.5),
        )
        assertEquals(at(9.0), s.lastWateredMillis)
    }
}
