package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarePrefillTest {

    private val care = requireNotNull(findSpeciesCare("monstera")) {
        "the catalogue is expected to carry a monstera; the prefill has nothing to test without one"
    }

    @Test
    fun `fills every field the form has left unanswered`() {
        val p = carePrefill(care, "", "", Medium.SOIL, DEFAULT_DEPLETION_TRIGGER_PCT)
        assertEquals(care.light, p.light)
        assertEquals(care.asTargetDryness(), p.dryness)
        assertEquals((care.depletionTrigger * 100).toInt(), p.depletionTriggerPct)
    }

    @Test
    fun `an answer the user typed always wins`() {
        val p = carePrefill(care, "south window", "bone dry", Medium.SOIL, DEFAULT_DEPLETION_TRIGGER_PCT)
        assertNull("light was typed, so it must not be replaced", p.light)
        assertNull("dryness was typed, so it must not be replaced", p.dryness)
    }

    @Test
    fun `a medium the user picked is not undone by typing the species afterwards`() {
        // A cutting in a jar of water weighs what the jar weighs. Someone who
        // said WATER must not have it silently reset to the catalogue's soil.
        val p = carePrefill(care, "", "", Medium.WATER, DEFAULT_DEPLETION_TRIGGER_PCT)
        assertNull(p.medium)
    }

    @Test
    fun `a trigger the user moved off the default is left alone`() {
        val p = carePrefill(care, "", "", Medium.SOIL, 65)
        assertNull(p.depletionTriggerPct)
    }

    @Test
    fun `nothing to say when the form is already fully answered`() {
        val p = carePrefill(care, "x", "y", Medium.WATER, 65)
        assertTrue(p.isEmpty)
        assertEquals(emptyList<String>(), p.filled)
    }

    @Test
    fun `filled names read in the order the fields appear on the form`() {
        val p = carePrefill(care, "", "", Medium.SOIL, DEFAULT_DEPLETION_TRIGGER_PCT)
        assertEquals(listOf("light", "dryness", "the depletion trigger"), p.filled - "growing medium")
    }
}
