package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnchorsTest {

    @Test
    fun `calibration estimates the dry anchor and flags it provisional`() {
        val a = Anchors.fromWetAnchor(1400.0)
        assertEquals(840.0, a.dryGrams, 1e-9)
        assertTrue(a.dryIsProvisional)
    }

    @Test
    fun `depletion runs zero to one across the range`() {
        val a = Anchors(1400.0, 1000.0, false)
        assertEquals(0.0, a.depletionAt(1400.0), 1e-9)
        assertEquals(0.5, a.depletionAt(1200.0), 1e-9)
        assertEquals(1.0, a.depletionAt(1000.0), 1e-9)
    }

    @Test
    fun `drier than the anchor is shown rather than hidden`() {
        val a = Anchors(1400.0, 1000.0, false)
        assertTrue(a.depletionAt(950.0) > 1.0)
    }

    @Test
    fun `trigger weight follows the depletion fraction`() {
        val a = Anchors(1400.0, 1000.0, false)
        assertEquals(1200.0, a.triggerWeight(0.5), 1e-9)
        assertEquals(1120.0, a.triggerWeight(0.7), 1e-9) // succulent
        assertEquals(1280.0, a.triggerWeight(0.3), 1e-9) // fern
    }

    @Test
    fun `a post-water reading re-anchors the wet end`() {
        val a = Anchors(1400.0, 1000.0, false)
            .withReading(reading(0.0, 1450.0, ReadingContext.POST_WATER))
        assertEquals(1450.0, a.wetGrams, 1e-9)
    }

    @Test
    fun `a pre-water reading lowers the dry anchor and clears provisional`() {
        val a = Anchors.fromWetAnchor(1400.0)
            .withReading(reading(0.0, 1150.0, ReadingContext.PRE_WATER))
        assertEquals(1150.0, a.dryGrams, 1e-9)
        assertFalse(a.dryIsProvisional)
    }

    @Test
    fun `the first real observation replaces a provisional anchor even when higher`() {
        // Provisional is 840 g. The user actually watered at 1150 g, so the
        // estimate was too aggressive and the observation wins.
        val a = Anchors.fromWetAnchor(1400.0)
            .withReading(reading(0.0, 1150.0, ReadingContext.PRE_WATER))
        assertEquals(1150.0, a.dryGrams, 1e-9)
        assertFalse(a.dryIsProvisional)
    }

    @Test
    fun `the dry anchor only ever decreases`() {
        val a = Anchors(1400.0, 1100.0, false)
            .withReading(reading(0.0, 1250.0, ReadingContext.PRE_WATER))
        assertEquals(1100.0, a.dryGrams, 1e-9)
    }

    @Test
    fun `an implausibly light pre-water reading is rejected as a mis-weigh`() {
        val a = Anchors(1400.0, 1000.0, false)
            .withReading(reading(0.0, 300.0, ReadingContext.PRE_WATER))
        assertEquals(1000.0, a.dryGrams, 1e-9)
    }

    @Test
    fun `a pre-water reading at the wet weight is rejected rather than collapsing the range`() {
        // A pot still at container capacity says nothing about how dry this
        // person lets it get. It matters on the provisional path specifically,
        // because that branch replaces the guess outright even upward, so it is
        // the one route that can plant the dry anchor on top of the wet one -
        // for a measured anchor the running minimum already refuses it.
        val a = Anchors.fromWetAnchor(1400.0)
            .withReading(reading(0.0, 1400.0, ReadingContext.PRE_WATER))
        assertEquals(840.0, a.dryGrams, 1e-9)
        assertTrue(a.dryIsProvisional)
        assertTrue(a.isUsable)
    }

    @Test
    fun `a post-water reading below the measured dry anchor restarts the estimate`() {
        // A watering cannot leave a pot lighter than its own dry weight, so
        // the pot changed. Keeping the old dry end would leave a negative
        // range; the measured value goes back to being a guess.
        val a = Anchors(1400.0, 1000.0, false)
            .withReading(reading(0.0, 900.0, ReadingContext.POST_WATER))
        assertEquals(900.0, a.wetGrams, 1e-9)
        assertEquals(540.0, a.dryGrams, 1e-9)
        assertTrue(a.dryIsProvisional)
        assertTrue(a.isUsable)
    }

    @Test
    fun `depletion is a number even for an impossible stored pair`() {
        // Anchors also arrive straight from a database row, which can carry
        // anything. Division by a zero range gives NaN, and NaN survives
        // coerceIn because every comparison against it is false, so it would
        // reach the dashboard ring.
        val degenerate = Anchors(1000.0, 1000.0, false)
        assertFalse(degenerate.isUsable)
        assertFalse(degenerate.depletionAt(1000.0).isNaN())
        assertFalse(degenerate.depletionAt(900.0).isNaN())
        assertFalse(degenerate.triggerWeight(0.5).isNaN())

        val inverted = Anchors(1000.0, 1020.0, false)
        assertFalse(inverted.isUsable)
        assertFalse(inverted.depletionAt(980.0).isNaN())
        assertTrue(inverted.minMeaningfulSlope() > 0.0)
    }

    @Test
    fun `routine readings carry no anchor information`() {
        val before = Anchors(1400.0, 1000.0, false)
        assertEquals(before, before.withReading(reading(0.0, 1200.0, ReadingContext.ROUTINE)))
    }
}
