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

/**
 * One pot, one watering history.
 *
 * A reminder interval derived from a plant's own log gives two different answers
 * for two plants in the same jar, because they have been in it for different
 * lengths of time. On 2026-10-07 the Fittonia had 22 waterings and its cutting
 * had one, so the parent's interval resolved to a day and the cutting's fell
 * through to the seven-day default for a new plant, off the same shared
 * watering. Merging the notifications does not fix that: the worker collapses
 * reminders that are due in the same sweep, and two plants on a one-day and a
 * seven-day clock are never due in the same sweep.
 *
 * So the inputs are merged instead of the outputs. Every watering of any plant
 * in the pot is a watering of the pot, and the pot is what gets carried to the
 * tap.
 *
 * Shared copies are counted once. A watering logged on one plant and fanned out
 * to the others is one event; counting it per plant would halve the apparent
 * gap between waterings and pull the interval down for every extra plant in the
 * jar, which would be an interval derived from how many plants share a pot
 * rather than from how often it is watered.
 */
fun potWateringMillis(events: List<CareEvent>): List<Long> {
    val seenGroups = HashSet<String>()
    val out = ArrayList<Long>()
    for (e in events) {
        if (e.type != CareEventType.WATERED) continue
        val g = e.shareGroupId
        if (g != null && !seenGroups.add(g)) continue
        out.add(e.timestampMillis)
    }
    return out.sorted()
}
