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
    if (readings.isEmpty()) {
        return WeightState(
            plant = plant,
            readings = readings,
            prediction = predictWatering(plant, emptyList(), nowMillis),
        )
    }

    val ordered = readings.sortedBy { it.timestampMillis }

    // A wet anchor is a weight taken just after watering. That is exactly what a
    // POST_WATER reading is, so one can serve as the anchor without a separate
    // calibration ceremony - and requiring the ceremony first meant a pot could
    // not be weighed at all until the user happened to be standing there having
    // just watered it, which is the one moment they are least likely to be
    // holding a phone.
    //
    // Derived on read rather than stored, for the same reason the EWMA is: a
    // stored anchor drifts from the readings it came from, and excluding a bad
    // post-water weigh should re-derive it rather than leave the plant
    // calibrated against a mistake.
    //
    // Only readings after the last repot count. A repot changes the pot's dry
    // weight, which is what markNeedsRecalibration exists to say; letting an
    // older reading re-anchor would silently undo that.
    val sinceRepot = repotEventsMillis.maxOrNull() ?: Long.MIN_VALUE
    val derivedWet = ordered.firstOrNull {
        !it.excluded &&
            it.timestampMillis >= sinceRepot &&
            (it.context == ReadingContext.POST_WATER || it.context == ReadingContext.CALIBRATION)
    }

    val startingAnchors: Anchors? = when {
        plant.anchors != null && !plant.needsRecalibration -> plant.anchors
        derivedWet != null -> Anchors.fromWetAnchor(derivedWet.grams)
        else -> plant.anchors.takeIf { !plant.needsRecalibration }
    }

    if (startingAnchors == null) {
        // No anchor yet, which suppresses the prediction and the depletion -
        // but the readings are real and their curve is worth drawing. Weighing
        // a pot before the app can interpret the number is how somebody starts.
        val segments = segmentReadings(readings, null, wateringEventsMillis, repotEventsMillis)
        return WeightState(
            plant = plant,
            readings = readings,
            segments = segments,
            prediction = predictWatering(plant, segments, nowMillis),
        )
    }

    // Anchors move with the readings: the wet end is re-captured on every
    // post-water weigh, and the dry end walks down toward where this person
    // actually waters. Replaying them in order is what keeps both honest.
    var anchors: Anchors = startingAnchors
    for (reading in ordered) {
        if (!reading.excluded) anchors = anchors.withReading(reading)
    }

    // needsRecalibration is cleared here rather than written back: a post-water
    // weigh after a repot IS the recalibration, so continuing to ask for one
    // would be asking for something already done.
    val adapted = plant.copy(anchors = anchors, needsRecalibration = false)
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
