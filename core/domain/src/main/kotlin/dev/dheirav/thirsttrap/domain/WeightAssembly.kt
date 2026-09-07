package dev.dheirav.thirsttrap.domain

/**
 * Turns raw readings into everything the weight UI shows.
 *
 * Pure, so the whole pipeline - segmentation, anchor adaptation, the fit, the
 * prior, the prediction and both diagnostics - is testable without a device.
 *
 * The EWMA is folded from the closed segments on every call rather than stored.
 * A cached prior can drift from the readings it was derived from; recomputing
 * a handful of medians costs nothing and cannot be wrong.
 */
fun assembleWeightState(
    plant: Plant,
    readings: List<WeightReading>,
    wateringEventsMillis: List<Long>,
    repotEventsMillis: List<Long>,
    nowMillis: Long,
): WeightState {
    if (readings.isEmpty() || plant.anchors == null) {
        return WeightState(
            plant = plant,
            readings = readings,
            prediction = predictWatering(plant, emptyList(), nowMillis),
        )
    }

    // Anchors move with the readings: the wet end is re-captured on every
    // post-water weigh, and the dry end walks down toward where this person
    // actually waters. Replaying them in order is what keeps both honest.
    var anchors: Anchors = plant.anchors
    for (reading in readings.sortedBy { it.timestampMillis }) {
        if (!reading.excluded) anchors = anchors.withReading(reading)
    }

    val adapted = plant.copy(anchors = anchors)
    val segments = segmentReadings(readings, anchors, wateringEventsMillis, repotEventsMillis)

    // Every segment but the last is closed, so its slope is final and can feed
    // the prior.
    var ewma: Double? = null
    var closed = 0
    for (segment in segments.dropLast(1)) {
        val fit = fitSegmentSlope(segment, anchors)
        if (fit is SlopeFit.Fitted) {
            ewma = updateEwma(ewma, fit.gramsPerDay)
            closed++
        }
    }

    val withPrior = adapted.copy(slopeEwmaGramsPerDay = ewma)
    val prediction = predictWatering(withPrior, segments, nowMillis)
    val current = segments.lastOrNull()
    val latest = current?.readings?.lastOrNull { !it.excluded }

    val slope = (current?.let { fitSegmentSlope(it, anchors) } as? SlopeFit.Fitted)?.gramsPerDay

    return WeightState(
        plant = withPrior,
        readings = readings,
        segments = segments,
        prediction = prediction,
        diagnostic = diagnoseDrying(withPrior, current, closed, nowMillis),
        depletion = latest?.let { anchors.depletionAt(it.grams) },
        slopeGramsPerDay = slope,
        closedSegmentCount = closed,
    )
}
