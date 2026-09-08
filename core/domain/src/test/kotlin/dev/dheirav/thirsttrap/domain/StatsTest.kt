package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StatsTest {

    private var n = 0

    private fun at(year: Int, month: Int, day: Int): Long {
        val c = Calendar.getInstance(TimeZone.getDefault())
        c.set(year, month - 1, day, 12, 0, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun watering(millis: Long) = CareEvent(
        id = "e${n++}", plantId = "p", timestampMillis = millis,
        tzOffsetMinutes = 330, type = CareEventType.WATERED,
    )

    private fun plant(status: PlantStatus) =
        Plant(id = "p${n++}", name = "x", status = status)

    // --- waterings per month -------------------------------------------------

    @Test
    fun `months with no waterings are kept as zeros`() {
        // A gap is the interesting part. Closing it up would make a winter
        // where nothing needed water look identical to a busy month.
        val now = at(2026, 9, 15)
        val months = wateringsByMonth(
            listOf(watering(at(2026, 9, 1)), watering(at(2026, 7, 3))),
            monthsBack = 4,
            nowMillis = now,
        )
        // Jul, Aug, Sep - the table begins at the first watering, and August's
        // silence between them is kept.
        assertEquals(3, months.size)
        assertEquals(listOf(1, 0, 1), months.map { it.count })
        assertEquals("Sep 2026", months.last().label)
    }

    @Test
    fun `only waterings are counted`() {
        val now = at(2026, 9, 15)
        val events = listOf(
            watering(at(2026, 9, 2)),
            CareEvent("x", "p", at(2026, 9, 3), 330, CareEventType.CHECKED),
            CareEvent("y", "p", at(2026, 9, 4), 330, CareEventType.OBSERVATION),
        )
        assertEquals(1, wateringsByMonth(events, monthsBack = 1, nowMillis = now).single().count)
    }

    @Test
    fun `month keys sort chronologically across a year boundary`() {
        val months = wateringsByMonth(emptyList(), monthsBack = 3, nowMillis = at(2027, 1, 10))
        assertEquals(listOf("2026-11", "2026-12", "2027-01"), months.map { it.key })
        assertEquals(months.map { it.key }, months.map { it.key }.sorted())
    }

    // --- outcomes ------------------------------------------------------------

    @Test
    fun `nothing having left is no data, not a perfect score`() {
        // 100% would invite watching it fall, which is the streak dynamic this
        // app exists to avoid.
        val o = outcomesOf(listOf(plant(PlantStatus.ACTIVE), plant(PlantStatus.DORMANT)))
        assertEquals(2, o.stillHere)
        assertEquals(0, o.departed)
        assertNull(o.survivalRate)
    }

    @Test
    fun `the rate is computed only over plants that have actually left`() {
        val o = outcomesOf(
            listOf(
                plant(PlantStatus.ACTIVE), plant(PlantStatus.ACTIVE), plant(PlantStatus.ACTIVE),
                plant(PlantStatus.GIVEN_AWAY), plant(PlantStatus.GIVEN_AWAY),
                plant(PlantStatus.DEAD), plant(PlantStatus.DEAD),
            ),
        )
        // A living plant is not a pending failure, so it is not in the divisor.
        assertEquals(4, o.departed)
        assertEquals(0.5, o.survivalRate!!, 0.0001)
        assertEquals(3, o.stillHere)
    }

    // --- days to root --------------------------------------------------------

    private fun cutting(id: String, acquiredDay: Long, stage: PropagationStage?) = Plant(
        id = id, name = "cutting", source = PlantSource.CUTTING,
        acquiredEpochDay = acquiredDay, propagationStage = stage,
    )

    private fun potted(plantId: String, dayOffsetFromEpoch: Long) = CareEvent(
        id = "m${n++}", plantId = plantId,
        timestampMillis = dayOffsetFromEpoch * MILLIS_PER_DAY.toLong(),
        tzOffsetMinutes = 330, type = CareEventType.MILESTONE,
        propagationStage = PropagationStage.POTTED,
    )

    @Test
    fun `days to root runs from acquisition to the move to potted`() {
        val s = rootingStat(
            listOf(cutting("a", 100, PropagationStage.POTTED)),
            listOf(potted("a", 121)),
        )
        assertEquals(1, s.samples)
        assertEquals(21, s.medianDays)
    }

    @Test
    fun `the median is used, because one jar left for months would wreck a mean`() {
        val plants = listOf(
            cutting("a", 0, PropagationStage.POTTED),
            cutting("b", 0, PropagationStage.POTTED),
            cutting("c", 0, PropagationStage.POTTED),
        )
        val events = listOf(potted("a", 14), potted("b", 20), potted("c", 160))
        val s = rootingStat(plants, events)
        assertEquals(3, s.samples)
        assertEquals(20, s.medianDays)   // a mean would say 64
    }

    @Test
    fun `the earliest potting counts, not a later correction`() {
        val s = rootingStat(
            listOf(cutting("a", 0, PropagationStage.ESTABLISHED)),
            listOf(potted("a", 30), potted("a", 12)),
        )
        assertEquals(12, s.medianDays)
    }

    @Test
    fun `cuttings potted before stages were recorded are counted as untracked`() {
        // They must not silently vanish from the denominator, and they must not
        // be averaged in as zero either.
        val s = rootingStat(
            listOf(
                cutting("a", 0, PropagationStage.POTTED),
                cutting("old", 0, PropagationStage.ESTABLISHED),
            ),
            listOf(potted("a", 18)),
        )
        assertEquals(1, s.samples)
        assertEquals(18, s.medianDays)
        assertEquals(1, s.untracked)
    }

    @Test
    fun `a cutting still rooting is neither a sample nor untracked`() {
        val s = rootingStat(listOf(cutting("a", 0, PropagationStage.ROOTING)), emptyList())
        assertEquals(0, s.samples)
        assertEquals(0, s.untracked)
        assertNull(s.medianDays)
    }

    @Test
    fun `a cutting with no acquisition date cannot be measured`() {
        val s = rootingStat(
            listOf(Plant(id = "a", name = "c", source = PlantSource.CUTTING,
                propagationStage = PropagationStage.POTTED)),
            listOf(potted("a", 18)),
        )
        assertEquals(0, s.samples)
        assertEquals(1, s.untracked)
    }

    @Test
    fun `plants that were never cuttings are ignored entirely`() {
        val s = rootingStat(listOf(plant(PlantStatus.ACTIVE)), emptyList())
        assertEquals(0, s.samples)
        assertEquals(0, s.untracked)
    }

    @Test
    fun `computeStats survives an empty collection`() {
        val s = computeStats(emptyList(), emptyList(), at(2026, 9, 1))
        assertEquals(0, s.plantCount)
        assertNull(s.outcomes.survivalRate)
        assertTrue(s.waterings.all { it.count == 0 })
    }

    @Test
    fun `the table starts at the first watering, not a year of prehistory`() {
        // A new diary was showing eleven rows of dashes for months it did not
        // exist in, burying the one row with anything in it.
        val now = at(2026, 9, 15)
        val months = wateringsByMonth(
            listOf(watering(at(2026, 8, 20)), watering(at(2026, 9, 2))),
            monthsBack = 12,
            nowMillis = now,
        )
        assertEquals(listOf("Aug 2026", "Sep 2026"), months.map { it.label })
    }

    @Test
    fun `gaps inside the record are still kept`() {
        // Starting late must not also close up a genuine quiet spell.
        val now = at(2026, 9, 15)
        val months = wateringsByMonth(
            listOf(watering(at(2026, 6, 4)), watering(at(2026, 9, 2))),
            monthsBack = 12,
            nowMillis = now,
        )
        assertEquals(listOf("Jun 2026", "Jul 2026", "Aug 2026", "Sep 2026"), months.map { it.label })
        assertEquals(listOf(1, 0, 0, 1), months.map { it.count })
    }
}
