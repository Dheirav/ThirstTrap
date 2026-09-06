package dev.dheirav.thirsttrap.domain

import kotlin.math.abs

/**
 * The drying curve is a health monitor, not just a timer.
 *
 * Both alerts need an established baseline — without one there is nothing to
 * deviate from. Phrase them as questions in the UI: the app has a slope, not a
 * stethoscope. docs/WATERING-MODEL.md §7.
 */
enum class DryingDiagnostic {
    /** Water may be channelling down the sides of a shrunken root ball, or it is root-bound. */
    DRYING_FASTER_THAN_USUAL,

    /** Roots may have stopped drinking: rot, over-potting, or a cold dark spell. */
    POT_STAYING_HEAVY,
}

/**
 * @param closedSegmentCount how many segments have completed. Diagnostics stay
 *        silent until there are enough to trust the baseline.
 * @return at most one diagnostic — never nag with two at once.
 */
fun diagnoseDrying(
    plant: Plant,
    currentSegment: DryingSegment?,
    closedSegmentCount: Int,
    nowMillis: Long,
): DryingDiagnostic? {
    if (closedSegmentCount < MIN_CLOSED_SEGMENTS_FOR_DIAGNOSTICS) return null
    val baseline = plant.slopeEwmaGramsPerDay ?: return null
    val anchors = plant.anchors ?: return null
    val segment = currentSegment ?: return null
    if (baseline >= 0.0) return null

    val readings = segment.readings.filterNot { it.excluded }
    val firstReading = readings.firstOrNull() ?: return null
    val daysSinceWatering = (nowMillis - firstReading.timestampMillis) / MILLIS_PER_DAY

    val fit = fitSegmentSlope(segment, anchors)

    if (fit is SlopeFit.Fitted && readings.size >= 3 &&
        abs(fit.gramsPerDay) > FAST_DRYING_MULTIPLIER * abs(baseline)
    ) {
        return DryingDiagnostic.DRYING_FASTER_THAN_USUAL
    }

    val slowSlope = when (fit) {
        is SlopeFit.Fitted -> abs(fit.gramsPerDay)
        is SlopeFit.NoMeasurableDrying -> abs(fit.gramsPerDay)
        SlopeFit.Insufficient -> return null
    }
    if (daysSinceWatering >= SLOW_DRYING_MIN_DAYS &&
        slowSlope < SLOW_DRYING_MULTIPLIER * abs(baseline)
    ) {
        return DryingDiagnostic.POT_STAYING_HEAVY
    }

    return null
}
