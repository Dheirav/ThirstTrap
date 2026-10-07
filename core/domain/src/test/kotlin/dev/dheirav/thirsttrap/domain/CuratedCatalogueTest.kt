package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What it means for an entry to be curated.
 *
 * The file says curated entries "beat the bundled ones on any query they both
 * match: only these say what actually kills the plant". That was the claim, and
 * on 2026-10-08 a third of them did not say it: sixteen of forty-nine carried no
 * commonProblems at all, so for those species the curated tier offered nothing
 * the bundled one did not.
 *
 * These tests make the claim enforceable. A new curated entry without the field
 * that defines the tier now fails here rather than quietly joining it.
 */
class CuratedCatalogueTest {

    @Test
    fun everyCuratedEntrySaysWhatGoesWrong() {
        val thin = curatedSpeciesCatalogue.filter { it.commonProblems.isEmpty() }
        assertTrue(
            "curated entries with no commonProblems: ${thin.map { it.name }}",
            thin.isEmpty(),
        )
    }

    @Test
    fun problemsAreSentencesRatherThanLabels() {
        // "Root rot" is a name for a thing. "Soft translucent leaves means too
        // much water" is what somebody standing over the pot can act on, which
        // is the whole difference between this tier and a plant dictionary.
        val terse = curatedSpeciesCatalogue.flatMap { c ->
            c.commonProblems.filter { it.length < 40 || !it.contains(' ') }.map { c.name to it }
        }
        assertTrue("problems too terse to act on: $terse", terse.isEmpty())
    }

    @Test
    fun everyCuratedEntryCarriesTheOneNumberAUserCannotGuess() {
        // The depletion trigger, per the file's own header. Outside 0.2 to 0.9
        // it is a typo rather than a decision.
        val odd = curatedSpeciesCatalogue.filter { it.depletionTrigger !in 0.2..0.9 }
        assertTrue("implausible depletion triggers: ${odd.map { it.name to it.depletionTrigger }}", odd.isEmpty())
    }

    @Test
    fun aliasesAreNormalisedSoLookupCanMatchThem() {
        // Lookup lowercases and strips punctuation before comparing. An alias
        // with a capital or a full stop in it can never match anything, and
        // would fail silently.
        val bad = curatedSpeciesCatalogue.flatMap { c ->
            c.aliases.filter { it != it.lowercase() || it.trim() != it }.map { c.name to it }
        }
        assertTrue("aliases that can never match: $bad", bad.isEmpty())
    }

    @Test
    fun everyEntryIsReachableByItsOwnName() {
        val unreachable = curatedSpeciesCatalogue.filter { c ->
            findSpeciesCare(c.name) == null
        }
        assertTrue("curated entries their own name cannot find: ${unreachable.map { it.name }}", unreachable.isEmpty())
    }

    @Test
    fun theCuratedTierIsWorthPayingAttentionTo() {
        // A floor rather than a target. If this ever drops it means entries were
        // added without the research that makes them curated.
        val withProblems = curatedSpeciesCatalogue.count { it.commonProblems.isNotEmpty() }
        assertEquals(curatedSpeciesCatalogue.size, withProblems)
        assertTrue("fewer curated species than expected: $withProblems", withProblems >= 49)
    }
}
