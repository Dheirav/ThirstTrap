package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelapseTest {

    private var n = 0
    private fun photo(dayOffset: Double, id: String? = null) = Photo(
        id = id ?: "p${n++}",
        plantId = "plant",
        relativePath = "photos/plant/${id ?: n}.jpg",
        takenAtMillis = (dayOffset * MILLIS_PER_DAY).toLong(),
        tzOffsetMinutes = 330,
    )

    @Test
    fun `frames run oldest to newest whatever order they arrive in`() {
        // A restored backup writes files in whatever order the zip held them,
        // so import order says nothing about the plant's life.
        val frames = buildTimelapse(
            listOf(photo(9.0, "c"), photo(0.0, "a"), photo(4.0, "b")),
        )
        assertEquals(listOf("a", "b", "c"), frames.map { it.photo.id })
        assertEquals(listOf(0, 1, 2), frames.map { it.index })
    }

    @Test
    fun `elapsed days are measured from the first photo, not from zero`() {
        val frames = buildTimelapse(listOf(photo(100.0), photo(107.0), photo(141.0)))
        assertEquals(listOf(0, 7, 41), frames.map { it.daysSinceFirst })
    }

    @Test
    fun `part days round to the nearest whole one`() {
        // A photo taken 6 hours after the first is still day 0; 20 hours is day 1.
        val frames = buildTimelapse(listOf(photo(0.0), photo(0.25), photo(0.83)))
        assertEquals(listOf(0, 0, 1), frames.map { it.daysSinceFirst })
    }

    @Test
    fun `the first plate is named rather than numbered`() {
        val frames = buildTimelapse(listOf(photo(0.0), photo(3.0)))
        assertEquals("the first photo", frames[0].dayLabel)
        assertEquals("day 3", frames[1].dayLabel)
    }

    @Test
    fun `one photo is not a timelapse`() {
        assertFalse(buildTimelapse(listOf(photo(0.0))).isPlayable())
        assertTrue(buildTimelapse(listOf(photo(0.0), photo(1.0))).isPlayable())
    }

    @Test
    fun `no photos produces no frames rather than an error`() {
        val frames = buildTimelapse(emptyList())
        assertTrue(frames.isEmpty())
        assertFalse(frames.isPlayable())
        assertEquals(0, frames.spanDays())
    }

    @Test
    fun `the span is the whole stretch the sequence covers`() {
        assertEquals(41, buildTimelapse(listOf(photo(0.0), photo(7.0), photo(41.0))).spanDays())
    }

    @Test
    fun `several photos on one day all keep their own frame`() {
        // People photograph a sick plant repeatedly in one afternoon, and that
        // burst is exactly the interesting part of a symptom progression.
        val frames = buildTimelapse(listOf(photo(2.0), photo(2.1), photo(2.2)))
        assertEquals(3, frames.size)
        assertEquals(listOf(0, 0, 0), frames.map { it.daysSinceFirst })
        assertTrue(frames.isPlayable())
    }
}
