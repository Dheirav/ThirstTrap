package dev.dheirav.thirsttrap.domain

/**
 * A prompt to *assess*, never an instruction to water blindly.
 * docs/NOTIFICATIONS.md section 5.
 */
data class Reminder(
    val id: String,
    val plantId: String,
    val kind: ReminderKind,
    /** Task reminders only. */
    val title: String? = null,
    /** Check reminders. Null means "derive it from the log". */
    val intervalDays: Int? = null,
    val nextDueAtMillis: Long,
    val enabled: Boolean = true,
    val snoozedUntilMillis: Long? = null,
    val lastFiredAtMillis: Long? = null,
) {
    fun isDue(nowMillis: Long): Boolean =
        enabled &&
            nextDueAtMillis <= nowMillis &&
            (snoozedUntilMillis == null || snoozedUntilMillis <= nowMillis)
}

enum class ReminderKind { CHECK, TASK }

/** Fallback for a brand-new plant with no history at all. */
const val DEFAULT_CHECK_INTERVAL_DAYS = 7

/**
 * How long to wait before asking the user to check this plant again.
 *
 * Priority order, per docs/NOTIFICATIONS.md section 5:
 *   1. the user's explicit per-plant setting
 *   2. the prediction from the weight model - the whole point of the app is
 *      that the reminder tracks the measured pot, not a calendar
 *   3. the average interval computed from the log
 *   4. a week, as a last resort
 */
fun resolveIntervalDays(
    explicitIntervalDays: Int?,
    prediction: Prediction?,
    loggedAverageDays: Double?,
): Int {
    explicitIntervalDays?.let { return it.coerceAtLeast(1) }

    if (prediction is Prediction.Eta && !prediction.capped) {
        return prediction.days.toInt().coerceAtLeast(1)
    }
    loggedAverageDays?.let { return it.toInt().coerceAtLeast(1) }
    return DEFAULT_CHECK_INTERVAL_DAYS
}

/**
 * The next due time, measured from the last time the plant was *assessed*.
 *
 * A check that concluded "still wet" resets the clock exactly as a watering
 * does, because both mean the pot was recently looked at. Counting only
 * waterings would nag someone for being careful.
 */
fun computeNextDue(
    lastAssessedMillis: Long?,
    intervalDays: Int,
    nowMillis: Long,
): Long {
    val from = lastAssessedMillis ?: nowMillis
    return from + intervalDays * 86_400_000L
}
