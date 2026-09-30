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
    val lastWatered = wateringEventsMillis.maxOrNull()

    if (readings.isEmpty()) {
        return WeightState(
            plant = plant,
            readings = readings,
            prediction = predictWatering(plant, emptyList(), nowMillis, lastWatered),
            lastWateredMillis = lastWatered,
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

    // A stored pair that spans no range cannot be interpreted, so it counts as
    // no calibration at all and gets re-derived from the readings below. The
    // anchors are guarded where they are folded, but a row can also arrive from
    // an import or an older schema, and predicting from an impossible pair is
    // worse than admitting there is nothing to predict from.
    val stored = plant.anchors?.takeIf { it.isUsable }
    val startingAnchors: Anchors? = when {
        stored != null && !plant.needsRecalibration -> stored
        derivedWet != null -> Anchors.fromWetAnchor(derivedWet.grams)
        else -> null
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
            prediction = predictWatering(plant, segments, nowMillis, lastWatered),
            lastWateredMillis = lastWatered,
        )
    }

    // Anchors move with the readings: the wet end is re-captured on every
    // post-water weigh, and the dry end walks down toward where this person
    // actually waters. Replaying them in order is what keeps both honest.
    var anchors: Anchors = startingAnchors
    for (reading in ordered) {
        if (reading.excluded) continue
        // The same cutoff the derived wet anchor uses, and for the same reason.
        // A repot replaces the pot, so a pre-water reading from the old one is
        // evidence about a dry weight that no longer exists; folding it in
        // anyway let the old pot's lighter dry end win forever, which pushed
        // the trigger weight down and delayed the prompt. The wet end was
        // already protected by the filter above, which is why this only showed
        // up at the dry end.
        if (reading.timestampMillis < sinceRepot) continue
        anchors = anchors.withReading(reading)
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
        val fit = fitSegmentSlope(segment, anchors, plant.weighingStepGrams)
        if (fit is SlopeFit.Fitted) {
            ewma = updateEwma(ewma, fit.gramsPerDay)
            closed++
        }
    }

    val withPrior = adapted.copy(slopeEwmaGramsPerDay = ewma)
    val prediction = predictWatering(withPrior, segments, nowMillis, lastWatered)
    val current = segments.lastOrNull()
    val latest = current?.readings?.lastOrNull { !it.excluded }

    val slope = (current?.let { fitSegmentSlope(it, anchors, plant.weighingStepGrams) }
        as? SlopeFit.Fitted)?.gramsPerDay

    return WeightState(
        plant = withPrior,
        readings = readings,
        segments = segments,
        prediction = prediction,
        diagnostic = diagnoseDrying(withPrior, current, closed, nowMillis),
        depletion = latest?.let { anchors.depletionAt(it.grams) },
        slopeGramsPerDay = slope,
        closedSegmentCount = closed,
        lastWateredMillis = lastWatered,
    )
}

/**
 * Which context the keypad should start on.
 *
 * Watering and weighing are one moment that the app modelled as two. You water
 * a plant from the list, carry it to the scale, and the keypad opens on
 * ROUTINE, so the reading that should have become the new wet anchor is filed
 * as an ordinary sample and the anchor quietly stays stale. Nothing tells you,
 * because from the app's point of view nothing went wrong.
 *
 * A reading counts as the anchor when it comes after the last watering and
 * close enough to it. A day is the ceiling: drainage finishes in an hour and
 * people weigh when they get round to it, while a pot weighed three days after
 * watering has visibly dried and calling that "full" would poison the scale it
 * measures everything else against.
 *
 * The user can always override the chip. This only decides where it starts.
 */
fun suggestReadingContext(
    prediction: Prediction,
    lastWateredMillis: Long?,
    lastReadingMillis: Long?,
    nowMillis: Long,
): ReadingContext {
    if (lastWateredMillis != null &&
        nowMillis - lastWateredMillis in 0..POST_WATER_WINDOW_MILLIS &&
        (lastReadingMillis == null || lastReadingMillis < lastWateredMillis)
    ) {
        return ReadingContext.POST_WATER
    }
    return if (prediction is Prediction.WaterNow) {
        ReadingContext.PRE_WATER
    } else {
        ReadingContext.ROUTINE
    }
}

/** The same rule, for a screen that already has the assembled state. */
fun suggestReadingContext(state: WeightState, nowMillis: Long): ReadingContext =
    suggestReadingContext(
        prediction = state.prediction,
        lastWateredMillis = state.lastWateredMillis,
        lastReadingMillis = state.readings.filter { !it.excluded }
            .maxOfOrNull { it.timestampMillis },
        nowMillis = nowMillis,
    )

/** How long after a watering a reading still counts as the wet anchor. */
const val POST_WATER_WINDOW_MILLIS = 24 * 60 * 60 * 1000L

/**
 * A POST_WATER reading with no watering on the log is a watering the log
 * missed. Eighteen days of real diary proved it: the user weighs every day
 * because weighing is two taps, and stopped logging waterings because that is
 * a separate chore - four waterings in a row arrived as bare POST_WATER
 * readings. The drying model self-heals through jump segmentation, but the
 * dashboard's "last watered", the average interval and the reminder clock all
 * count from the event that was never written.
 *
 * So the weigh-in carries the watering with it. Same window as the anchor
 * rule: a watering logged within the last day explains the reading; anything
 * older (or nothing at all) means the log is missing one.
 */
fun impliesUnloggedWatering(
    context: ReadingContext,
    lastWateredMillis: Long?,
    nowMillis: Long,
): Boolean =
    context == ReadingContext.POST_WATER &&
        (lastWateredMillis == null || nowMillis - lastWateredMillis > POST_WATER_WINDOW_MILLIS)
