package dev.dheirav.thirsttrap.domain

/**
 * What the record says about how a plant died. Requirements item 24.
 *
 * The point is not to assign blame - it is that the answer is usually already
 * in the log, and nobody reads a log unprompted. Killing a plant is the moment
 * you are most willing to look, and the least likely to remember what happened
 * two months ago.
 */
data class PostMortemRecap(
    val daysOwned: Int?,
    val totalWaterings: Int,
    val averageIntervalDays: Double?,
    val lastWateredDaysAgo: Int?,
    val photoCount: Int,
    /** The events worth surfacing: repots, medium changes, treatments, pests. */
    val keyEvents: List<CareEvent>,
    val observations: List<String>,
)

/**
 * Notes worth putting in front of someone, drawn from the log rather than
 * guessed at. Each is phrased as something that happened, not a verdict -
 * the record can say a plant was watered every three days; it cannot say that
 * is what killed it.
 */
fun buildPostMortemObservations(
    events: List<CareEvent>,
    averageIntervalDays: Double?,
    nowMillis: Long,
): List<String> = buildList {
    val waterings = events.filter { it.type == CareEventType.WATERED }

    averageIntervalDays?.let { avg ->
        when {
            avg < 2.5 -> add(
                "Watered about every ${avg.toInt()} days, which is often for most " +
                    "things in soil. Worth asking whether the pot was drying out between."
            )
            avg > 21 -> add(
                "Watered roughly every ${avg.toInt()} days. Long gaps suit succulents " +
                    "and little else."
            )
        }
    }

    if (waterings.size >= 3) {
        val gaps = waterings.map { it.timestampMillis }.sorted().zipWithNext { a, b ->
            (b - a) / 86_400_000.0
        }
        val longest = gaps.max()
        // Compared against the median of the OTHER gaps, not the mean of all
        // of them. A single long gap drags the mean up far enough to hide
        // itself - 34 days among 3-day gaps only lifts the mean to 13, and 34
        // does not clear three times that. The median of the rest is what the
        // eye actually compares it to.
        val others = gaps.filter { it != longest }.sorted()
        val typical = others.getOrNull(others.size / 2)
        if (typical != null && typical > 0 && longest > typical * 3) {
            add(
                "There was one gap of ${longest.toInt()} days, against ${typical.toInt()} " +
                    "for the rest."
            )
        }
    }

    events.firstOrNull { it.type == CareEventType.REPOTTED }?.let {
        val days = ((nowMillis - it.timestampMillis) / 86_400_000L).toInt()
        if (days < 30) {
            add("Repotted $days days ago. Roots take a few weeks to settle after that.")
        }
    }

    events.firstOrNull { it.type == CareEventType.MEDIUM_CHANGED }?.let {
        val days = ((nowMillis - it.timestampMillis) / 86_400_000L).toInt()
        add("Moved to a different medium $days days ago - a high-risk change.")
    }

    if (events.any { it.type == CareEventType.PEST_OR_DISEASE }) {
        add("A pest or disease was logged. Worth checking whatever it was is not still around.")
    }

    if (waterings.isEmpty()) {
        add("No waterings were ever logged, so the record cannot say much here.")
    }
}
