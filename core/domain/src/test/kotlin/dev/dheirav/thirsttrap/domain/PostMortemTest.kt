package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class PostMortemTest {

    private fun watering(dayOffset: Int) = CareEvent(
        id = "w$dayOffset", plantId = "p1",
        timestampMillis = T0 + dayOffset * DAY, tzOffsetMinutes = 330,
        type = CareEventType.WATERED,
    )

    private fun event(type: CareEventType, dayOffset: Int) = CareEvent(
        id = "e$dayOffset-$type", plantId = "p1",
        timestampMillis = T0 + dayOffset * DAY, tzOffsetMinutes = 330,
        type = type,
    )

    @Test
    fun `very frequent watering is raised as a question, not a verdict`() {
        val notes = buildPostMortemObservations(
            (0..8).map { watering(it) }, averageIntervalDays = 1.0, nowMillis = T0 + 9 * DAY,
        )
        assertTrue(notes.any { "often" in it })
        // Never states a cause.
        assertTrue(notes.none { it.contains("killed", true) || it.contains("caused", true) })
    }

    @Test
    fun `long gaps are noted for what they suit`() {
        val notes = buildPostMortemObservations(
            listOf(watering(0), watering(30)), averageIntervalDays = 30.0, nowMillis = T0 + 31 * DAY,
        )
        assertTrue(notes.any { "succulents" in it })
    }

    @Test
    fun `an unusually long single gap is surfaced`() {
        val events = listOf(watering(0), watering(3), watering(6), watering(40))
        val notes = buildPostMortemObservations(events, averageIntervalDays = 13.0, nowMillis = T0 + 41 * DAY)
        assertTrue(notes.any { "one gap of" in it })
    }

    @Test
    fun `a recent repot is flagged, an old one is not`() {
        val recent = buildPostMortemObservations(
            listOf(event(CareEventType.REPOTTED, 0)), null, nowMillis = T0 + 10 * DAY,
        )
        assertTrue(recent.any { "Repotted" in it })

        val old = buildPostMortemObservations(
            listOf(event(CareEventType.REPOTTED, 0)), null, nowMillis = T0 + 200 * DAY,
        )
        assertTrue(old.none { "Repotted" in it })
    }

    @Test
    fun `a medium change is always high-risk enough to mention`() {
        val notes = buildPostMortemObservations(
            listOf(event(CareEventType.MEDIUM_CHANGED, 0)), null, nowMillis = T0 + 300 * DAY,
        )
        assertTrue(notes.any { "high-risk" in it })
    }

    @Test
    fun `an empty log says so rather than inventing something`() {
        val notes = buildPostMortemObservations(emptyList(), null, T0)
        assertTrue(notes.any { "cannot say much" in it })
    }
}
