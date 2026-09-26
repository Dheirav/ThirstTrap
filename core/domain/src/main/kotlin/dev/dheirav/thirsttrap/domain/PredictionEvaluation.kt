package dev.dheirav.thirsttrap.domain

import kotlin.math.abs

/**
 * Scores the model against what actually happened.
 *
 * "The app says water in 3 days" is a claim, and the diary already holds
 * everything needed to check it: replay history to some past reading, take the
 * prediction the app would have made standing there, then look at when the pot
 * really crossed the trigger weight. No new instrumentation - the evidence was
 * collected by ordinary use.
 *
 * A wrong confident date is the failure mode the whole suppression table
 * exists to prevent, so the app should measure itself by the same standard it
 * is designed around, and show the result rather than assert it.
 */
data class PredictionSample(
    /** Days-to-trigger the model claimed, standing at some past reading. */
    val predictedDays: Double,
    /** Days until the pot actually crossed the trigger weight. */
    val actualDays: Double,
    val confidence: Confidence,
) {
    /** Negative = predicted early, the safe direction. */
    val errorDays: Double get() = predictedDays - actualDays
}

data class PredictionScore(
    val samples: Int = 0,
    /** Median of |error|, in days. The headline number. */
    val medianAbsErrorDays: Double? = null,
    /** Median signed error. Negative = the model runs early, which is safe. */
    val biasDays: Double? = null,
)

/**
 * One plant's samples: at every reading where the model would have produced an
 * ETA, compare it against the measured crossing.
 *
 * Ground truth is the moment the weight series crosses the trigger, linearly
 * interpolated between the readings either side. Segments the user watered
 * before the pot ever reached the trigger have no ground truth and score
 * nothing - a censored cycle is missing data, not evidence of error.
 */
fun evaluatePredictions(
    plant: Plant,
    readings: List<WeightReading>,
    wateringEventsMillis: List<Long>,
    repotEventsMillis: List<Long>,
): List<PredictionSample> {
    val ordered = readings.filter { !it.excluded }.sortedBy { it.timestampMillis }
    if (ordered.size < 3) return emptyList()

    // The full-history state supplies the segmentation and the final anchors
    // that define ground truth. Predictions themselves are replayed with only
    // the data the model had at the time - scoring it with hindsight anchors
    // it never saw would flatter nobody but the grader.
    val full = assembleWeightState(
        plant = plant,
        readings = readings,
        wateringEventsMillis = wateringEventsMillis,
        repotEventsMillis = repotEventsMillis,
        nowMillis = ordered.last().timestampMillis,
    )
    val anchors = full.plant.anchors ?: return emptyList()
    val trigger = anchors.triggerWeight(plant.depletionTrigger)

    val samples = mutableListOf<PredictionSample>()
    for (segment in full.segments) {
        val segReadings = segment.readings.filter { !it.excluded }
        if (segReadings.size < 2) continue

        val crossingMillis = crossingTime(segReadings, trigger) ?: continue

        for (r in segReadings) {
            if (r.timestampMillis >= crossingMillis) break
            val asOf = assembleWeightState(
                plant = plant,
                readings = ordered.filter { it.timestampMillis <= r.timestampMillis },
                wateringEventsMillis = wateringEventsMillis.filter { it <= r.timestampMillis },
                repotEventsMillis = repotEventsMillis.filter { it <= r.timestampMillis },
                nowMillis = r.timestampMillis,
            )
            val p = asOf.prediction
            if (p is Prediction.Eta && !p.capped) {
                samples += PredictionSample(
                    predictedDays = p.days,
                    actualDays = (crossingMillis - r.timestampMillis) / MILLIS_PER_DAY,
                    confidence = p.confidence,
                )
            }
        }
    }
    return samples
}

/** Where the series first crosses [trigger], interpolated. Null if it never does. */
private fun crossingTime(segReadings: List<WeightReading>, trigger: Double): Long? {
    for ((a, b) in segReadings.zipWithNext()) {
        if (a.grams > trigger && b.grams <= trigger) {
            val f = (a.grams - trigger) / (a.grams - b.grams)
            return a.timestampMillis + (f * (b.timestampMillis - a.timestampMillis)).toLong()
        }
    }
    return null
}

fun scorePredictions(samples: List<PredictionSample>): PredictionScore {
    if (samples.isEmpty()) return PredictionScore()
    val absErrors = samples.map { abs(it.errorDays) }.sorted()
    val signed = samples.map { it.errorDays }.sorted()
    return PredictionScore(
        samples = samples.size,
        medianAbsErrorDays = median(absErrors),
        biasDays = median(signed),
    )
}

private fun median(sorted: List<Double>): Double {
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
}
