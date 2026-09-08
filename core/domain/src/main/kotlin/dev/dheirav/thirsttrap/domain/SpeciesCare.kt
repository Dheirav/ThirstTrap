package dev.dheirav.thirsttrap.domain

/**
 * Care notes for a species, bundled with the app.
 *
 * Offline by construction: no key, no quota, nothing to go stale when a service
 * shuts down. Coverage is deliberately narrow and deep - common houseplants,
 * written properly - rather than thousands of shallow entries.
 *
 * Everything here is a **starting point**. Generic advice says "water when the
 * top few centimetres are dry"; weighing the pot says what this particular
 * plant in this particular flat actually does, which is the whole reason the
 * app exists. The care card fills the gap before there are enough readings to
 * know better.
 */
data class SpeciesCare(
    /** The name shown. */
    val name: String,
    val botanical: String? = null,
    /**
     * Everything this should match on: lowercase, no punctuation. Common names
     * and the spellings people actually type.
     */
    val aliases: List<String>,
    val light: String,
    val water: String,
    val medium: Medium,
    /** Suggested depletion trigger - the number a user would otherwise guess. */
    val depletionTrigger: Double,
    val humidity: String? = null,
    /** Blunt, because people have cats. */
    val toxicity: String? = null,
    val commonProblems: List<String> = emptyList(),
    val note: String? = null,
) {
    /** Prefills the plant's own care profile from this entry. */
    fun asTargetDryness(): String = water
}

private fun normalise(raw: String): String =
    raw.lowercase().trim()
        // Apostrophes are DROPPED, not turned into spaces: "devil's ivy" has to
        // become "devils ivy" to match, not "devil s ivy".
        .replace("'", "")
        .replace("\u2019", "")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

/**
 * Finds care notes for whatever the user typed.
 *
 * Matching is deliberately generous - people type "peperomia" for any of forty
 * species, "ficus" for a genus spanning a fig tree and a creeping vine - and
 * deliberately silent when it is not confident. Guessing wrong here would put
 * confident, specific advice against the wrong plant.
 */
fun findSpeciesCare(query: String?, catalogue: List<SpeciesCare> = speciesCatalogue): SpeciesCare? {
    val q = normalise(query.orEmpty())
    if (q.length < 3) return null

    // Aliases are normalised at comparison time as well as being stored clean.
    // Without this, "devil's ivy" typed by a user becomes "devil s ivy" and
    // never matches an alias that kept its apostrophe.
    fun aliasesOf(entry: SpeciesCare) = entry.aliases.map(::normalise)

    catalogue.firstOrNull { entry -> aliasesOf(entry).any { it == q } }?.let { return it }

    // Whole-word containment, not arbitrary substring. "terrarium" should not
    // find "terrarium moss": a terrarium is a container, and a plant this
    // confident about care advice must not be attached to the wrong plant.
    fun containsPhrase(haystack: String, needle: String): Boolean =
        " $haystack ".contains(" $needle ")

    val matches = catalogue.filter { entry ->
        aliasesOf(entry).any { alias -> containsPhrase(q, alias) || containsPhrase(alias, q) }
    }

    // The longest matching alias wins, being the most specific - so "creeping
    // fig" resolves to the vine rather than to every Ficus.
    return matches.maxByOrNull { entry ->
        aliasesOf(entry)
            .filter { containsPhrase(q, it) || containsPhrase(it, q) }
            .maxOf { it.length }
    }
}

/** True when the app has nothing useful to say, so the UI can be honest about it. */
fun hasSpeciesCare(query: String?): Boolean = findSpeciesCare(query) != null
