package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A big pot cannot go on a kitchen scale, which looked like it ruled the whole
 * feature out for floor plants. It does not, and this pins why.
 *
 * Depletion is (wet - now) / (wet - dry) and the ETA is remaining over slope.
 * Multiply every reading by the same constant and both are unchanged, so a
 * measurement that is only *proportional* to the pot's weight carries the same
 * information as the pot's weight. Tip the pot onto one edge with the scale
 * under that edge, or stand it on a bathroom scale reading in 100 g steps, and
 * the model works, provided the method never changes mid-cycle.
 *
 * That proviso is the whole risk, and it is the same failure as a repot: the
 * anchors describe a measurement that no longer exists.
 */
class PartialWeightInvarianceTest {

    private fun run(scale: Double, nowDays: Double, stepGrams: Double = 1.0): WeightState {
        val readings = (0..4).map {
            reading(it.toDouble(), (1400.0 - 40.0 * it) * scale)
        }
        return assembleWeightState(
            plant(anchors = Anchors(1400.0 * scale, 1000.0 * scale, dryIsProvisional = false))
                .copy(weighingStepGrams = stepGrams),
            readings,
            emptyList(),
            emptyList(),
            T0 + (nowDays * DAY).toLong(),
        )
    }

    @Test
    fun `a partial measurement predicts the same day as the full one`() {
        val full = run(1.0, 4.0)
        // 0.38: tipping a pot onto one edge reads roughly a third of its weight.
        val tilted = run(0.38, 4.0)

        assertTrue("full should predict, got ${full.prediction}", full.prediction is Prediction.Eta)
        assertTrue("tilted should predict, got ${tilted.prediction}", tilted.prediction is Prediction.Eta)
        assertEquals(
            (full.prediction as Prediction.Eta).days,
            (tilted.prediction as Prediction.Eta).days,
            1e-6,
        )
        assertEquals(full.depletion!!, tilted.depletion!!, 1e-9)
    }

    @Test
    fun `the same holds across every fraction a real method produces`() {
        val reference = run(1.0, 3.0)
        // 1.0 is the whole pot on a kitchen scale; 0.6 to 0.25 is the range a
        // tip-onto-one-edge reading falls in, depending how far it is tipped.
        for (scale in listOf(1.0, 0.6, 0.38, 0.25, 0.1)) {
            val s = run(scale, 3.0)
            assertTrue("scale=$scale gave ${s.prediction}", s.prediction is Prediction.Eta)
            assertEquals(
                "scale=$scale",
                (reference.prediction as Prediction.Eta).days,
                (s.prediction as Prediction.Eta).days,
                1e-6,
            )
        }
    }

    @Test
    fun `invariance stops where the absolute noise floor takes over`() {
        // minMeaningfulSlope is max(MIN_SLOPE_GRAMS_PER_DAY, 0.5% of range),
        // and that 1 g is a statement about the *instrument*: a kitchen scale
        // reads to about a gram, so a sub-gram daily slope cannot be told from
        // its own noise. It is not a statement about the pot, which is why the
        // relative part of the model scales and this part does not.
        //
        // At 2% of the pot the measured range is 8 g and the daily loss 0.8 g,
        // under the floor, so the app declines instead of fitting noise. That
        // is the right call for a kitchen scale and the wrong floor entirely
        // for a bathroom scale reading in 100 g steps, which needs a far
        // higher one. See the weighing-method note in docs/SHARING.md.
        val s = run(0.02, 3.0)
        assertTrue(
            "expected a refusal, got ${s.prediction}",
            s.prediction is Prediction.NeedAnotherReading,
        )
        assertEquals(
            SuppressionReason.NO_MEASURABLE_DRYING,
            (s.prediction as Prediction.NeedAnotherReading).reason,
        )
    }

    @Test
    fun `a coarse scale raises the floor instead of fitting its own rounding`() {
        // 40 g a day is a real slope on a kitchen scale and is indistinguishable
        // from rounding on a bathroom scale that moves in 100 g steps. The pot
        // and the readings are identical; only the instrument differs.
        val kitchen = run(1.0, 3.0, stepGrams = 1.0)
        assertTrue("got ${kitchen.prediction}", kitchen.prediction is Prediction.Eta)

        val bathroom = run(1.0, 3.0, stepGrams = 100.0)
        assertTrue(
            "a 100 g step should not support a 40 g/day slope, got ${bathroom.prediction}",
            bathroom.prediction is Prediction.NeedAnotherReading,
        )
        assertEquals(
            SuppressionReason.NO_MEASURABLE_DRYING,
            (bathroom.prediction as Prediction.NeedAnotherReading).reason,
        )
    }

    @Test
    fun `a coarse scale still works once the pot moves enough water`() {
        // The same bathroom scale under a 20 kg pot holding 6 kg of available
        // water and losing 400 g a day: four steps a day, which it can
        // genuinely see, and still short of its trigger on day 4.
        val readings = (0..4).map { reading(it.toDouble(), 20_000.0 - 400.0 * it) }
        val s = assembleWeightState(
            plant(anchors = Anchors(20_000.0, 14_000.0, dryIsProvisional = false))
                .copy(weighingStepGrams = 100.0),
            readings,
            emptyList(),
            emptyList(),
            T0 + 3 * DAY,
        )
        assertTrue("got ${s.prediction}", s.prediction is Prediction.Eta)
    }
}
