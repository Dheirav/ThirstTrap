package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentationTest {

    private val anchors = Anchors(1400.0, 1000.0, dryIsProvisional = false)

    @Test
    fun `a clean drying run is one segment`() {
        val segments = segmentReadings(linearRun(5, 1400.0, 20.0), anchors)
        assertEquals(1, segments.size)
    }

    @Test
    fun `a watering jump splits the run`() {
        val readings = listOf(
            reading(0.0, 1400.0), reading(1.0, 1380.0), reading(2.0, 1360.0),
            reading(3.0, 1395.0), // +35 g, above 8% of a 400 g range (32 g)
            reading(4.0, 1375.0),
        )
        val segments = segmentReadings(readings, anchors)
        assertEquals(2, segments.size)
        assertEquals(3, segments[0].readings.size)
        assertEquals(SegmentStart.WATERING, segments[1].startReason)
    }

    @Test
    fun `a jump below the threshold is treated as noise`() {
        val readings = listOf(
            reading(0.0, 1400.0), reading(1.0, 1380.0),
            reading(2.0, 1390.0), // +10 g, well under 32 g
            reading(3.0, 1370.0),
        )
        assertEquals(1, segmentReadings(readings, anchors).size)
    }

    @Test
    fun `a watering event splits even when no weight jump was recorded`() {
        val readings = listOf(reading(0.0, 1400.0), reading(1.0, 1380.0), reading(2.0, 1360.0))
        val wateredAt = T0 + (1.5 * DAY).toLong()
        val segments = segmentReadings(readings, anchors, wateringEventsMillis = listOf(wateredAt))
        assertEquals(2, segments.size)
    }

    @Test
    fun `a repot starts a new segment and is labelled as one`() {
        val readings = listOf(reading(0.0, 1400.0), reading(1.0, 1380.0), reading(2.0, 1360.0))
        val repotAt = T0 + (1.5 * DAY).toLong()
        val segments = segmentReadings(readings, anchors, repotEventsMillis = listOf(repotAt))
        assertEquals(2, segments.size)
        assertEquals(SegmentStart.REPOT, segments[1].startReason)
    }

    @Test
    fun `a long gap makes the old segment stale`() {
        val readings = listOf(
            reading(0.0, 1400.0), reading(1.0, 1380.0),
            reading(25.0, 1300.0),
        )
        val segments = segmentReadings(readings, anchors)
        assertEquals(2, segments.size)
        assertEquals(SegmentStart.GAP, segments[1].startReason)
    }

    /**
     * The cardinal rule of the model. A slope averaged across a watering is a
     * slope through a sawtooth, which means nothing.
     */
    @Test
    fun `no fit ever spans a watering boundary`() {
        val readings = listOf(
            reading(0.0, 1400.0), reading(1.0, 1380.0), reading(2.0, 1360.0),
            reading(3.0, 1400.0), reading(4.0, 1380.0), reading(5.0, 1360.0),
        )
        val segments = segmentReadings(readings, anchors)
        assertEquals(2, segments.size)
        segments.forEach { segment ->
            val fit = fitSegmentSlope(segment, anchors)
            assertTrue("segment produced $fit", fit is SlopeFit.Fitted)
            assertEquals(-20.0, (fit as SlopeFit.Fitted).gramsPerDay, 0.001)
        }
    }

    @Test
    fun `readings arriving out of order are sorted before segmenting`() {
        val jumbled = listOf(reading(2.0, 1360.0), reading(0.0, 1400.0), reading(1.0, 1380.0))
        val segments = segmentReadings(jumbled, anchors)
        assertEquals(1, segments.size)
        assertEquals(listOf(1400.0, 1380.0, 1360.0), segments[0].readings.map { it.grams })
    }

    @Test
    fun `no readings gives no segments`() {
        assertTrue(segmentReadings(emptyList(), anchors).isEmpty())
    }
}
