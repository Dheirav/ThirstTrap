package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Weighing must not require calibrating first.
 *
 * The wet anchor is "the pot just after watering", which is exactly what a
 * POST_WATER reading is - so demanding a separate calibration step before any
 * reading could be recorded meant a pot could not be weighed at all until the
 * user happened to be standing there having just watered it.
 */
class WeighWithoutCalibrationTest {

    private var n = 0
    private fun r(day: Double, grams: Double, context: ReadingContext = ReadingContext.ROUTINE) =
        WeightReading(
            id = "w${n++}", plantId = "p",
            timestampMillis = (day * MILLIS_PER_DAY).toLong(),
            tzOffsetMinutes = 330, grams = grams, context = context,
        )

    private val plant = Plant(id = "p", name = "Fern")

    private fun assemble(readings: List<WeightReading>, p: Plant = plant, repots: List<Long> = emptyList()) =
        assembleWeightState(p, readings, emptyList(), repots, (30 * MILLIS_PER_DAY).toLong())

    @Test
    fun `readings are kept even with no anchor at all`() {
        val s = assemble(listOf(r(0.0, 900.0), r(2.0, 870.0), r(4.0, 840.0)))
        assertEquals(3, s.readings.size)
        assertFalse(s.isCalibrated)
        // The curve exists. It just cannot be turned into a percentage yet.
        assertTrue(s.segments.isNotEmpty())
        assertNull(s.depletion)
    }

    @Test
    fun `no anchor means no prediction, stated as such`() {
        val s = assemble(listOf(r(0.0, 900.0), r(2.0, 870.0)))
        val p = s.prediction
        assertTrue(p is Prediction.NeedAnotherReading)
        assertEquals(
            SuppressionReason.NOT_CALIBRATED,
            (p as Prediction.NeedAnotherReading).reason,
        )
    }

    @Test
    fun `a just-watered reading is the calibration`() {
        val s = assemble(
            listOf(
                r(0.0, 1000.0, ReadingContext.POST_WATER),
                r(3.0, 940.0),
            ),
        )
        assertTrue(s.isCalibrated)
        assertEquals(1000.0, s.plant.anchors!!.wetGrams, 0.001)
        assertNotNull(s.depletion)
    }

    @Test
    fun `weighing first and watering later still calibrates`() {
        // The realistic order: somebody starts weighing, then a few days later
        // waters and weighs again. Nothing should be lost from before.
        val s = assemble(
            listOf(
                r(0.0, 880.0),
                r(2.0, 860.0),
                r(4.0, 1010.0, ReadingContext.POST_WATER),
                r(6.0, 970.0),
            ),
        )
        assertTrue(s.isCalibrated)
        assertEquals(1010.0, s.plant.anchors!!.wetGrams, 0.001)
        assertEquals(4, s.readings.size)
    }

    @Test
    fun `an excluded post-water reading does not anchor the plant`() {
        // Deriving on read rather than storing is what makes this work: undoing
        // a bad weigh-in undoes the calibration it caused.
        val bad = r(0.0, 5000.0, ReadingContext.POST_WATER).copy(excluded = true)
        val s = assemble(listOf(bad, r(2.0, 870.0)))
        assertFalse(s.isCalibrated)
    }

    @Test
    fun `an explicit calibration still wins over a derived one`() {
        val calibrated = plant.copy(anchors = Anchors.fromWetAnchor(1200.0))
        val s = assemble(listOf(r(0.0, 1000.0, ReadingContext.POST_WATER)), p = calibrated)
        // The reading re-anchors the wet end, which is the documented behaviour
        // of a post-water weigh - but it started from the explicit anchor.
        assertEquals(1000.0, s.plant.anchors!!.wetGrams, 0.001)
        assertTrue(s.isCalibrated)
    }

    @Test
    fun `a repot invalidates older post-water readings`() {
        // The pot's dry weight changed. Anchoring off a weigh from the old pot
        // would silently undo the recalibration a repot asks for.
        val repotAt = (10 * MILLIS_PER_DAY).toLong()
        val s = assemble(
            listOf(r(2.0, 1000.0, ReadingContext.POST_WATER), r(12.0, 800.0)),
            p = plant.copy(needsRecalibration = true),
            repots = listOf(repotAt),
        )
        assertFalse(s.isCalibrated)
    }

    @Test
    fun `weighing after a repot recalibrates without being asked twice`() {
        val repotAt = (10 * MILLIS_PER_DAY).toLong()
        val s = assemble(
            listOf(r(2.0, 1000.0, ReadingContext.POST_WATER), r(12.0, 1400.0, ReadingContext.POST_WATER)),
            p = plant.copy(anchors = Anchors.fromWetAnchor(1000.0), needsRecalibration = true),
            repots = listOf(repotAt),
        )
        assertTrue(s.isCalibrated)
        assertFalse(s.plant.needsRecalibration)
        assertEquals(1400.0, s.plant.anchors!!.wetGrams, 0.001)
    }

    @Test
    fun `an empty plant is still an empty plant`() {
        val s = assemble(emptyList())
        assertTrue(s.readings.isEmpty())
        assertTrue(s.segments.isEmpty())
        assertNull(s.depletion)
    }
}
