package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientTest {

    private var counter = 0
    private fun reading(
        tempC: Double? = null,
        humidity: Double? = null,
        location: String = "Windowsill",
        atMillis: Long = 0L,
        source: AmbientSource = AmbientSource.MANUAL,
    ) = AmbientReading(
        id = "a${counter++}",
        location = location,
        timestampMillis = atMillis,
        tzOffsetMinutes = 330,
        temperatureC = tempC,
        humidityPercent = humidity,
        source = source,
    )

    private fun period(temp: Double, humidity: Double? = null, n: Int = 3) =
        List(n) { reading(tempC = temp, humidity = humidity) }

    @Test
    fun `a warmer room explains a pot drying faster`() {
        val e = explainDryingChange(
            baselineGramsPerDay = -10.0,
            currentGramsPerDay = -18.0,
            baseline = period(19.0),
            current = period(28.0),
        )
        assertEquals(AmbientVerdict.EXPLAINS_FASTER, e?.verdict)
        assertEquals(9.0, e!!.temperatureDeltaC!!, 0.001)
        assertEquals(1.8, e.dryingRatio, 0.001)
    }

    @Test
    fun `a cooler room explains a pot staying heavy`() {
        val e = explainDryingChange(
            baselineGramsPerDay = -20.0,
            currentGramsPerDay = -6.0,
            baseline = period(27.0),
            current = period(18.0),
        )
        assertEquals(AmbientVerdict.EXPLAINS_SLOWER, e?.verdict)
    }

    @Test
    fun `drier air alone explains faster drying, with no thermometer at all`() {
        // Plenty of people own a hygrometer and no thermometer, or the reverse.
        val e = explainDryingChange(
            baselineGramsPerDay = -8.0,
            currentGramsPerDay = -14.0,
            baseline = List(3) { reading(humidity = 70.0) },
            current = List(3) { reading(humidity = 40.0) },
        )
        assertEquals(AmbientVerdict.EXPLAINS_FASTER, e?.verdict)
        assertNull(e!!.temperatureDeltaC)
        assertEquals(-30.0, e.humidityDeltaPercent!!, 0.001)
    }

    @Test
    fun `an unchanged room is the finding, not the absence of one`() {
        // This is the verdict that makes a diagnostic worth acting on: the
        // boring explanation is ruled out, so what is left is the plant.
        val e = explainDryingChange(
            baselineGramsPerDay = -10.0,
            currentGramsPerDay = -19.0,
            baseline = period(22.0, 55.0),
            current = period(23.0, 52.0),
        )
        assertEquals(AmbientVerdict.ROOM_UNCHANGED, e?.verdict)
    }

    @Test
    fun `a room moving the opposite way is flagged, not smoothed over`() {
        // A cold snap while a pot dries faster is what a shrunken root ball
        // channelling water down the sides looks like. Worth surfacing.
        val e = explainDryingChange(
            baselineGramsPerDay = -10.0,
            currentGramsPerDay = -20.0,
            baseline = period(26.0),
            current = period(17.0),
        )
        assertEquals(AmbientVerdict.CONTRADICTS, e?.verdict)
    }

    @Test
    fun `warmer but much more humid is no direction at all`() {
        val e = explainDryingChange(
            baselineGramsPerDay = -10.0,
            currentGramsPerDay = -20.0,
            baseline = period(18.0, 40.0),
            current = period(26.0, 75.0),
        )
        assertEquals(AmbientVerdict.ROOM_UNCHANGED, e?.verdict)
    }

    @Test
    fun `nothing is said when the pot has not actually changed`() {
        // The room may have swung wildly; if the drying rate did not move there
        // is no phenomenon to explain and saying "it is warmer" is noise.
        assertNull(
            explainDryingChange(
                baselineGramsPerDay = -10.0,
                currentGramsPerDay = -10.5,
                baseline = period(18.0),
                current = period(30.0),
            ),
        )
    }

    @Test
    fun `nothing is said from a single reading per period`() {
        assertNull(
            explainDryingChange(
                baselineGramsPerDay = -10.0,
                currentGramsPerDay = -20.0,
                baseline = period(18.0, n = 1),
                current = period(30.0, n = 1),
            ),
        )
    }

    @Test
    fun `nothing is said when the two periods measured different things`() {
        // Temperature before, humidity after. There is no comparison to make.
        assertNull(
            explainDryingChange(
                baselineGramsPerDay = -10.0,
                currentGramsPerDay = -20.0,
                baseline = List(3) { reading(tempC = 20.0) },
                current = List(3) { reading(humidity = 40.0) },
            ),
        )
    }

    @Test
    fun `a baseline of zero cannot produce a ratio`() {
        assertNull(
            explainDryingChange(
                baselineGramsPerDay = 0.0,
                currentGramsPerDay = -20.0,
                baseline = period(18.0),
                current = period(30.0),
            ),
        )
    }

    @Test
    fun `outdoor weather stays labelled as outdoor weather`() {
        val e = explainDryingChange(
            baselineGramsPerDay = -10.0,
            currentGramsPerDay = -20.0,
            baseline = List(3) { reading(tempC = 18.0, source = AmbientSource.WEATHER) },
            current = List(3) { reading(tempC = 30.0, source = AmbientSource.WEATHER) },
        )
        assertEquals(AmbientSource.WEATHER, e?.source)
    }

    @Test
    fun `a hand reading outranks weather when both are present`() {
        // Someone who bothered to read a thermometer in the room knows more
        // about that room than an outdoor forecast does.
        val e = explainDryingChange(
            baselineGramsPerDay = -10.0,
            currentGramsPerDay = -20.0,
            baseline = List(3) { reading(tempC = 18.0, source = AmbientSource.WEATHER) },
            current = listOf(
                reading(tempC = 30.0, source = AmbientSource.WEATHER),
                reading(tempC = 29.0, source = AmbientSource.MANUAL),
            ),
        )
        assertEquals(AmbientSource.MANUAL, e?.source)
    }

    @Test
    fun `readings are matched by location and expire`() {
        val now = 100L * MILLIS_PER_DAY.toLong()
        val all = listOf(
            reading(tempC = 20.0, location = "Windowsill", atMillis = now - 1_000L),
            reading(tempC = 20.0, location = "windowsill", atMillis = now - 2_000L),
            reading(tempC = 20.0, location = "Bathroom", atMillis = now - 3_000L),
            reading(
                tempC = 20.0, location = "Windowsill",
                atMillis = now - ((AMBIENT_STALE_DAYS + 5) * MILLIS_PER_DAY).toLong(),
            ),
        )
        val kept = all.forLocation("Windowsill", now)
        assertEquals(2, kept.size)
        assertTrue(kept.all { it.location.equals("windowsill", ignoreCase = true) })
        // Newest first, so a caller taking the head gets the most recent.
        assertTrue(kept[0].timestampMillis > kept[1].timestampMillis)
    }

    @Test
    fun `no location means no readings rather than everything`() {
        val all = listOf(reading(tempC = 20.0, location = "Windowsill"))
        assertTrue(all.forLocation(null, 0L).isEmpty())
        assertTrue(all.forLocation("", 0L).isEmpty())
        assertTrue(all.forLocation("  ", 0L).isEmpty())
    }

    @Test
    fun `summarising ignores the fields that were left blank`() {
        val s = listOf(
            reading(tempC = 20.0),
            reading(tempC = 24.0, humidity = 50.0),
            reading(humidity = 60.0),
        ).summarise()
        assertEquals(22.0, s.meanTemperatureC!!, 0.001)
        assertEquals(55.0, s.meanHumidityPercent!!, 0.001)
        assertEquals(3, s.readingCount)
        assertNotNull(s.meanTemperatureC)
    }
}

class AmbientForPlantTest {

    private var n = 0
    private fun ambient(location: String, atMillis: Long, tempC: Double) = AmbientReading(
        id = "a${n++}", location = location, timestampMillis = atMillis,
        tzOffsetMinutes = 330, temperatureC = tempC,
    )

    private fun reading(atMillis: Long, grams: Double) = WeightReading(
        id = "w${n++}", plantId = "p", timestampMillis = atMillis,
        tzOffsetMinutes = 330, grams = grams, context = ReadingContext.ROUTINE,
    )

    private val now = 200L * MILLIS_PER_DAY.toLong()
    private val segmentStart = now - (4 * MILLIS_PER_DAY).toLong()

    private fun state(
        location: String? = "Windowsill",
        ewma: Double? = -10.0,
        current: Double? = -20.0,
        closed: Int = 3,
    ) = WeightState(
        plant = Plant(
            id = "p", name = "Test", location = location,
            slopeEwmaGramsPerDay = ewma,
        ),
        segments = listOf(
            DryingSegment(
                readings = listOf(reading(segmentStart, 900.0), reading(now, 820.0)),
                startReason = SegmentStart.WATERING,
            ),
        ),
        slopeGramsPerDay = current,
        closedSegmentCount = closed,
    )

    private fun ambientSet() = listOf(
        ambient("Windowsill", segmentStart - (10 * MILLIS_PER_DAY).toLong(), 19.0),
        ambient("Windowsill", segmentStart - (8 * MILLIS_PER_DAY).toLong(), 19.5),
        ambient("Windowsill", segmentStart + (1 * MILLIS_PER_DAY).toLong(), 29.0),
        ambient("Windowsill", segmentStart + (2 * MILLIS_PER_DAY).toLong(), 28.5),
    )

    @Test
    fun `the segment boundary splits baseline from current`() {
        val e = explainForPlant(state(), ambientSet(), now)
        assertEquals(AmbientVerdict.EXPLAINS_FASTER, e?.verdict)
        assertEquals(9.5, e!!.temperatureDeltaC!!, 0.001)
    }

    @Test
    fun `a plant with no location has no room to describe`() {
        assertNull(explainForPlant(state(location = null), ambientSet(), now))
        assertNull(explainForPlant(state(location = "  "), ambientSet(), now))
    }

    @Test
    fun `another room's readings are not borrowed`() {
        val elsewhere = ambientSet().map { it.copy(location = "Bathroom") }
        assertNull(explainForPlant(state(), elsewhere, now))
    }

    @Test
    fun `one closed segment is not a baseline`() {
        // The EWMA after a single segment is that segment, so this would be
        // comparing the pot to itself and calling the room responsible.
        assertNull(explainForPlant(state(closed = 1), ambientSet(), now))
    }

    @Test
    fun `nothing is claimed before there is a fitted rate`() {
        assertNull(explainForPlant(state(current = null), ambientSet(), now))
        assertNull(explainForPlant(state(ewma = null), ambientSet(), now))
    }

    @Test
    fun `stale readings do not explain today's weather`() {
        val old = ambientSet().map {
            it.copy(timestampMillis = now - ((AMBIENT_STALE_DAYS + 10) * MILLIS_PER_DAY).toLong())
        }
        assertNull(explainForPlant(state(), old, now))
    }
}
