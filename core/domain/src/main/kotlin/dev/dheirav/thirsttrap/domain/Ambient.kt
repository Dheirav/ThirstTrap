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
    suspend fun knownLocations(): List<String>
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
fun explainForPlant(
    state: WeightState,
    ambient: List<AmbientReading>,
    nowMillis: Long,
): AmbientExplanation? {
    val baselineRate = state.plant.slopeEwmaGramsPerDay ?: return null
    val currentRate = state.slopeGramsPerDay ?: return null
    // One closed segment is not a baseline - the EWMA is still just that
    // segment, so comparing the current cycle to it compares it to itself.
    if (state.closedSegmentCount < MIN_CLOSED_SEGMENTS_FOR_DIAGNOSTICS) return null

    val segmentStart = state.currentSegment?.first?.timestampMillis ?: return null
    val relevant = ambient.forLocation(state.plant.location, nowMillis)

    return explainDryingChange(
        baselineGramsPerDay = baselineRate,
        currentGramsPerDay = currentRate,
        baseline = relevant.filter { it.timestampMillis < segmentStart },
        current = relevant.filter { it.timestampMillis >= segmentStart },
    )
}
