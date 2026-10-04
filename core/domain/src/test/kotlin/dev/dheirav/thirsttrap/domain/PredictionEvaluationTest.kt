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

    /**
     * Three ordinary cycles five days apart, then a hot week where the same pot
     * dries twice as fast. This is the whole product argument in one fixture:
     * the habit has not changed, so a calendar fitted to it still says five
     * days, while the pot says two. If weighing cannot win here it cannot win
     * anywhere, and if it beats a calendar ONLY here, the honest claim is
     * narrower than "better", it is "better when the weather moves".
     */
    private fun hotWeek(): Pair<List<WeightReading>, List<Long>> {
        val readings = mutableListOf<WeightReading>()
        val waterings = mutableListOf<Long>()
        for (c in 0..2) {
            val base = c * 5.0
            waterings += T0 + (base * DAY).toLong()
            readings += reading(base + 0.01, 1400.0, ReadingContext.POST_WATER)
            for (d in 1..4) readings += reading(base + d, 1400.0 - 50.0 * d)
        }
        val base = 15.0
        waterings += T0 + (base * DAY).toLong()
        readings += reading(base + 0.01, 1400.0, ReadingContext.POST_WATER)
        for (d in 1..3) readings += reading(base + d, 1400.0 - 100.0 * d)
        return readings to waterings
    }

    @Test
    fun `weighing beats the calendar when the drying rate changes`() {
        val (readings, waterings) = hotWeek()
        val score = scorePredictions(
            evaluatePredictions(plant(trigger = 0.5), readings, waterings, emptyList()),
        )

        assertTrue("nothing to compare", score.comparedSamples > 0)
        val model = score.medianAbsErrorDaysCompared!!
        val calendar = score.calendarMedianAbsErrorDays!!
        assertTrue(
            "model $model should beat calendar $calendar over the same moments",
            model < calendar,
        )
        assertTrue("advantage should be positive", score.advantageDays!! > 0.0)
    }

    @Test
    fun `the calendar is scored over exactly the same moments as the model`() {
        val (readings, waterings) = hotWeek()
        val samples = evaluatePredictions(plant(trigger = 0.5), readings, waterings, emptyList())
        val score = scorePredictions(samples)
        // Comparing a model over one set of moments against a calendar over a
        // different set is not a comparison at all, so both sides count only
        // the samples where each made a call.
        assertEquals(samples.count { it.calendarDays != null }, score.comparedSamples)
        assertTrue(score.comparedSamples <= score.samples)
    }

    @Test
    fun `no calendar opinion until it has seen enough waterings to have one`() {
        // One gap is not an interval. A calendar nobody could have written is
        // not a fair opponent, so it abstains rather than guessing.
        val (readings, waterings) = linearCycle()
        val samples = evaluatePredictions(plant(anchors = null, trigger = 0.5), readings, waterings, emptyList())
        assertTrue("expected samples", samples.isNotEmpty())
        assertTrue("a single watering cannot fit an interval", samples.all { it.calendarDays == null })
        assertEquals(0, scorePredictions(samples).comparedSamples)
        assertEquals(null, scorePredictions(samples).advantageDays)
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
