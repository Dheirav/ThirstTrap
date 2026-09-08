package dev.dheirav.thirsttrap.domain

/**
 * When a logged event actually happened.
 *
 * Every entry used to carry the moment its screen was opened, so recording
 * last night's watering this morning shifted it by half a day - and both the
 * cadence figure and the drying model are built from those intervals.
 */
enum class WhenLogged(val label: String) {
    NOW("Just now"),
    EARLIER_TODAY("Earlier today"),
    YESTERDAY("Yesterday"),
    PICK("Another day"),
}

/**
 * Backdated entries land at **midday**, not midnight.
 *
 * Midnight would let a watering recorded for "yesterday" sort ahead of one
 * genuinely logged early that morning, and would sit close enough to a day
 * boundary that a timezone offset could move it to the wrong day. Midday is
 * far from both edges.
 *
 * @param offsetMinutes the local UTC offset, so "midday" means midday where
 *        the user is rather than midday UTC.
 */
fun middayOf(millis: Long, offsetMinutes: Int): Long {
    val offsetMillis = offsetMinutes * 60_000L
    val localDayStart = Math.floorDiv(millis + offsetMillis, 86_400_000L) * 86_400_000L
    return localDayStart - offsetMillis + 12 * 60 * 60 * 1000L
}

/**
 * The timestamp to record. Never in the future: an "earlier today" logged at
 * 09:00 must not be filed at midday, which has not happened yet.
 */
fun resolveLoggedAt(
    whenLogged: WhenLogged,
    nowMillis: Long,
    offsetMinutes: Int,
    pickedDateMillis: Long? = null,
): Long = when (whenLogged) {
    WhenLogged.NOW -> nowMillis
    WhenLogged.EARLIER_TODAY -> middayOf(nowMillis, offsetMinutes).coerceAtMost(nowMillis)
    WhenLogged.YESTERDAY -> middayOf(nowMillis - 86_400_000L, offsetMinutes)
    WhenLogged.PICK -> pickedDateMillis
        ?.let { middayOf(it, offsetMinutes).coerceAtMost(nowMillis) }
        ?: nowMillis
}
