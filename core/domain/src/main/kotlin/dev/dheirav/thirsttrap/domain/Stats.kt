package dev.dheirav.thirsttrap.domain

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Simple stats: what happened, counted.
 *
 * Requirement 15 asks for waterings per month, a survival rate and an average
 * days-to-root. The first and third are plain measurements. The second needs
 * care, because "survival rate" is a failure count wearing a percentage, and
 * the requirements list gamification and anything that makes a missed day feel
 * like failure as an explicit anti-goal.
 *
 * It is here, because it was asked for and because it is genuinely the thing
 * somebody wants to know after a year. But it is computed only over plants that
 * have actually left - a plant still alive is not a pending failure - and the
 * screen leads with the outcomes rather than the ratio. A number you cannot
 * improve by opening the app more often is not a streak.
 */
data class MonthCount(
    /** Sortable key, e.g. "2026-09". */
    val key: String,
    /** For display, e.g. "Sep 2026". */
    val label: String,
    val count: Int,
)

data class Outcomes(
    val active: Int = 0,
    val dormant: Int = 0,
    val died: Int = 0,
    val givenAway: Int = 0,
    val unknown: Int = 0,
) {
    val stillHere: Int get() = active + dormant
    /** Plants whose story with you has ended, one way or another. */
    val departed: Int get() = died + givenAway

    /**
     * Null until something has actually left.
     *
     * A collection where nothing has died yet does not have a 100% survival
     * rate; it has no data. Showing 100% would invite watching it fall.
     */
    val survivalRate: Double?
        get() = if (departed == 0) null else givenAway.toDouble() / departed
}

data class RootingStat(
    val samples: Int = 0,
    val medianDays: Int? = null,
    /** Cuttings that rooted but were logged before stages were recorded. */
    val untracked: Int = 0,
)

data class Stats(
    val waterings: List<MonthCount> = emptyList(),
    val outcomes: Outcomes = Outcomes(),
    val rooting: RootingStat = RootingStat(),
    val totalEvents: Int = 0,
    val plantCount: Int = 0,
)

private fun monthKey(millis: Long, tzOffsetMinutes: Int): Pair<String, String> {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.timeInMillis = millis + tzOffsetMinutes * 60_000L
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH)
    val name = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )[m]
    return "%04d-%02d".format(y, m + 1) to "$name $y"
}

/**
 * Waterings per calendar month, in the timezone each was logged in.
 *
 * Months with none are included, so a gap reads as a gap rather than being
 * closed up - which is the whole point of looking at this over a year.
 *
 * The table starts at the first watering, though, not a fixed twelve months
 * back. Months before the diary began are not gaps in it; a new user was
 * otherwise shown eleven rows of dashes for a year they never recorded, which
 * buries the one row that has anything in it.
 */
fun wateringsByMonth(events: List<CareEvent>, monthsBack: Int = 12, nowMillis: Long): List<MonthCount> {
    val watered = events.filter { it.type == CareEventType.WATERED }
    val counts = watered.groupingBy { monthKey(it.timestampMillis, it.tzOffsetMinutes).first }
        .eachCount()

    val firstKey = watered.minByOrNull { it.timestampMillis }
        ?.let { monthKey(it.timestampMillis, it.tzOffsetMinutes).first }

    val cal = Calendar.getInstance()
    cal.timeInMillis = nowMillis
    val out = mutableListOf<MonthCount>()
    for (back in (monthsBack - 1) downTo 0) {
        val c = cal.clone() as Calendar
        c.add(Calendar.MONTH, -back)
        val (key, label) = monthKey(c.timeInMillis, 0)
        if (firstKey != null && key < firstKey) continue
        out += MonthCount(key, label, counts[key] ?: 0)
    }
    return out
}

fun outcomesOf(plants: List<Plant>): Outcomes {
    var a = 0; var d = 0; var died = 0; var gave = 0; var unk = 0
    plants.forEach {
        when (it.status) {
            PlantStatus.ACTIVE -> a++
            PlantStatus.DORMANT -> d++
            PlantStatus.DEAD -> died++
            PlantStatus.GIVEN_AWAY -> gave++
            PlantStatus.UNKNOWN -> unk++
        }
    }
    return Outcomes(a, d, died, gave, unk)
}

/**
 * How long cuttings took to root.
 *
 * "Rooted" is the move to [PropagationStage.POTTED] - roots long enough to
 * plant - measured from when the cutting was acquired. Reads the structured
 * stage on the milestone event rather than parsing its note.
 *
 * The **median**, not the mean: one cutting that sat in a jar for five months
 * would drag an average somewhere useless, and with a handful of samples that
 * is likely rather than rare.
 */
fun rootingStat(plants: List<Plant>, events: List<CareEvent>): RootingStat {
    val cuttings = plants.filter { it.isPropagating || it.source == PlantSource.CUTTING }
    val byPlant = events.groupBy { it.plantId }

    val days = mutableListOf<Int>()
    var untracked = 0
    cuttings.forEach { plant ->
        // acquiredEpochDay is a date, not an instant - the day the cutting was
        // taken, which is the only start point a user actually knows.
        val start = plant.acquiredEpochDay?.let { it * MILLIS_PER_DAY.toLong() }
        val potted = byPlant[plant.id]
            ?.filter { it.propagationStage == PropagationStage.POTTED }
            ?.minByOrNull { it.timestampMillis }
        when {
            potted != null && start != null && potted.timestampMillis >= start ->
                days += ((potted.timestampMillis - start) / MILLIS_PER_DAY).toInt()
            // Reached potted or beyond, but before stages were recorded.
            (plant.effectiveStage?.ordinal ?: -1) >= PropagationStage.POTTED.ordinal -> untracked++
        }
    }

    val median = days.sorted().let {
        when {
            it.isEmpty() -> null
            it.size % 2 == 1 -> it[it.size / 2]
            else -> (it[it.size / 2 - 1] + it[it.size / 2]) / 2
        }
    }
    return RootingStat(samples = days.size, medianDays = median, untracked = untracked)
}

fun computeStats(plants: List<Plant>, events: List<CareEvent>, nowMillis: Long): Stats = Stats(
    waterings = wateringsByMonth(events, nowMillis = nowMillis),
    outcomes = outcomesOf(plants),
    rooting = rootingStat(plants, events),
    totalEvents = events.size,
    plantCount = plants.size,
)
