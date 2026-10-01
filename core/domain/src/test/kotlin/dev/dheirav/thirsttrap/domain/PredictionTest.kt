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

    @Test
    fun `the eta counts down between weigh-ins`() {
        // The whole suite asserted Eta values with now sitting exactly on the
        // latest reading, so the wall-clock term was multiplied by zero every
        // time. Replacing it with `+ 0.0 * elapsedSinceLatest` left all 256
        // tests passing, which means nothing held the countdown the dashboard
        // shows on every day the pot is not weighed.
        val readings = linearRun(4, 1400.0, 20.0)
        val segments = segmentReadings(readings, plant().anchors)

        val onTheDay = predictWatering(plant(), segments, at(3.0)) as Prediction.Eta
        val twoDaysLater = predictWatering(plant(), segments, at(5.0)) as Prediction.Eta
        assertEquals(onTheDay.days - 2.0, twoDaysLater.days, 0.01)
    }

    @Test
    fun `the countdown reaching zero is water now, not a negative eta`() {
        val readings = linearRun(4, 1400.0, 20.0)
        val segments = segmentReadings(readings, plant().anchors)
        // 1340 g on day 3, 140 g above the 1200 g trigger at 20 g/day, so the
        // crossing is day 10. Standing on day 11 without having weighed it.
        assertTrue(predictWatering(plant(), segments, at(11.0)) is Prediction.WaterNow)
    }

    @Test
    fun `a watering logged after the last reading suppresses the prediction`() {
        // Segmentation only opens a new segment for a watering that falls
        // between two readings, so one logged after the most recent reading was
        // invisible and the card kept saying "needs water now" about a pot
        // watered an hour ago.
        val readings = linearRun(6, 1400.0, 40.0)
        val segments = segmentReadings(readings, plant().anchors)
        val pastTrigger = predictWatering(plant(), segments, at(5.0))
        assertTrue("fixture should be due: $pastTrigger", pastTrigger is Prediction.WaterNow)

        val watered = predictWatering(
            plant(), segments, at(5.5), lastWateredMillis = at(5.2),
        )
        assertEquals(
            SuppressionReason.WATERED_SINCE_LAST_READING,
            (watered as Prediction.NeedAnotherReading).reason,
        )
    }

    @Test
    fun `a plant that has gone is never due anything`() {
        // Found in the live diary: a flax cup recorded as dead weeks earlier
        // was still being reminded about and still said it needed water. The
        // cause was elsewhere (un-archiving re-enabled its reminders) but the
        // prediction answering at all is what made it visible, and that half
        // belongs here where it can be held.
        val readings = linearRun(6, 1400.0, 40.0)
        val segments = segmentReadings(readings, plant().anchors)
        assertTrue(
            "the fixture has to be due, or this proves nothing",
            predictWatering(plant(), segments, at(5.0)) is Prediction.WaterNow,
        )

        listOf(PlantStatus.DEAD, PlantStatus.GIVEN_AWAY).forEach { status ->
            val p = predictWatering(
                plant().copy(status = status), segments, at(5.0),
            )
            assertEquals(
                "$status should suppress",
                SuppressionReason.PLANT_IS_GONE,
                (p as Prediction.NeedAnotherReading).reason,
            )
        }
    }

    @Test
    fun `dormant and unknown are still looked after`() {
        // Dormant is the one worth stating: a plant resting over winter still
        // wants water occasionally, and weighing is how you find out how much
        // less. Unknown is the shrug value, and refusing to care for a plant
        // on the strength of a shrug is worse than the alternative.
        val readings = linearRun(4, 1400.0, 20.0)
        val segments = segmentReadings(readings, plant().anchors)
        listOf(PlantStatus.ACTIVE, PlantStatus.DORMANT, PlantStatus.UNKNOWN).forEach { status ->
            val p = predictWatering(plant().copy(status = status), segments, at(3.0))
            assertTrue("$status should still predict, got $p", p is Prediction.Eta)
        }
    }

    @Test
    fun `a watering before the last reading is the segment boundary, not a suppression`() {
        // The ordinary case must not be swallowed by the check above: here the
        // reading is the newer fact and the model should use it.
        val readings = linearRun(4, 1400.0, 20.0)
        val segments = segmentReadings(readings, plant().anchors)
        val p = predictWatering(plant(), segments, at(3.0), lastWateredMillis = at(0.5))
        assertTrue("got $p", p is Prediction.Eta)
    }
}
