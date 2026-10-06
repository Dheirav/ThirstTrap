package dev.dheirav.thirsttrap.domain

/**
 * Plants that share a pot, and how to speak about them as one.
 *
 * This lives in the domain rather than in the reminder worker because the
 * worker is an Android class that needs a device to run, and the part worth
 * pinning is a decision about words and grouping that needs nothing at all.
 */

/**
 * What to collapse a due reminder under.
 *
 * A plant in its own pot keys on itself, so the ordinary case stays one
 * notification per plant exactly as before. Keying everything unshared on a
 * single constant would have swept every separate plant into one group, which
 * is the obvious way to write this and is wrong.
 */
fun containerGroupKey(plantId: String, containerId: String?): String =
    containerId ?: "plant:$plantId"

/**
 * "the Fittonia", "Fittonia and Fittonia v2", "a, b and c".
 *
 * Both plants are named rather than one, because a notification about a pot
 * that mentions only half of what is in it sends you to one plant and leaves
 * you wondering about the other.
 */
fun potDisplayName(names: List<String>): String = when (names.size) {
    0 -> ""
    1 -> names[0]
    2 -> "${names[0]} and ${names[1]}"
    else -> names.dropLast(1).joinToString(", ") + " and ${names.last()}"
}
