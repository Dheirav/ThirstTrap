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
