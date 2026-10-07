package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ContainersTest {

    @Test
    fun plantsInOneContainerShareAKey() {
        assertEquals(
            containerGroupKey("a", "jar"),
            containerGroupKey("b", "jar"),
        )
    }

    @Test
    fun everyUnsharedPlantKeepsItsOwnKey() {
        // The bug this guards: keying all the unshared plants on one constant
        // collapses the whole shelf into a single notification.
        assertNotEquals(
            containerGroupKey("a", null),
            containerGroupKey("b", null),
        )
    }

    @Test
    fun aSharedPlantAndALonePlantNeverCollide() {
        // "plant:a" must not be reachable as a container id by accident.
        assertNotEquals(
            containerGroupKey("a", null),
            containerGroupKey("b", "a"),
        )
    }

    @Test
    fun namesTheWholePot() {
        assertEquals("Fittonia", potDisplayName(listOf("Fittonia")))
        assertEquals(
            "Fittonia and Fittonia v2",
            potDisplayName(listOf("Fittonia", "Fittonia v2")),
        )
        assertEquals("a, b and c", potDisplayName(listOf("a", "b", "c")))
    }

    @Test
    fun anEmptyPotSaysNothing() {
        assertEquals("", potDisplayName(emptyList()))
    }
}

class PotWateringTest {

    private fun w(id: String, at: Long, group: String? = null,
                  type: CareEventType = CareEventType.WATERED) =
        CareEvent(id = id, plantId = "p", timestampMillis = at, tzOffsetMinutes = 330,
                  type = type, shareGroupId = group)

    @Test
    fun aSharedWateringCountsOnce() {
        // The copies are the same trip to the tap. Counting both would halve
        // the apparent gap and pull the pot's interval down for no reason but
        // the number of plants in it.
        val events = listOf(w("a", 1000, "g1"), w("b", 1000, "g1"))
        assertEquals(listOf(1000L), potWateringMillis(events))
    }

    @Test
    fun unsharedWateringsAllCount() {
        val events = listOf(w("a", 1000), w("b", 2000))
        assertEquals(listOf(1000L, 2000L), potWateringMillis(events))
    }

    @Test
    fun theWholePotsHistoryIsUsed() {
        // The parent's long history and the cutting's short one are one pot's
        // history, which is the whole point.
        val parent = listOf(w("p1", 1000, "g1"), w("p2", 2000, "g2"), w("p3", 3000, "g3"))
        val cutting = listOf(w("c1", 3000, "g3"))
        assertEquals(listOf(1000L, 2000L, 3000L), potWateringMillis(parent + cutting))
    }

    @Test
    fun onlyWateringsCount() {
        val events = listOf(
            w("a", 1000),
            w("b", 2000, type = CareEventType.CHECKED),
            w("c", 3000, type = CareEventType.REPOTTED),
        )
        assertEquals(listOf(1000L), potWateringMillis(events))
    }

    @Test
    fun theResultIsSortedWhateverOrderItArrivesIn() {
        val events = listOf(w("a", 3000), w("b", 1000), w("c", 2000))
        assertEquals(listOf(1000L, 2000L, 3000L), potWateringMillis(events))
    }
}
