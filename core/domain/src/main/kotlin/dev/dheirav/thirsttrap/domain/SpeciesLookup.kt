package dev.dheirav.thirsttrap.domain

/**
 * Name resolution, not care advice.
 *
 * When the bundled catalogue has nothing, the honest answer is "no care notes
 * for this one - here is what the plant actually is". That is a very different
 * promise from a care API: it cannot be wrong about how often to water,
 * because it never says. Nothing here feeds [SpeciesCare], the depletion
 * trigger, or any prediction.
 *
 * Both services behind it are free, keyless and public-good. If either ever
 * disappears the app loses a link and nothing else - the catalogue, the
 * predictions and every screen carry on working with no network at all.
 */
data class SpeciesLookup(
    /** What the user typed. */
    val query: String,
    /** GBIF's accepted name, which is frequently not what was typed. */
    val acceptedName: String,
    val family: String? = null,
    /**
     * Set when the typed name is a synonym GBIF rewrote - "Scindapsus aureus"
     * for Epipremnum aureum. Worth showing: it is usually the moment a user
     * discovers their plant has another name, and it is why the catalogue
     * looked empty.
     */
    val synonymOf: String? = null,
    val wikipediaTitle: String? = null,
    val wikipediaUrl: String? = null,
    val wikipediaExtract: String? = null,
)

/** Why a lookup produced nothing, so the UI can say which. */
enum class LookupFailure {
    /** The user has not turned online lookup on. Never assume; always ask. */
    NOT_ENABLED,
    OFFLINE,
    /** Reached the service, and it genuinely does not know this name. */
    NO_MATCH,
    /** Reached the service and it failed. Different from NO_MATCH on purpose. */
    SERVICE_ERROR,
}

sealed interface LookupResult {
    data class Found(val lookup: SpeciesLookup) : LookupResult
    data class Failed(val reason: LookupFailure) : LookupResult
}

/**
 * Whether a GBIF match is specific enough to show.
 *
 * GBIF always answers something. Ask it for "Flax seeds" and it returns
 * `matchType: HIGHERRANK`, `rank: KINGDOM`, `canonicalName: Plantae` at 99
 * confidence - technically correct, and useless. Shown to a user it reads as
 * "your plant is: Plantae", with an encyclopaedia article about photosynthesis
 * attached. A confident wrong answer is worse than no answer, which is the
 * whole reason this feature resolves names instead of fetching care advice.
 *
 * Genus is kept: "Ficus" is a real answer to "some kind of fig". Family and
 * above are not - nobody typed a plant name meaning to be told "Moraceae".
 */
fun isUsableMatch(matchType: String?, rank: String?, confidence: Int?): Boolean {
    if (matchType == null || matchType == "NONE" || matchType == "HIGHERRANK") return false
    val usableRank = rank in setOf("SPECIES", "SUBSPECIES", "VARIETY", "FORM", "GENUS")
    if (!usableRank) return false
    // A fuzzy match is a guess about what was meant. A weak guess is a wrong
    // plant, and this screen has no way to signal doubt.
    return matchType != "FUZZY" || (confidence ?: 0) >= 80
}

interface SpeciesLookupService {
    /**
     * Resolves a typed name. Implementations must return
     * [LookupFailure.NOT_ENABLED] rather than touching the network when the
     * user has not opted in - the offline-first promise in the requirements is
     * a promise about packets, not about intent.
     */
    suspend fun lookUp(query: String): LookupResult
}
