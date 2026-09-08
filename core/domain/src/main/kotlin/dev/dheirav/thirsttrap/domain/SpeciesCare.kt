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
/**
 * How much is actually known about an entry.
 *
 * The two tiers come from genuinely different sources and it would be dishonest
 * to render them identically. [CURATED] entries are written by hand: the failure
 * modes, the "what usually goes wrong", the reason this particular plant dies.
 * [BUNDLED] entries are derived from an open dataset of ordinal codes - roughly
 * "how bright", "how wet", "how humid" - and can say no more than that.
 *
 * The UI shows the tier, so a thin entry never wears the authority of a thick one.
 */
enum class CareDetail {
    CURATED,
    BUNDLED,
}

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
    val detail: CareDetail = CareDetail.CURATED,
    /**
     * Where a [CareDetail.BUNDLED] entry's numbers came from, shown verbatim in
     * the UI. Public-domain sources only, so there is nothing to attribute and
     * nothing that can be withdrawn.
     */
    val source: String? = null,
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
 * Normalised aliases for the bundled catalogue, computed once.
 *
 * This used to normalise every alias on every lookup, which was free at 48
 * entries and is not at 250-odd: the plant detail screen calls [hasSpeciesCare]
 * during composition, so the cost lands on every recomposition.
 */
private val defaultAliasIndex: List<List<String>> by lazy {
    speciesCatalogue.map { entry -> entry.aliases.map(::normalise) }
}

// Identity, not equality: comparing list contents would cost more than the
// normalising it saves. Only the shared catalogue is worth caching; the small
// ones the tests pass are cheap to normalise outright.
private fun aliasIndex(catalogue: List<SpeciesCare>): List<List<String>> =
    if (catalogue === speciesCatalogue) defaultAliasIndex
    else catalogue.map { entry -> entry.aliases.map(::normalise) }

/** Whole-word containment, not arbitrary substring. */
private fun containsPhrase(haystack: String, needle: String): Boolean =
    " $haystack ".contains(" $needle ")

/**
 * Finds care notes for whatever the user typed.
 *
 * Matching is deliberately generous - people type "peperomia" for any of forty
 * species, "ficus" for a genus spanning a fig tree and a creeping vine - and
 * deliberately silent when it is not confident. Guessing wrong here would put
 * confident, specific advice against the wrong plant.
 *
 * A hand-written entry always beats a generated one on the same query. The
 * bundled tier exists to cover the long tail, never to displace notes that
 * actually say what kills the plant.
 */
fun findSpeciesCare(query: String?, catalogue: List<SpeciesCare> = speciesCatalogue): SpeciesCare? {
    val q = normalise(query.orEmpty())
    if (q.length < 3) return null

    val aliases = aliasIndex(catalogue)

    // Rank: curated before bundled, then the longest matching alias, which is
    // the most specific - so "creeping fig" resolves to the vine rather than to
    // every Ficus.
    fun rank(i: Int, matched: Int): Long =
        (if (catalogue[i].detail == CareDetail.CURATED) 1_000_000L else 0L) + matched

    var bestIndex = -1
    var bestRank = -1L

    catalogue.indices.forEach { i ->
        val exact = aliases[i].filter { it == q }.maxOfOrNull { it.length }
        // An exact hit outranks any containment hit, curated or not.
        val matched = exact?.plus(10_000) ?: aliases[i]
            .filter { containsPhrase(q, it) || containsPhrase(it, q) }
            .maxOfOrNull { it.length }
        if (matched != null) {
            val r = rank(i, matched)
            if (r > bestRank) {
                bestRank = r
                bestIndex = i
            }
        }
    }

    return bestIndex.takeIf { it >= 0 }?.let { catalogue[it] }
}

/** True when the app has nothing useful to say, so the UI can be honest about it. */
fun hasSpeciesCare(query: String?): Boolean = findSpeciesCare(query) != null
