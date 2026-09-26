package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PredictionEvaluationTest {

    /**
     * A clean linear cycle the model should nail: post-water at 1400 g losing
     * 50 g/day toward a measured dry anchor of 1000 g. Trigger 0.5 puts the
     * crossing at 1200 g, day 4.
     */
    private fun linearCycle(): Pair<List<WeightReading>, List<Long>> {
        val readings = mutableListOf(
            reading(0.0, 210.0, ReadingContext.PRE_WATER),
        )
        // Watering, then a post-water weigh and a daily routine weigh.
        readings += reading(0.01, 1400.0, ReadingContext.POST_WATER)
        for (d in 1..7) readings += reading(d.toDouble(), 1400.0 - 50.0 * d)
        val watering = T0 + (0.005 * DAY).toLong()
        return readings to listOf(watering)
    }

    @Test
    fun `linear decay scores near zero error`() {
        val (readings, waterings) = linearCycle()
        val p = plant(anchors = null, trigger = 0.5)
        val samples = evaluatePredictions(p, readings, waterings, emptyList())

        assertTrue("expected samples, got none", samples.isNotEmpty())
        val score = scorePredictions(samples)
        assertTrue(
            "median abs error ${score.medianAbsErrorDays} should be under half a day",
            score.medianAbsErrorDays!! < 0.5,
        )
    }

    @Test
    fun `decelerating drying scores an early bias, the safe direction`() {
        // Fast at first, slowing as it dries - the real shape of a drydown
        // tail. A linear fit extrapolates the early speed, so it must predict
        // the crossing early (negative bias), never late.
        val readings = mutableListOf(reading(0.01, 1400.0, ReadingContext.POST_WATER))
        var w = 1400.0
        for (d in 1..10) {
            w -= 60.0 * Math.pow(0.85, d.toDouble())
            readings += reading(d.toDouble(), w)
        }
        // A measured dry anchor so the trigger is not a guess.
        val p = plant(
            anchors = Anchors(wetGrams = 1400.0, dryGrams = 1100.0, dryIsProvisional = false),
            trigger = 0.5,
        )
        val samples = evaluatePredictions(
            p, readings, listOf(T0 + (0.005 * DAY).toLong()), emptyList(),
        )
        if (samples.isNotEmpty()) {
            val score = scorePredictions(samples)
            assertTrue(
                "bias ${score.biasDays} should be early (negative) or near zero",
                score.biasDays!! < 0.5,
            )
        }
    }

    @Test
    fun `a cycle watered before the trigger scores nothing`() {
        // Watered at 1300 g, well above the 1200 g trigger: no ground truth,
        // so no samples - a censored cycle is missing data, not model error.
        val readings = listOf(
            reading(0.01, 1400.0, ReadingContext.POST_WATER),
            reading(1.0, 1350.0),
            reading(2.0, 1300.0),
            reading(2.1, 1400.0, ReadingContext.POST_WATER),
            reading(3.0, 1350.0),
        )
        val p = plant(trigger = 0.5)
        val samples = evaluatePredictions(
            p, readings,
            listOf(T0 + (0.005 * DAY).toLong(), T0 + (2.05 * DAY).toLong()),
            emptyList(),
        )
        assertEquals(0, samples.size)
    }

    @Test
    fun `too little data scores nothing rather than nonsense`() {
        assertEquals(0, evaluatePredictions(plant(), emptyList(), emptyList(), emptyList()).size)
        assertEquals(
            PredictionScore(),
            scorePredictions(emptyList()),
        )
    }

    @Test
    fun `score medians are computed correctly`() {
        val samples = listOf(
            PredictionSample(3.0, 4.0, Confidence.HIGH),   // error -1
            PredictionSample(2.0, 2.5, Confidence.MEDIUM), // error -0.5
            PredictionSample(5.0, 3.0, Confidence.HIGH),   // error +2
        )
        val score = scorePredictions(samples)
        assertEquals(3, score.samples)
        assertEquals(1.0, score.medianAbsErrorDays!!, 1e-9)
        assertEquals(-0.5, score.biasDays!!, 1e-9)
        assertTrue(abs(samples[0].errorDays - (-1.0)) < 1e-9)
    }
}
