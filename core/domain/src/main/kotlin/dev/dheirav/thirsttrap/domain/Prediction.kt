package dev.dheirav.thirsttrap.domain

import kotlin.math.abs

sealed interface Prediction {
    /** Already at or past the trigger weight. */
    data object WaterNow : Prediction

    data class Eta(
        val days: Double,
        val confidence: Confidence,
        /** True when the real ETA is beyond the cap and should read "more than 2 weeks". */
        val capped: Boolean,
    ) : Prediction

    /** Deliberately no number. [reason] drives the wording. */
    data class NeedAnotherReading(val reason: SuppressionReason) : Prediction
}

/**
 * Drives how confidently the UI words the prediction. A measured fit over four
 * readings and a guess from history are not the same claim.
 */
enum class Confidence { HIGH, MEDIUM, LOW }

/**
 * Why no prediction is being shown. Each of these is a deliberate refusal: an
 * app that states a wrong date confidently is worse than one that admits it
 * does not know yet, because the first trains exactly the calendar-watering
 * habit the weight method exists to replace. docs/WATERING-MODEL.md §6.
 */
enum class SuppressionReason {
    NOT_CALIBRATED,
    NEEDS_RECALIBRATION,
    NO_READINGS,
    ONE_READING_NO_HISTORY,
    NO_MEASURABLE_DRYING,
    WEIGHT_MEANINGLESS_FOR_MEDIUM,
}

/**
 * Where a plant is now, and when it will next want water.
 *
 * @param nowMillis injected rather than read from the clock, so this stays pure
 *        and testable.
 */
fun predictWatering(
    plant: Plant,
    segments: List<DryingSegment>,
    nowMillis: Long,
): Prediction {
    if (!plant.isWeightTrackable) {
        return Prediction.NeedAnotherReading(SuppressionReason.WEIGHT_MEANINGLESS_FOR_MEDIUM)
    }
    if (plant.needsRecalibration) {
        return Prediction.NeedAnotherReading(SuppressionReason.NEEDS_RECALIBRATION)
    }
    val anchors = plant.anchors
        ?: return Prediction.NeedAnotherReading(SuppressionReason.NOT_CALIBRATED)

    val current = segments.lastOrNull()
    val latest = current?.readings?.lastOrNull { !it.excluded }
        ?: return Prediction.NeedAnotherReading(SuppressionReason.NO_READINGS)

    val triggerWeight = anchors.triggerWeight(plant.depletionTrigger)

    // Use the last measured weight, not an extrapolation, to decide "now".
    if (latest.grams <= triggerWeight) return Prediction.WaterNow

    val fit = fitSegmentSlope(current, anchors)
    val (slope, confidence) = when (fit) {
        is SlopeFit.Fitted -> {
            val c = when {
                fit.readingCount >= 4 && fit.method == SlopeMethod.THEIL_SEN -> Confidence.HIGH
                else -> Confidence.MEDIUM
            }
            fit.gramsPerDay to c
        }

        is SlopeFit.NoMeasurableDrying ->
            return Prediction.NeedAnotherReading(SuppressionReason.NO_MEASURABLE_DRYING)

        SlopeFit.Insufficient -> {
            val prior = plant.slopeEwmaGramsPerDay
                ?: return Prediction.NeedAnotherReading(SuppressionReason.ONE_READING_NO_HISTORY)
            if (prior > -anchors.minMeaningfulSlope) {
                return Prediction.NeedAnotherReading(SuppressionReason.NO_MEASURABLE_DRYING)
            }
            prior to Confidence.LOW
        }
    }

    val daysFromLatest = (latest.grams - triggerWeight) / abs(slope)
    val elapsedSinceLatest = (nowMillis - latest.timestampMillis) / MILLIS_PER_DAY
    val remaining = daysFromLatest - elapsedSinceLatest

    if (remaining <= 0.0) return Prediction.WaterNow
    return if (remaining > ETA_CAP_DAYS) {
        Prediction.Eta(ETA_CAP_DAYS, confidence, capped = true)
    } else {
        Prediction.Eta(remaining, confidence, capped = false)
    }
}
