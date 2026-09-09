package dev.dheirav.thirsttrap.domain

/**
 * Everything the dashboard needs about one plant, already resolved.
 * The UI sorts by [attentionScore] and renders; it does no reasoning of its own.
 */
data class PlantAttention(
    val plant: Plant,
    val lastWateredMillis: Long?,
    val lastCheckedMillis: Long?,
    val reminderDueMillis: Long?,
    val depletion: Double?,
    val prediction: Prediction,
    /** Requirements item 8: "waters roughly every 8 days". Null until measurable. */
    val averageIntervalDays: Double? = null,
    /** Absolute path to the most recent photo, if there is one. */
    val coverPhotoPath: String? = null,
    /** The plant's set standard, or the last amount logged. Null if neither. */
    val suggestedWaterMl: Double? = null,
)

/**
 * Descending priority, per docs/UI-SPEC.md §3:
 *   1. overdue reminders, most overdue first
 *   2. calibrated plants already past their trigger
 *   3. predicted to need water within 24h
 *   4. longest since last checked
 *   5. name, as a stable tiebreak
 *
 * Deliberately *not* a single weighted number: a plant that is genuinely
 * overdue must always outrank one that merely has not been checked lately, and
 * a weighted score lets a long-uncheck accumulate past a real overdue.
 */
fun attentionRank(item: PlantAttention, nowMillis: Long): Int = when {
    item.reminderDueMillis != null && item.reminderDueMillis <= nowMillis -> 0
    item.prediction is Prediction.WaterNow -> 1
    item.prediction is Prediction.Eta && item.prediction.days <= 1.0 -> 2
    else -> 3
}

fun sortByAttention(items: List<PlantAttention>, nowMillis: Long): List<PlantAttention> =
    items.sortedWith(
        compareBy<PlantAttention> { attentionRank(it, nowMillis) }
            .thenBy { it.reminderDueMillis ?: Long.MAX_VALUE }
            .thenByDescending {
                val last = it.lastCheckedMillis ?: it.lastWateredMillis
                last?.let { t -> nowMillis - t } ?: Long.MAX_VALUE
            }
            .thenBy { it.plant.name.lowercase() }
    )

/**
 * Average days between waterings, from the log. Requirements item 8.
 * Null until there are two waterings to measure between.
 */
fun averageWateringIntervalDays(wateringTimestampsMillis: List<Long>): Double? {
    val sorted = wateringTimestampsMillis.sorted()
    if (sorted.size < 2) return null
    val gaps = sorted.zipWithNext { a, b -> (b - a) / MILLIS_PER_DAY }
    return gaps.average()
}

/**
 * Days between waterings, but robust enough to schedule from.
 *
 * The plain average is the right thing to *show* someone, while it is a poor
 * thing to plan on, because the log is not a clean series. Two waterings hours
 * apart are one episode, a top-up or a correction, not a rhythm, and averaging
 * across a pair like that produced a Fittonia that fell due the next morning.
 *
 * So same-day repeats collapse, and there have to be at least two real gaps
 * left before the log is allowed to speak at all. The median of what remains
 * survives one unusual gap, which an average does not.
 */
fun checkIntervalFromLogDays(wateringTimestampsMillis: List<Long>): Double? {
    val sorted = wateringTimestampsMillis.sorted()
    val gaps = sorted.zipWithNext { a, b -> (b - a) / MILLIS_PER_DAY }
        .filter { it >= SAME_EPISODE_DAYS }
    if (gaps.size < 2) return null
    val ordered = gaps.sorted()
    val mid = ordered.size / 2
    return if (ordered.size % 2 == 1) {
        ordered[mid]
    } else {
        (ordered[mid - 1] + ordered[mid]) / 2.0
    }
}

/** Below this, two waterings are the same watering. */
private const val SAME_EPISODE_DAYS = 0.5
