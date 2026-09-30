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

/**
 * Which local day an instant falls on, as a day number.
 *
 * The offset is a parameter rather than read from the clock because the same
 * instant is a different day depending on where you were standing, and this app
 * stores an offset per entry precisely so that question has an answer.
 */
fun localDayIndex(utcMillis: Long, offsetMinutes: Int): Long =
    Math.floorDiv(utcMillis + offsetMinutes * 60_000L, MILLIS_PER_DAY.toLong())

/**
 * Whole calendar days between two instants, read in one frame.
 *
 * Not `(now - then) / MILLIS_PER_DAY`, which is elapsed time and a different
 * question. Dividing gives 0 for anything inside 24 hours, so a plant watered
 * at 23:00 last night read "Watered today" at 08:00 this morning. That is the
 * single most-read line on a plant card and it was wrong for nine hours of
 * every day.
 */
fun calendarDaysAgo(nowMillis: Long, thenMillis: Long, offsetMinutes: Int): Int =
    (localDayIndex(nowMillis, offsetMinutes) - localDayIndex(thenMillis, offsetMinutes)).toInt()
