package dev.dheirav.thirsttrap.domain

import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * What the room was doing, per location.
 *
 * Requirement 23 asks for this "so seasonal drying-rate changes are
 * explainable", and explainable is the whole scope. Nothing here feeds the
 * prediction, the depletion trigger or the slope fit. A pot that dries faster
 * dries faster whether or not the app knows why; inventing a temperature
 * correction on two thermometer readings would be a model built on nothing.
 *
 * What it does instead is tell the difference between "this plant changed" and
 * "the room changed", which is the difference between a diagnostic worth
 * reading and one worth ignoring.
 */
@Serializable
data class AmbientReading(
    val id: String,
    /** Free text, matched against [Plant.location]. */
    val location: String,
    val timestampMillis: Long,
    val tzOffsetMinutes: Int,
    val temperatureC: Double? = null,
    val humidityPercent: Double? = null,
    val source: AmbientSource = AmbientSource.MANUAL,
    val note: String? = null,
)

@Serializable
enum class AmbientSource(val label: String) {
    /** A thermometer, a hygrometer, or a guess the user was willing to write down. */
    MANUAL("Entered by hand"),

    /**
     * Outdoor weather for a named place. Indoors is not outdoors, and this is
     * labelled as such everywhere it is shown - it tracks the *season*, which
     * is what requirement 23 actually asks about, not the room.
     */
    WEATHER("Outdoor weather"),
}

/** A location's conditions over some window. */
data class AmbientSummary(
    val meanTemperatureC: Double?,
    val meanHumidityPercent: Double?,
    val readingCount: Int,
)

fun List<AmbientReading>.summarise(): AmbientSummary {
    val temps = mapNotNull { it.temperatureC }
    val humidities = mapNotNull { it.humidityPercent }
    return AmbientSummary(
        meanTemperatureC = temps.takeIf { it.isNotEmpty() }?.average(),
        meanHumidityPercent = humidities.takeIf { it.isNotEmpty() }?.average(),
        readingCount = size,
    )
}

/**
 * Whether the room accounts for a change in how fast a pot is drying.
 *
 * [ROOM_UNCHANGED] is the useful one. It is the only verdict that makes a
 * diagnostic *more* worth acting on, because it removes the boring explanation
 * and leaves the interesting ones - a shrunken root ball, a rootbound pot, rot.
 */
enum class AmbientVerdict {
    /** Warmer or drier air, and the pot is drying faster. Expected. */
    EXPLAINS_FASTER,

    /** Cooler or more humid air, and the pot is drying slower. Expected. */
    EXPLAINS_SLOWER,

    /** The room barely moved, so whatever changed was not the weather. */
    ROOM_UNCHANGED,

    /**
     * The room moved the opposite way to the pot. Deliberately not called
     * "wrong" - a cold snap while a pot dries faster is exactly what a cracked
     * root ball channelling water down the sides looks like.
     */
    CONTRADICTS,
}

data class AmbientExplanation(
    val verdict: AmbientVerdict,
    /** Positive when the room got warmer. Null when nobody recorded temperature. */
    val temperatureDeltaC: Double?,
    /** Positive when the room got more humid. */
    val humidityDeltaPercent: Double?,
    /** Ratio of current drying rate to baseline. 1.5 means half again as fast. */
    val dryingRatio: Double,
    val source: AmbientSource,
)

/**
 * Compares two periods and says whether the room explains the difference.
 *
 * Returns null - says nothing at all - when either period is too thin to
 * average, when the pot's drying rate has not actually changed, or when no
 * temperature or humidity was recorded in both periods. The same discipline as
 * [Prediction]: an app that confidently names a wrong cause is worse than one
 * that stays quiet, because a wrong cause sends someone to repot a healthy
 * plant.
 *
 * @param baselineGramsPerDay the established rate, negative for drying.
 * @param currentGramsPerDay the current segment's rate, negative for drying.
 */
fun explainDryingChange(
    baselineGramsPerDay: Double,
    currentGramsPerDay: Double,
    baseline: List<AmbientReading>,
    current: List<AmbientReading>,
): AmbientExplanation? {
    if (baseline.size < AMBIENT_MIN_READINGS_PER_PERIOD) return null
    if (current.size < AMBIENT_MIN_READINGS_PER_PERIOD) return null

    val base = abs(baselineGramsPerDay)
    val now = abs(currentGramsPerDay)
    if (base <= 0.0) return null

    val ratio = now / base
    // No change in the pot means there is nothing to explain, whatever the room
    // did. Saying "it is warmer" about an unchanged drying rate is noise.
    if (ratio in (1.0 / AMBIENT_DRYING_SHIFT_RATIO)..AMBIENT_DRYING_SHIFT_RATIO) return null

    val b = baseline.summarise()
    val c = current.summarise()

    val tempDelta = if (b.meanTemperatureC != null && c.meanTemperatureC != null) {
        c.meanTemperatureC - b.meanTemperatureC
    } else {
        null
    }
    val humidityDelta = if (b.meanHumidityPercent != null && c.meanHumidityPercent != null) {
        c.meanHumidityPercent - b.meanHumidityPercent
    } else {
        null
    }
    if (tempDelta == null && humidityDelta == null) return null

    // Warmer air and drier air both speed evaporation, so they point the same
    // way; either alone is enough to call the room changed.
    val warmer = tempDelta != null && tempDelta >= AMBIENT_TEMP_SHIFT_C
    val cooler = tempDelta != null && tempDelta <= -AMBIENT_TEMP_SHIFT_C
    val drier = humidityDelta != null && humidityDelta <= -AMBIENT_HUMIDITY_SHIFT_PCT
    val moister = humidityDelta != null && humidityDelta >= AMBIENT_HUMIDITY_SHIFT_PCT

    val roomPushesFaster = warmer || drier
    val roomPushesSlower = cooler || moister
    val potIsFaster = ratio > AMBIENT_DRYING_SHIFT_RATIO

    val verdict = when {
        // A room that moved both ways at once - warmer but also much more humid
        // - is not a direction, so it cannot explain anything.
        roomPushesFaster && roomPushesSlower -> AmbientVerdict.ROOM_UNCHANGED
        roomPushesFaster && potIsFaster -> AmbientVerdict.EXPLAINS_FASTER
        roomPushesSlower && !potIsFaster -> AmbientVerdict.EXPLAINS_SLOWER
        roomPushesFaster || roomPushesSlower -> AmbientVerdict.CONTRADICTS
        else -> AmbientVerdict.ROOM_UNCHANGED
    }

    return AmbientExplanation(
        verdict = verdict,
        temperatureDeltaC = tempDelta,
        humidityDeltaPercent = humidityDelta,
        dryingRatio = ratio,
        // Outdoor weather has to stay labelled as outdoor weather all the way
        // to the sentence the user reads.
        source = if (current.any { it.source == AmbientSource.MANUAL }) {
            AmbientSource.MANUAL
        } else {
            AmbientSource.WEATHER
        },
    )
}

/** Readings for [location], newest first, discarding anything gone stale. */
fun List<AmbientReading>.forLocation(location: String?, nowMillis: Long): List<AmbientReading> {
    if (location.isNullOrBlank()) return emptyList()
    val cutoff = nowMillis - (AMBIENT_STALE_DAYS * MILLIS_PER_DAY).toLong()
    return filter { it.location.equals(location, ignoreCase = true) && it.timestampMillis >= cutoff }
        .sortedByDescending { it.timestampMillis }
}

interface AmbientRepository {
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<AmbientReading>>
    suspend fun all(): List<AmbientReading>
    suspend fun record(reading: AmbientReading)
    suspend fun delete(id: String)
}

/**
 * The ambient explanation for one plant's current drying cycle, if any.
 *
 * "Current" is the segment the pot is in now; "baseline" is everything before
 * it that has not gone stale. The split is by the segment boundary rather than
 * by a fixed number of days because that is where the drying rate the app is
 * comparing actually changes.
 */
/**
 * Why no room comparison is being made.
 *
 * This existed as a bare null, and a bare null is the one answer this screen
 * must not give: silence is identical whether the room has been steady or
 * nobody has ever typed a reading, and those are completely different facts.
 * With no room readings at all the card simply never appeared, and there was no
 * way to learn that it was waiting for something.
 *
 * Exhaustive, so a seventh precondition cannot be added without wording it, for
 * the same reason [SuppressionReason] is.
 */
enum class AmbientGap {
    /** Nothing has ever been recorded for this plant's place. */
    NO_READINGS,

    /** Readings exist, but not from before this drying cycle started. */
    NONE_BEFORE_THIS_CYCLE,

    /** Readings exist from before, but none since this cycle started. */
    NONE_DURING_THIS_CYCLE,

    /** Fewer than two finished drying cycles, so there is no rate to compare to. */
    NO_BASELINE_YET,

    /** The pot's drying rate has not meaningfully changed, so nothing needs explaining. */
    POT_UNCHANGED,

    /** The pot changed but the room did not, which is itself worth knowing. */
    ROOM_UNCHANGED,
}

/** Either an explanation, or the reason there is not one. */
sealed interface AmbientInsight {
    data class Explained(val explanation: AmbientExplanation) : AmbientInsight
    data class Waiting(val gap: AmbientGap) : AmbientInsight
}

fun insightForPlant(
    state: WeightState,
    ambient: List<AmbientReading>,
    nowMillis: Long,
): AmbientInsight {
    val baselineRate = state.plant.slopeEwmaGramsPerDay
    val currentRate = state.slopeGramsPerDay
    // One closed segment is not a baseline - the EWMA is still just that
    // segment, so comparing the current cycle to it compares it to itself.
    if (baselineRate == null || currentRate == null ||
        state.closedSegmentCount < MIN_CLOSED_SEGMENTS_FOR_DIAGNOSTICS
    ) {
        return AmbientInsight.Waiting(AmbientGap.NO_BASELINE_YET)
    }

    val segmentStart = state.currentSegment?.first?.timestampMillis
        ?: return AmbientInsight.Waiting(AmbientGap.NO_BASELINE_YET)
    val relevant = ambient.forLocation(state.plant.location, nowMillis)
    if (relevant.isEmpty()) return AmbientInsight.Waiting(AmbientGap.NO_READINGS)

    val before = relevant.filter { it.timestampMillis < segmentStart }
    val during = relevant.filter { it.timestampMillis >= segmentStart }
    if (before.size < AMBIENT_MIN_READINGS_PER_PERIOD) {
        return AmbientInsight.Waiting(AmbientGap.NONE_BEFORE_THIS_CYCLE)
    }
    if (during.size < AMBIENT_MIN_READINGS_PER_PERIOD) {
        return AmbientInsight.Waiting(AmbientGap.NONE_DURING_THIS_CYCLE)
    }

    val explained = explainDryingChange(
        baselineGramsPerDay = baselineRate,
        currentGramsPerDay = currentRate,
        baseline = before,
        current = during,
    ) ?: run {
        // explainDryingChange declines for two different reasons and the
        // difference matters to a reader: an unchanged pot needs no
        // explanation, while a changed pot in an unchanged room is the
        // interesting case where the cause is something else.
        val ratio = abs(currentRate) / abs(baselineRate).coerceAtLeast(1e-9)
        val potMoved = ratio !in (1.0 / AMBIENT_DRYING_SHIFT_RATIO)..AMBIENT_DRYING_SHIFT_RATIO
        return AmbientInsight.Waiting(
            if (potMoved) AmbientGap.ROOM_UNCHANGED else AmbientGap.POT_UNCHANGED,
        )
    }
    return AmbientInsight.Explained(explained)
}

/** What to say for each [AmbientGap]. One line, no blame, no nagging. */
fun wordingFor(gap: AmbientGap): String = when (gap) {
    AmbientGap.NO_READINGS ->
        "No room readings for this place yet, so nothing is being compared against " +
            "the weather. Recording the temperature and humidity now and then is what " +
            "lets the app tell a warm week apart from a thirsty plant."
    AmbientGap.NONE_BEFORE_THIS_CYCLE ->
        "There are room readings, but none from before this drying cycle started, so " +
            "there is nothing to compare this one against yet."
    AmbientGap.NONE_DURING_THIS_CYCLE ->
        "No room readings since this drying cycle started. One now would let the app " +
            "say whether the room explains how the pot is behaving."
    AmbientGap.NO_BASELINE_YET ->
        "Not enough finished drying cycles yet to know what normal looks like for this " +
            "pot, so there is nothing for the room to explain."
    AmbientGap.POT_UNCHANGED ->
        "This pot is drying at about its usual rate, so there is nothing for the room " +
            "to account for."
    AmbientGap.ROOM_UNCHANGED ->
        "This pot's drying rate has changed and the room has not, so the cause is " +
            "something else: where it is standing, how much it has grown, or the soil."
}
