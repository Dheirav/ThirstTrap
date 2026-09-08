package dev.dheirav.thirsttrap.domain

import kotlinx.coroutines.flow.Flow

/**
 * What a place in the house is like.
 *
 * Requirement 13 asks for light and placement notes per location. Until now a
 * location was a free-text string on a plant and nothing else - four pots could
 * say "windowsill" and the app knew nothing about the windowsill.
 *
 * This gives the place an identity of its own: a note about the aspect and what
 * the sun does there, and the last light reading taken in it, so [F22]'s meter
 * leaves a record instead of a number that vanishes when the screen closes.
 *
 * Still keyed by the name rather than by an id, which is the same trade
 * [AmbientReading] makes and for the same reason: a locations table with real
 * ids would mean a picker, a rename flow and a migration for every plant, in
 * exchange for solving a problem nobody has reported. Renaming a location still
 * orphans its note, and that is written down rather than hidden.
 */
data class LocationNote(
    /** The location string as typed on a plant. Matched case-insensitively. */
    val name: String,
    val note: String? = null,
    /** Last measured light, from the meter. */
    val lux: Float? = null,
    val luxMeasuredAtMillis: Long? = null,
    val updatedAtMillis: Long = 0L,
) {
    val level: LightLevel? get() = lux?.let { classifyLux(it) }
    val hasAnything: Boolean get() = !note.isNullOrBlank() || lux != null
}

interface LocationRepository {
    fun observeAll(): Flow<List<LocationNote>>
    suspend fun get(name: String): LocationNote?
    suspend fun setNote(name: String, note: String?)
    suspend fun recordLight(name: String, lux: Float, atMillis: Long)
    suspend fun delete(name: String)
}

/**
 * Every place a plant currently lives, plus every place that has a note.
 *
 * A location with no plants in it is still worth listing - it is usually the
 * spot somebody is deciding whether to move something to, which is the whole
 * point of writing down what the light is like there.
 */
fun knownLocations(plants: List<Plant>, notes: List<LocationNote>): List<String> =
    (plants.mapNotNull { it.location?.takeIf { l -> l.isNotBlank() } } + notes.map { it.name })
        .distinctBy { it.lowercase() }
        .sortedBy { it.lowercase() }

/** How many plants currently live in each location, keyed lowercase. */
fun plantsPerLocation(plants: List<Plant>): Map<String, Int> =
    plants.mapNotNull { it.location?.takeIf { l -> l.isNotBlank() }?.lowercase() }
        .groupingBy { it }
        .eachCount()
