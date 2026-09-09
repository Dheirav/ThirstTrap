package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttentionTest {

    private val now = T0 + 10 * DAY

    private fun item(
        name: String,
        dueMillis: Long? = null,
        lastChecked: Long? = null,
        prediction: Prediction = Prediction.NeedAnotherReading(SuppressionReason.NO_READINGS),
    ) = PlantAttention(
        plant = plant().copy(id = name, name = name),
        lastWateredMillis = lastChecked,
        lastCheckedMillis = lastChecked,
        reminderDueMillis = dueMillis,
        depletion = null,
        prediction = prediction,
    )

    @Test
    fun `overdue reminders come first, most overdue leading`() {
        val sorted = sortByAttention(
            listOf(
                item("b", dueMillis = now - DAY),
                item("a", dueMillis = now - 5 * DAY),
                item("c"),
            ),
            now,
        )
        assertEquals(listOf("a", "b", "c"), sorted.map { it.plant.name })
    }

    @Test
    fun `past the trigger outranks merely unchecked`() {
        val sorted = sortByAttention(
            listOf(
                item("unchecked", lastChecked = now - 30 * DAY),
                item("thirsty", prediction = Prediction.WaterNow),
            ),
            now,
        )
        assertEquals(listOf("thirsty", "unchecked"), sorted.map { it.plant.name })
    }

    @Test
    fun `due within a day outranks a distant prediction`() {
        val sorted = sortByAttention(
            listOf(
                item("later", prediction = Prediction.Eta(6.0, Confidence.HIGH, false)),
                item("soon", prediction = Prediction.Eta(0.5, Confidence.HIGH, false)),
            ),
            now,
        )
        assertEquals(listOf("soon", "later"), sorted.map { it.plant.name })
    }

    @Test
    fun `longest unchecked breaks ties, then name`() {
        val sorted = sortByAttention(
            listOf(
                item("recent", lastChecked = now - DAY),
                item("stale", lastChecked = now - 9 * DAY),
            ),
            now,
        )
        assertEquals(listOf("stale", "recent"), sorted.map { it.plant.name })
    }

    @Test
    fun `average interval needs two waterings`() {
        assertNull(averageWateringIntervalDays(emptyList()))
        assertNull(averageWateringIntervalDays(listOf(T0)))
        assertEquals(
            8.0,
            averageWateringIntervalDays(listOf(T0, T0 + 8 * DAY, T0 + 16 * DAY))!!,
            1e-9,
        )
    }

    @Test
    fun `the schedule ignores a top-up pair`() {
        // The Fittonia case: watered, then watered again a day and a bit later.
        // An average calls that a one day rhythm and falls due the next morning.
        val topUp = listOf(T0, T0 + (1.32 * DAY).toLong())
        assertEquals(1.32, averageWateringIntervalDays(topUp)!!, 1e-2)
        assertNull(checkIntervalFromLogDays(topUp))
    }

    @Test
    fun `the schedule needs two real gaps before it trusts the log`() {
        assertNull(checkIntervalFromLogDays(emptyList()))
        assertNull(checkIntervalFromLogDays(listOf(T0)))
        assertNull(checkIntervalFromLogDays(listOf(T0, T0 + 8 * DAY)))
        assertEquals(
            8.0,
            checkIntervalFromLogDays(listOf(T0, T0 + 8 * DAY, T0 + 16 * DAY))!!,
            1e-9,
        )
    }

    @Test
    fun `same-episode waterings collapse instead of dragging the median down`() {
        // Watered, topped up two hours later, then watered on day 9 and day 16.
        // The top-up gap disappears rather than counting as a two hour cycle,
        // leaving gaps of 8.92 and 7 days.
        val log = listOf(
            T0,
            T0 + (0.08 * DAY).toLong(),
            T0 + 9 * DAY,
            T0 + 16 * DAY,
        )
        assertEquals(7.96, checkIntervalFromLogDays(log)!!, 1e-9)
    }

    @Test
    fun `the median survives one unusual gap`() {
        // A holiday in the middle should not double the interval.
        val log = listOf(T0, T0 + 7 * DAY, T0 + 14 * DAY, T0 + 44 * DAY, T0 + 51 * DAY)
        assertEquals(7.0, checkIntervalFromLogDays(log)!!, 1e-9)
        assertEquals(12.75, averageWateringIntervalDays(log)!!, 1e-9)
    }
}
