package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PredictionTest {

    private fun at(days: Double) = T0 + (days * DAY).toLong()

    @Test
    fun `predicts the crossing of the trigger weight`() {
        // 1400 g wet, 1000 g dry, trigger 0.5 -> 1200 g. Losing 20 g/day from
        // 1340 g on day 3 means 140 g to go: seven days.
        val readings = linearRun(4, 1400.0, 20.0)
        val segments = segmentReadings(readings, plant().anchors)
        val p = predictWatering(plant(), segments, at(3.0))
        assertTrue(p is Prediction.Eta)
        assertEquals(7.0, (p as Prediction.Eta).days, 0.2)
    }

    @Test
    fun `noise of five grams still lands within half a day`() {
        val readings = linearRun(5, 1400.0, 20.0, noise = 5.0)
        val segments = segmentReadings(readings, plant().anchors)
        val p = predictWatering(plant(), segments, at(4.0)) as Prediction.Eta
        val truth = (readings.last().grams - 1200.0) / 20.0
        assertTrue("eta ${p.days} vs truth $truth", abs(p.days - truth) < 0.5)
    }

    @Test
    fun `already past the trigger says water now`() {
        val readings = linearRun(12, 1400.0, 20.0)
        val segments = segmentReadings(readings, plant().anchors)
        assertEquals(Prediction.WaterNow, predictWatering(plant(), segments, at(11.0)))
    }

    @Test
    fun `a far-off crossing is capped rather than extrapolated`() {
        val readings = listOf(reading(0.0, 1400.0), reading(1.0, 1399.0), reading(2.0, 1398.0))
        val segments = segmentReadings(readings, plant().anchors)
        val p = predictWatering(plant(), segments, at(2.0))
        // 1 g/day is inside the dead band for a 400 g range (2 g/day), so this
        // is correctly refused rather than reported as a 198-day ETA.
        assertTrue(p is Prediction.NeedAnotherReading)
        assertEquals(
            SuppressionReason.NO_MEASURABLE_DRYING,
            (p as Prediction.NeedAnotherReading).reason,
        )
    }

    @Test
    fun `a slow but real slope is capped at two weeks`() {
        val readings = linearRun(3, 1400.0, 4.0)
        val segments = segmentReadings(readings, plant().anchors)
        val p = predictWatering(plant(), segments, at(2.0)) as Prediction.Eta
        assertTrue(p.capped)
        assertEquals(ETA_CAP_DAYS, p.days, 1e-9)
    }

    @Test
    fun `four readings earn high confidence`() {
        val segments = segmentReadings(linearRun(4, 1400.0, 20.0), plant().anchors)
        val p = predictWatering(plant(), segments, at(3.0)) as Prediction.Eta
        assertEquals(Confidence.HIGH, p.confidence)
    }

    @Test
    fun `two readings earn only medium confidence`() {
        val segments = segmentReadings(linearRun(2, 1400.0, 20.0), plant().anchors)
        val p = predictWatering(plant(), segments, at(1.0)) as Prediction.Eta
        assertEquals(Confidence.MEDIUM, p.confidence)
    }

    @Test
    fun `one reading falls back to the history prior at low confidence`() {
        val segments = segmentReadings(listOf(reading(0.0, 1400.0)), plant().anchors)
        val p = predictWatering(plant(ewma = -20.0), segments, at(0.0)) as Prediction.Eta
        assertEquals(Confidence.LOW, p.confidence)
        assertEquals(10.0, p.days, 0.1)
    }

    // ---- suppression: every row of the table in WATERING-MODEL.md §6 ----

    @Test
    fun `uncalibrated plants get no prediction`() {
        val segments = segmentReadings(linearRun(4, 1400.0, 20.0), null)
        val p = predictWatering(plant(anchors = null), segments, at(3.0))
        assertEquals(SuppressionReason.NOT_CALIBRATED, (p as Prediction.NeedAnotherReading).reason)
    }

    @Test
    fun `a plant needing recalibration gets no prediction`() {
        val segments = segmentReadings(linearRun(4, 1400.0, 20.0), plant().anchors)
        val p = predictWatering(plant(needsRecalibration = true), segments, at(3.0))
        assertEquals(
            SuppressionReason.NEEDS_RECALIBRATION,
            (p as Prediction.NeedAnotherReading).reason,
        )
    }

    @Test
    fun `no readings gives no prediction`() {
        val p = predictWatering(plant(), emptyList(), at(0.0))
        assertEquals(SuppressionReason.NO_READINGS, (p as Prediction.NeedAnotherReading).reason)
    }

    @Test
    fun `one reading and no history gives no prediction`() {
        val segments = segmentReadings(listOf(reading(0.0, 1400.0)), plant().anchors)
        val p = predictWatering(plant(), segments, at(0.0))
        assertEquals(
            SuppressionReason.ONE_READING_NO_HISTORY,
            (p as Prediction.NeedAnotherReading).reason,
        )
    }

    @Test
    fun `weight is meaningless for a water-propagation subject`() {
        val segments = segmentReadings(linearRun(4, 1400.0, 20.0), plant().anchors)
        val p = predictWatering(plant(medium = Medium.WATER), segments, at(3.0))
        assertEquals(
            SuppressionReason.WEIGHT_MEANINGLESS_FOR_MEDIUM,
            (p as Prediction.NeedAnotherReading).reason,
        )
    }

    // ---- properties ----

    @Test
    fun `eta is never negative for any monotonically decreasing series`() {
        for (rate in listOf(1.0, 5.0, 20.0, 60.0)) {
            for (days in 2..10) {
                val segments = segmentReadings(linearRun(days, 1400.0, rate), plant().anchors)
                val p = predictWatering(plant(), segments, at(days - 1.0))
                if (p is Prediction.Eta) {
                    assertTrue("rate=$rate days=$days gave ${p.days}", p.days >= 0.0)
                    assertTrue("rate=$rate days=$days not finite", p.days.isFinite())
                }
            }
        }
    }

    @Test
    fun `no input produces NaN or infinity`() {
        val inputs = listOf(
            emptyList(),
            listOf(reading(0.0, 1400.0)),
            listOf(reading(0.0, 1400.0), reading(0.0, 1400.0)),
            listOf(reading(0.0, 0.0), reading(1.0, 0.0)),
            linearRun(30, 1400.0, 0.0),
        )
        for (readings in inputs) {
            val segments = segmentReadings(readings, plant().anchors)
            val p = predictWatering(plant(ewma = -20.0), segments, at(5.0))
            if (p is Prediction.Eta) {
                assertTrue("got ${p.days} for $readings", p.days.isFinite())
            }
        }
    }

    /**
     * Real drying slows as a pot approaches dry, so a linear fit slightly
     * overestimates the remaining rate and therefore predicts dry *early*.
     * Early is the safe direction; this asserts the sign of the error.
     */
    @Test
    fun `a decelerating curve is predicted early rather than late`() {
        // Exponential decay towards the dry anchor: fast at first, slower later.
        val readings = (0..4).map { d ->
            val decay = Math.exp(-0.12 * d)
            reading(d.toDouble(), 1000.0 + 400.0 * decay)
        }
        val segments = segmentReadings(readings, plant().anchors)
        val p = predictWatering(plant(), segments, at(4.0)) as Prediction.Eta

        var w = readings.last().grams
        var trueDays = 0.0
        while (w > 1200.0 && trueDays < 100) {
            trueDays += 0.01
            w = 1000.0 + 400.0 * Math.exp(-0.12 * (4.0 + trueDays))
        }
        assertTrue("predicted ${p.days}, truth $trueDays", p.days <= trueDays + 0.01)
    }
}
