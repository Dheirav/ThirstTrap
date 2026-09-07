package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightAssemblyTest {

    private fun at(days: Double) = T0 + (days * DAY).toLong()

    private fun calibrated(trigger: Double = 0.5) = plant(
        anchors = Anchors(1400.0, 1000.0, dryIsProvisional = false),
        trigger = trigger,
    )

    @Test
    fun `no readings leaves the plant uncalibrated in effect`() {
        val s = assembleWeightState(plant(anchors = null), emptyList(), emptyList(), emptyList(), T0)
        assertTrue(s.prediction is Prediction.NeedAnotherReading)
        assertNull(s.depletion)
    }

    @Test
    fun `a steady drying run yields a prediction and a depletion`() {
        val s = assembleWeightState(
            calibrated(), linearRun(4, 1400.0, 20.0), emptyList(), emptyList(), at(3.0),
        )
        assertTrue("got ${s.prediction}", s.prediction is Prediction.Eta)
        assertNotNull(s.depletion)
        assertEquals(-20.0, s.slopeGramsPerDay!!, 0.5)
    }

    @Test
    fun `the wet anchor follows the most recent post-water weigh`() {
        val readings = listOf(
            reading(0.0, 1400.0, ReadingContext.CALIBRATION),
            reading(5.0, 1250.0),
            reading(6.0, 1460.0, ReadingContext.POST_WATER),
        )
        val s = assembleWeightState(calibrated(), readings, emptyList(), emptyList(), at(6.0))
        assertEquals(1460.0, s.plant.anchors!!.wetGrams, 1e-9)
    }

    @Test
    fun `a measured dry anchor only ever walks down`() {
        // Already measured at 1000 g. Watering at 1180 g is watering earlier
        // than necessary, and must not raise where "dry" is judged to be.
        val higher = assembleWeightState(
            calibrated(),
            listOf(
                reading(0.0, 1400.0, ReadingContext.CALIBRATION),
                reading(4.0, 1180.0, ReadingContext.PRE_WATER),
            ),
            emptyList(), emptyList(), at(4.0),
        )
        assertEquals(1000.0, higher.plant.anchors!!.dryGrams, 1e-9)

        val lower = assembleWeightState(
            calibrated(),
            listOf(
                reading(0.0, 1400.0, ReadingContext.CALIBRATION),
                reading(4.0, 940.0, ReadingContext.PRE_WATER),
            ),
            emptyList(), emptyList(), at(4.0),
        )
        assertEquals(940.0, lower.plant.anchors!!.dryGrams, 1e-9)
    }

    @Test
    fun `a provisional dry anchor is replaced by the first real observation`() {
        // The estimate is a guess (wet x 0.60 = 840 g). Watering at 1180 g says
        // the guess was too aggressive, and the observation wins outright.
        val provisional = plant(anchors = Anchors.fromWetAnchor(1400.0))
        val s = assembleWeightState(
            provisional,
            listOf(reading(4.0, 1180.0, ReadingContext.PRE_WATER)),
            emptyList(), emptyList(), at(4.0),
        )
        assertEquals(1180.0, s.plant.anchors!!.dryGrams, 1e-9)
    }

    @Test
    fun `an excluded reading is kept but does not move the anchors`() {
        val readings = listOf(
            reading(0.0, 1400.0, ReadingContext.CALIBRATION),
            reading(1.0, 300.0, ReadingContext.PRE_WATER, excluded = true),
        )
        val s = assembleWeightState(calibrated(), readings, emptyList(), emptyList(), at(1.0))
        assertEquals(1000.0, s.plant.anchors!!.dryGrams, 1e-9)
        assertEquals(2, s.readings.size)
    }

    @Test
    fun `the prior is folded from closed segments, so day one still predicts`() {
        // Two full cycles at 20 g/day, then a single fresh reading.
        val readings = buildList {
            addAll((0..4).map { reading(it.toDouble(), 1400.0 - 20.0 * it) })
            addAll((0..4).map { reading(10.0 + it, 1400.0 - 20.0 * it) })
            add(reading(20.0, 1400.0))
        }
        val watered = listOf(at(9.5), at(19.5))
        val s = assembleWeightState(calibrated(), readings, watered, emptyList(), at(20.0))

        assertEquals(2, s.closedSegmentCount)
        assertNotNull("no prior was derived", s.plant.slopeEwmaGramsPerDay)
        assertEquals(-20.0, s.plant.slopeEwmaGramsPerDay!!, 1.0)
        // One reading in the open segment, but history carries it.
        assertTrue("got ${s.prediction}", s.prediction is Prediction.Eta)
        assertEquals(Confidence.LOW, (s.prediction as Prediction.Eta).confidence)
    }

    @Test
    fun `a repot invalidates everything rather than predicting from a different pot`() {
        val s = assembleWeightState(
            calibrated().copy(needsRecalibration = true),
            linearRun(4, 1400.0, 20.0), emptyList(), emptyList(), at(3.0),
        )
        assertEquals(
            SuppressionReason.NEEDS_RECALIBRATION,
            (s.prediction as Prediction.NeedAnotherReading).reason,
        )
    }

    @Test
    fun `drying far faster than the plant's own baseline is flagged`() {
        val readings = buildList {
            addAll((0..4).map { reading(it.toDouble(), 1400.0 - 20.0 * it) })
            addAll((0..4).map { reading(10.0 + it, 1400.0 - 20.0 * it) })
            addAll((0..3).map { reading(20.0 + it, 1400.0 - 60.0 * it) })
        }
        val watered = listOf(at(9.5), at(19.5))
        val s = assembleWeightState(calibrated(), readings, watered, emptyList(), at(23.0))
        assertEquals(DryingDiagnostic.DRYING_FASTER_THAN_USUAL, s.diagnostic)
    }

    @Test
    fun `water-propagation subjects are never asked to be weighed`() {
        val s = assembleWeightState(
            calibrated().copy(medium = Medium.WATER),
            linearRun(4, 1400.0, 20.0), emptyList(), emptyList(), at(3.0),
        )
        assertEquals(
            SuppressionReason.WEIGHT_MEANINGLESS_FOR_MEDIUM,
            (s.prediction as Prediction.NeedAnotherReading).reason,
        )
    }
}
