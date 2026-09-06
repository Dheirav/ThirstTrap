package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiagnosticsTest {

    private fun at(days: Double) = T0 + (days * DAY).toLong()

    @Test
    fun `nothing fires without an established baseline`() {
        val segment = segmentOf(linearRun(4, 1400.0, 60.0))
        assertNull(diagnoseDrying(plant(ewma = -20.0), segment, closedSegmentCount = 1, at(3.0)))
    }

    @Test
    fun `drying much faster than usual is flagged`() {
        val segment = segmentOf(linearRun(4, 1400.0, 60.0))
        assertEquals(
            DryingDiagnostic.DRYING_FASTER_THAN_USUAL,
            diagnoseDrying(plant(ewma = -20.0), segment, closedSegmentCount = 3, at(3.0)),
        )
    }

    @Test
    fun `a normal rate is not flagged`() {
        val segment = segmentOf(linearRun(4, 1400.0, 22.0))
        assertNull(diagnoseDrying(plant(ewma = -20.0), segment, closedSegmentCount = 3, at(3.0)))
    }

    @Test
    fun `a pot staying heavy is flagged after five days`() {
        val segment = segmentOf(
            listOf(
                reading(0.0, 1400.0), reading(2.0, 1398.0),
                reading(4.0, 1396.0), reading(6.0, 1394.0),
            ),
        )
        assertEquals(
            DryingDiagnostic.POT_STAYING_HEAVY,
            diagnoseDrying(plant(ewma = -20.0), segment, closedSegmentCount = 3, at(6.0)),
        )
    }

    @Test
    fun `a heavy pot is not flagged before five days have passed`() {
        val segment = segmentOf(
            listOf(reading(0.0, 1400.0), reading(1.0, 1399.0), reading(2.0, 1398.0)),
        )
        assertNull(diagnoseDrying(plant(ewma = -20.0), segment, closedSegmentCount = 3, at(2.0)))
    }

    @Test
    fun `fast drying needs three readings before it fires`() {
        val segment = segmentOf(linearRun(2, 1400.0, 60.0))
        assertNull(diagnoseDrying(plant(ewma = -20.0), segment, closedSegmentCount = 3, at(1.0)))
    }
}
