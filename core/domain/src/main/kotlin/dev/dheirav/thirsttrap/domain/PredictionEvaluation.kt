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
    /**
     * What watering by the calendar would have claimed at that same moment, or
     * null where a calendar had nothing to go on yet.
     *
     * This is the comparison the product rests on and it was not being made.
     * The app measured its own error and reported it, which says whether the
     * arithmetic works, not whether weighing beats the habit it exists to
     * replace. A model can be accurate and still be pointless.
     */
    val calendarDays: Double? = null,
) {
    /** Negative = predicted early, the safe direction. */
    val errorDays: Double get() = predictedDays - actualDays

    val calendarErrorDays: Double? get() = calendarDays?.minus(actualDays)
}

data class PredictionScore(
    val samples: Int = 0,
    /** Median of |error|, in days. The headline number. */
    val medianAbsErrorDays: Double? = null,
    /** Median signed error. Negative = the model runs early, which is safe. */
    val biasDays: Double? = null,
    /**
     * The same two numbers for watering by the calendar, over the SAME moments.
     * Only the samples where both made a call are counted, on both sides: a
     * comparison across two different sets of moments is not a comparison.
     */
    val comparedSamples: Int = 0,
    val calendarMedianAbsErrorDays: Double? = null,
    val calendarBiasDays: Double? = null,
    /** Model error over the compared subset, so the two sit on one footing. */
    val medianAbsErrorDaysCompared: Double? = null,
) {
    /**
     * Days of error the weighing saves against the calendar. Positive = weighing
     * is better. Null until there is something to compare.
     */
    val advantageDays: Double?
        get() {
            val c = calendarMedianAbsErrorDays ?: return null
            val m = medianAbsErrorDaysCompared ?: return null
            return c - m
        }
}

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
                    calendarDays = calendarPrediction(wateringEventsMillis, r.timestampMillis),
                )
            }
        }
    }
    return samples
}

/**
 * What a calendar would say, standing at [asOfMillis]: the last watering plus
 * the interval this plant has actually been watered at.
 *
 * Fitted the same walk-forward way the model is, from gaps observed strictly
 * before this moment and nothing after it. That matters, because the easy
 * version of this comparison is rigged. Picking a round number like seven days,
 * or fitting the interval over the whole history including the future, produces
 * a straw man that weighing beats without telling you anything. This is the
 * best calendar available from the same evidence, which is the only one worth
 * losing to.
 *
 * Null until two waterings have been seen, because one gap is not an interval,
 * and a calendar nobody could have written is not a fair opponent either.
 */
private fun calendarPrediction(wateringEventsMillis: List<Long>, asOfMillis: Long): Double? {
    val past = wateringEventsMillis.filter { it <= asOfMillis }.sorted()
    if (past.size < 3) return null
    val gaps = past.zipWithNext { a, b -> (b - a) / MILLIS_PER_DAY }.sorted()
    val interval = median(gaps)
    if (interval <= 0.0) return null
    val due = past.last() + (interval * MILLIS_PER_DAY).toLong()
    return (due - asOfMillis) / MILLIS_PER_DAY
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

    // Paired: only the moments where both the model and a calendar made a call.
    val both = samples.filter { it.calendarErrorDays != null }
    return PredictionScore(
        samples = samples.size,
        medianAbsErrorDays = median(absErrors),
        biasDays = median(signed),
        comparedSamples = both.size,
        calendarMedianAbsErrorDays =
            both.takeIf { it.isNotEmpty() }?.map { abs(it.calendarErrorDays!!) }?.sorted()?.let(::median),
        calendarBiasDays =
            both.takeIf { it.isNotEmpty() }?.map { it.calendarErrorDays!! }?.sorted()?.let(::median),
        medianAbsErrorDaysCompared =
            both.takeIf { it.isNotEmpty() }?.map { abs(it.errorDays) }?.sorted()?.let(::median),
    )
}

private fun median(sorted: List<Double>): Double {
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
}
