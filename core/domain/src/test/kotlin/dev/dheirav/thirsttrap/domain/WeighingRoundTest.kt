package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeighingRoundTest {

    private val roundStart = 1_000_000L

    private fun r(id: String, at: Long, g: Double, excluded: Boolean = false) =
        WeightReading(
            id = id, plantId = "p", timestampMillis = at, grams = g,
            context = ReadingContext.ROUTINE, excluded = excluded,
        )

    @Test
    fun theReadingJustTakenIsNotAlsoItsOwnPredecessor() {
        // The bug this exists for. Weigh a pot that was last weighed two days
        // ago, and "Last" must still say 500, not repeat the 480 just entered.
        val readings = listOf(
            r("old", roundStart - (2 * MILLIS_PER_DAY).toLong(), 500.0),
            r("new", roundStart + 5_000, 480.0),
        )
        assertEquals("new", readingThisRound(readings, roundStart)?.id)
        assertEquals("old", previousReading(readings, roundStart)?.id)
    }

    @Test
    fun aFirstEverWeighingHasNothingToCompareAgainst() {
        // And so the round must not claim it does, which is what drove the
        // "every pot has a previous weight" hint on a first round.
        val readings = listOf(r("new", roundStart + 5_000, 480.0))
        assertEquals("new", readingThisRound(readings, roundStart)?.id)
        assertNull(previousReading(readings, roundStart))
    }

    @Test
    fun theDeltaIsNonZeroWhenTheWeightChanged() {
        val readings = listOf(
            r("old", roundStart - MILLIS_PER_DAY.toLong(), 500.0),
            r("new", roundStart + 1_000, 480.0),
        )
        val now = readingThisRound(readings, roundStart)!!.grams
        val before = previousReading(readings, roundStart)!!.grams
        assertEquals(-20.0, now - before, 1e-9)
    }

    @Test
    fun reweighingToFixATypoReplacesTheNumberRatherThanBeingIgnored() {
        val readings = listOf(
            r("old", roundStart - MILLIS_PER_DAY.toLong(), 500.0),
            r("typo", roundStart + 1_000, 4800.0),
            r("fixed", roundStart + 9_000, 480.0),
        )
        assertEquals("fixed", readingThisRound(readings, roundStart)?.id)
        // and the thing it is compared against does not move
        assertEquals("old", previousReading(readings, roundStart)?.id)
    }

    @Test
    fun anExcludedReadingIsNotSomethingToMeasureChangeAgainst() {
        val readings = listOf(
            r("good", roundStart - (3 * MILLIS_PER_DAY).toLong(), 500.0),
            r("bad", roundStart - MILLIS_PER_DAY.toLong(), 9.0, excluded = true),
            r("new", roundStart + 1_000, 480.0),
        )
        assertEquals("good", previousReading(readings, roundStart)?.id)
    }

    @Test
    fun orderOfTheInputDoesNotMatter() {
        val readings = listOf(
            r("new", roundStart + 1_000, 480.0),
            r("old", roundStart - MILLIS_PER_DAY.toLong(), 500.0),
        )
        assertEquals("old", previousReading(readings, roundStart)?.id)
        assertEquals("new", readingThisRound(readings, roundStart)?.id)
    }

    @Test
    fun aPotNotWeighedThisRoundHasNoNowValue() {
        val readings = listOf(r("old", roundStart - MILLIS_PER_DAY.toLong(), 500.0))
        assertNull(readingThisRound(readings, roundStart))
        assertEquals("old", previousReading(readings, roundStart)?.id)
    }
}
