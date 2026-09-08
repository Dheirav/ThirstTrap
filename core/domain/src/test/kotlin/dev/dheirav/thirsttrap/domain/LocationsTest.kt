package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationsTest {

    private var n = 0
    private fun plant(location: String?) = Plant(id = "p${n++}", name = "x", location = location)
    private fun note(name: String, lux: Float? = null, text: String? = null) =
        LocationNote(name = name, note = text, lux = lux)

    @Test
    fun `places come from the plants and from the notes together`() {
        // A place with nothing in it is usually the spot somebody is deciding
        // whether to move something to, which is exactly when the light note
        // matters.
        val names = knownLocations(
            listOf(plant("Windowsill"), plant("Desk")),
            listOf(note("Bathroom shelf")),
        )
        assertEquals(listOf("Bathroom shelf", "Desk", "Windowsill"), names)
    }

    @Test
    fun `one place however it was capitalised`() {
        val names = knownLocations(
            listOf(plant("Windowsill"), plant("windowsill"), plant("WINDOWSILL")),
            emptyList(),
        )
        assertEquals(1, names.size)
    }

    @Test
    fun `blank and missing locations are not places`() {
        assertTrue(knownLocations(listOf(plant(null), plant(""), plant("   ")), emptyList()).isEmpty())
    }

    @Test
    fun `plants are counted per place, case-insensitively`() {
        val counts = plantsPerLocation(
            listOf(plant("Windowsill"), plant("windowsill"), plant("Desk"), plant(null)),
        )
        assertEquals(2, counts["windowsill"])
        assertEquals(1, counts["desk"])
        assertEquals(2, counts.size)
    }

    @Test
    fun `a light reading classifies itself`() {
        assertEquals(LightLevel.BRIGHT_INDIRECT, note("x", lux = 5_000f).level)
        assertEquals(LightLevel.DEEP_SHADE, note("x", lux = 50f).level)
        assertNull(note("x").level)
    }

    @Test
    fun `a place with neither a note nor a reading has nothing to show`() {
        assertFalse(note("x").hasAnything)
        assertTrue(note("x", text = "north facing").hasAnything)
        assertTrue(note("x", lux = 900f).hasAnything)
        assertFalse(note("x", text = "   ").hasAnything)
    }
}
