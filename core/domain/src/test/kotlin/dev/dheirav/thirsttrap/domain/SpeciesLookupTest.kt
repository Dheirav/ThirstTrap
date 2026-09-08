package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeciesLookupTest {

    @Test
    fun `a real species is usable`() {
        assertTrue(isUsableMatch("EXACT", "SPECIES", 97))
        assertTrue(isUsableMatch("EXACT", "SUBSPECIES", 95))
        assertTrue(isUsableMatch("EXACT", "VARIETY", 92))
    }

    @Test
    fun `a genus is a real answer to a vague question`() {
        // Someone who typed "ficus" meant the genus and should be told so.
        assertTrue(isUsableMatch("EXACT", "GENUS", 96))
    }

    @Test
    fun `the kingdom is not an answer`() {
        // The bug this exists for. GBIF answers "Flax seeds" with
        // matchType HIGHERRANK, rank KINGDOM, canonicalName Plantae, at 99
        // confidence - and the screen rendered "your plant is: Plantae" with an
        // article about photosynthesis.
        assertFalse(isUsableMatch("HIGHERRANK", "KINGDOM", 99))
        assertFalse(isUsableMatch("EXACT", "KINGDOM", 99))
    }

    @Test
    fun `family and order are too coarse to show anyone`() {
        assertFalse(isUsableMatch("EXACT", "FAMILY", 98))
        assertFalse(isUsableMatch("EXACT", "ORDER", 98))
        assertFalse(isUsableMatch("EXACT", "PHYLUM", 98))
    }

    @Test
    fun `a weak fuzzy match is a wrong plant, not a near miss`() {
        // This screen has no way to render doubt, so it does not accept a guess
        // it would have to hedge about.
        assertFalse(isUsableMatch("FUZZY", "SPECIES", 62))
        assertTrue(isUsableMatch("FUZZY", "SPECIES", 88))
    }

    @Test
    fun `no match and missing fields are refused`() {
        assertFalse(isUsableMatch("NONE", null, null))
        assertFalse(isUsableMatch(null, "SPECIES", 99))
        assertFalse(isUsableMatch("EXACT", null, 99))
    }
}
