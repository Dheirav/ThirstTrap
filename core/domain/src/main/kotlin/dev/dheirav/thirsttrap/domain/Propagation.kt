package dev.dheirav.thirsttrap.domain

import kotlinx.serialization.Serializable

/**
 * Where a cutting has got to. Requirements item 20.
 *
 * The stages are the ones that actually matter to a person holding a cutting,
 * and the order is fixed - things move forward, or they fail. There is no
 * "dead" stage because a failed cutting is a plant with status DEAD like any
 * other, and its history is worth keeping for the same reason.
 */
@Serializable
enum class PropagationStage(val label: String, val hint: String) {
    CUTTING("Cutting", "Just taken. Keep it humid and out of direct sun."),
    CALLUSING("Callusing", "Cut face drying over. Succulents and cacti want this; most soft cuttings skip it."),
    ROOTING("Rooting", "In water, sphagnum or soil, waiting for roots. Change water every few days."),
    POTTED("Potted", "Roots long enough to plant - about 3 to 5 cm. The riskiest move; keep it damp for a fortnight."),
    ESTABLISHED("Established", "Putting out new growth of its own. It is a plant now, not a cutting.");

    val next: PropagationStage? get() = entries.getOrNull(ordinal + 1)
    val previous: PropagationStage? get() = entries.getOrNull(ordinal - 1)
}

/** One cutting on the board, with how long it has sat where it is. */
data class PropagationCard(
    val plant: Plant,
    val stage: PropagationStage,
    val stageSinceMillis: Long?,
    val lastEventMillis: Long?,
) {
    fun daysInStage(nowMillis: Long): Int? =
        stageSinceMillis?.let { ((nowMillis - it) / 86_400_000L).toInt() }
}

/**
 * How long is too long, per stage.
 *
 * Not a failure state - a cutting that has sat in water for six weeks may be
 * perfectly fine, and nagging about it would be the guilt-stack mistake in a
 * different costume. It is a nudge to go and look.
 */
fun stageIsStale(stage: PropagationStage, days: Int): Boolean = when (stage) {
    PropagationStage.CUTTING -> days > 7
    PropagationStage.CALLUSING -> days > 10
    PropagationStage.ROOTING -> days > 42
    PropagationStage.POTTED -> days > 30
    PropagationStage.ESTABLISHED -> false
}

/**
 * Average days from first cutting to potted, across finished propagations.
 * Requirements item 15 wants this figure; it only means anything once a few
 * have made it through.
 */
fun averageDaysToRoot(durations: List<Int>): Double? =
    durations.takeIf { it.isNotEmpty() }?.average()
