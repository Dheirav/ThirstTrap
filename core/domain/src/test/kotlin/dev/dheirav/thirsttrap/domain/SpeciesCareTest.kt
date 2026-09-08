package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeciesCareTest {

    @Test
    fun `the plants actually in this collection are covered`() {
        assertEquals("Peperomia", findSpeciesCare("Peperomia")?.name)
        assertEquals("Creeping fig", findSpeciesCare("Creeping fig")?.name)
    }

    @Test
    fun `matching ignores case, spacing and punctuation`() {
        listOf("PEPEROMIA", "  peperomia  ", "Peperomia!", "peperomia obtusifolia").forEach {
            assertNotNull("failed on '$it'", findSpeciesCare(it))
        }
    }

    @Test
    fun `common names find the plant as readily as botanical ones`() {
        assertEquals("Snake plant", findSpeciesCare("sansevieria")?.name)
        assertEquals("Snake plant", findSpeciesCare("mother in laws tongue")?.name)
        assertEquals("Pothos", findSpeciesCare("devil's ivy")?.name)
        assertEquals("Monstera", findSpeciesCare("swiss cheese plant")?.name)
    }

    @Test
    fun `a more specific name beats a broader one`() {
        // "creeping fig" must not resolve to the fiddle leaf just because both
        // are figs.
        assertEquals("Creeping fig", findSpeciesCare("creeping fig")?.name)
        assertEquals("Fiddle leaf fig", findSpeciesCare("fiddle leaf fig")?.name)
    }

    @Test
    fun `nothing is returned when there is nothing useful to say`() {
        // Flax seeds are a germination experiment, not a houseplant. Inventing
        // care advice for them would be worse than silence.
        assertNull(findSpeciesCare("flax seeds"))
        assertNull(findSpeciesCare("terrarium"))
        assertNull(findSpeciesCare(null))
        assertNull(findSpeciesCare(""))
        assertNull(findSpeciesCare("xy"))
    }

    @Test
    fun `every entry carries the fields the UI depends on`() {
        speciesCatalogue.forEach { e ->
            assertTrue("${e.name} has no aliases", e.aliases.isNotEmpty())
            assertTrue("${e.name} light is empty", e.light.isNotBlank())
            assertTrue("${e.name} water is empty", e.water.isNotBlank())
            assertTrue(
                "${e.name} trigger ${e.depletionTrigger} is outside the sane range",
                e.depletionTrigger in 0.15..0.9,
            )
        }
    }

    @Test
    fun `aliases are stored normalised, or matching silently fails`() {
        speciesCatalogue.forEach { e ->
            e.aliases.forEach { a ->
                assertEquals("${e.name}: alias '$a' is not lowercase", a.lowercase(), a)
                assertTrue("${e.name}: alias '$a' has stray punctuation",
                    a.all { it.isLetterOrDigit() || it == ' ' })
            }
        }
    }

    @Test
    fun `triggers follow the bands in the watering model`() {
        // Succulents and cacti high, moisture-lovers low. Getting these
        // backwards would be worse than having no suggestion at all.
        assertTrue(findSpeciesCare("snake plant")!!.depletionTrigger >= 0.7)
        assertTrue(findSpeciesCare("aloe")!!.depletionTrigger >= 0.7)
        assertTrue(findSpeciesCare("maidenhair fern")!!.depletionTrigger <= 0.35)
        assertTrue(findSpeciesCare("moss")!!.depletionTrigger <= 0.3)
        assertTrue(findSpeciesCare("monstera")!!.depletionTrigger in 0.4..0.6)
    }

    @Test
    fun `regionally ambiguous names resolve the way they are used here`() {
        // "Money plant" means pothos across India, and "money tree" is Pachira
        // almost everywhere. Crassula is called both in different places, so it
        // claims neither - a wrong confident match is worse than no match.
        assertEquals("Pothos", findSpeciesCare("money plant")?.name)
        assertEquals("Money tree", findSpeciesCare("money tree")?.name)
        assertEquals("Jade plant", findSpeciesCare("crassula")?.name)
    }

    @Test
    fun `the terrarium plants are covered`() {
        assertEquals("Fittonia", findSpeciesCare("fittonia")?.name)
        assertEquals("Fittonia", findSpeciesCare("nerve plant")?.name)
        assertNotNull(findSpeciesCare("polka dot plant"))
        assertNotNull(findSpeciesCare("baby tears"))
    }

    @Test
    fun `no two entries claim the same alias`() {
        val seen = mutableMapOf<String, String>()
        speciesCatalogue.forEach { e ->
            e.aliases.forEach { a ->
                val prior = seen.put(a, e.name)
                assertNull("'$a' is claimed by both $prior and ${e.name}", prior)
            }
        }
    }

    @Test
    fun `a hand-written entry always beats a generated one`() {
        // The bundled tier covers the long tail; it must never displace notes
        // that say what actually kills the plant. Both tiers know Peperomia and
        // Monstera; the curated one has to answer.
        listOf("peperomia", "monstera", "calathea", "fittonia", "aloe vera").forEach {
            assertEquals(
                "'$it' was answered by the generated tier",
                CareDetail.CURATED,
                findSpeciesCare(it)?.detail,
            )
        }
    }

    @Test
    fun `the generated tier covers plants the hand-written one never did`() {
        // None of these were in the original 48.
        listOf("clivia", "aphelandra", "kentia palm", "gynura", "cast iron plant").forEach {
            val hit = findSpeciesCare(it)
            assertNotNull("no entry for '$it'", hit)
            assertEquals(CareDetail.BUNDLED, hit!!.detail)
        }
    }

    @Test
    fun `an old name on the label still finds the plant`() {
        // The dataset is decades out of date in places and so are plant labels.
        // GBIF resolved these at build time, so both spellings have to work.
        assertNotNull(findSpeciesCare("brassaia actinophylla"))
        assertNotNull(findSpeciesCare("heptapleurum actinophyllum"))
        // And a synonym of something the curated tier owns must land on the
        // curated entry, not on a second entry for the same plant.
        assertEquals("Pothos", findSpeciesCare("scindapsus aureus")?.name)
        assertEquals("Aloe vera", findSpeciesCare("aloe barbadensis")?.name)
    }

    @Test
    fun `every generated entry says where it came from`() {
        speciesCatalogue.filter { it.detail == CareDetail.BUNDLED }.forEach {
            assertTrue("${it.name} has no source", !it.source.isNullOrBlank())
        }
    }

    @Test
    fun `the two tiers never contradict each other`() {
        // The generator drops any entry the curated tier already answers, so no
        // query can return two different depletion triggers depending on how it
        // was spelled. This asserts the generator actually did that.
        speciesCatalogue.filter { it.detail == CareDetail.BUNDLED }.forEach { bundled ->
            val curatedHit = findSpeciesCare(
                bundled.botanical ?: bundled.name,
                curatedSpeciesCatalogue,
            )
            assertNull(
                "${bundled.name} is also answered by the curated entry " +
                    "'${curatedHit?.name}' - one of them has to go",
                curatedHit,
            )
        }
    }

    @Test
    fun `christmas cactus is not treated as a desert cactus`() {
        // The generated tier disagreed with the old shared Cactus entry and was
        // right: Schlumbergera is an epiphyte. Drying it to 0.85 is how they die.
        val holiday = findSpeciesCare("christmas cactus")
        assertEquals("Christmas cactus", holiday?.name)
        assertTrue(holiday!!.depletionTrigger <= 0.6)
        assertTrue(findSpeciesCare("cactus")!!.depletionTrigger >= 0.8)
        assertEquals("Christmas cactus", findSpeciesCare("zygocactus")?.name)
    }

    @Test
    fun `generated entries still refuse to answer for things that are not plants`() {
        // The long tail must not have quietly made the catalogue credulous.
        assertNull(findSpeciesCare("flax seeds"))
        assertNull(findSpeciesCare("terrarium"))
        assertNull(findSpeciesCare("kitchen windowsill"))
    }
}
