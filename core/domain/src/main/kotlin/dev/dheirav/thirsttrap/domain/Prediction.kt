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

    /**
     * Watered since the last weigh-in, so the last weight describes a pot that
     * no longer exists.
     *
     * Segmentation cannot see this on its own: a watering only opens a new
     * segment when it falls between two readings, so one logged after the most
     * recent reading left the model predicting from the pre-watering weight and
     * the card saying "needs water now" about a pot watered an hour ago.
     */
    WATERED_SINCE_LAST_READING,
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
    /**
     * When the plant was last watered, if it is known. Only used to notice a
     * watering that happened after the most recent reading; the drying curve
     * itself comes from the segments.
     */
    lastWateredMillis: Long? = null,
): Prediction {
    if (!plant.isWeightTrackable) {
        return Prediction.NeedAnotherReading(SuppressionReason.WEIGHT_MEANINGLESS_FOR_MEDIUM)
    }
    if (plant.needsRecalibration) {
        return Prediction.NeedAnotherReading(SuppressionReason.NEEDS_RECALIBRATION)
    }
    // A pair whose dry end sits at or above its wet end is not a calibration:
    // there is no range to deplete, so the trigger weight and the depletion
    // would both be fiction. Say "not calibrated" rather than predict from it.
    val anchors = plant.anchors?.takeIf { it.isUsable }
        ?: return Prediction.NeedAnotherReading(SuppressionReason.NOT_CALIBRATED)

    val current = segments.lastOrNull()
    val latest = current?.readings?.lastOrNull { !it.excluded }
        ?: return Prediction.NeedAnotherReading(SuppressionReason.NO_READINGS)

    // A watering after the last weigh-in makes that weight meaningless: the pot
    // is heavier than the number the model is holding, and every answer derived
    // from it is about the pot as it was before the can. Say so rather than
    // predict, because the alternative is telling somebody to water a pot they
    // have just watered.
    if (lastWateredMillis != null && lastWateredMillis > latest.timestampMillis) {
        return Prediction.NeedAnotherReading(SuppressionReason.WATERED_SINCE_LAST_READING)
    }

    val triggerWeight = anchors.triggerWeight(plant.depletionTrigger)

    // Use the last measured weight, not an extrapolation, to decide "now".
    if (latest.grams <= triggerWeight) return Prediction.WaterNow

    val fit = fitSegmentSlope(current, anchors, plant.weighingStepGrams)
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
            if (prior > -anchors.minMeaningfulSlope(plant.weighingStepGrams)) {
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
