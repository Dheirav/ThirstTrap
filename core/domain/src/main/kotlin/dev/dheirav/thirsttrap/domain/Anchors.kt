package dev.dheirav.thirsttrap.domain

import kotlinx.serialization.Serializable
import kotlin.math.max

/**
 * The wet and dry ends of a pot's usable water range, in grams.
 *
 * Wet is measured (water thoroughly, drain 30-60 min, weigh) and re-captured on
 * every post-watering weigh, because substrate settles and plants grow. Dry
 * starts as an estimate and is replaced adaptively — no plant is ever dried out
 * on purpose to calibrate a convenience feature. docs/WATERING-MODEL.md §2.
 */
@Serializable
data class Anchors(
    val wetGrams: Double,
    val dryGrams: Double,
    val dryIsProvisional: Boolean,
) {
    /**
     * The usable span, floored so that it can always be divided by.
     *
     * A pair that spans nothing is not a real calibration and [isUsable] is the
     * check that rejects one, but the floor sits here too because this is where
     * every division happens. A stored row can carry anything (a hand-edited
     * anchor, a truncated import), and the failure without the floor is not a
     * wrong number, it is NaN reaching the dashboard ring.
     */
    val rangeGrams: Double get() = max(wetGrams - dryGrams, MIN_ANCHOR_RANGE_GRAMS)

    /**
     * Whether these anchors describe a pot that can be interpreted at all.
     *
     * A dry end at or above the wet end leaves no water to deplete, so every
     * derived number would be fiction. Treated as no calibration rather than
     * patched into one: the readings are still drawn, and the app asks for a
     * post-water weigh instead of predicting from an impossible pair.
     */
    val isUsable: Boolean
        get() = wetGrams > 0.0 && wetGrams - dryGrams >= MIN_ANCHOR_RANGE_GRAMS

    /** 0.0 = just watered, 1.0 = at the dry anchor. Past 1.0 is real and worth showing. */
    fun depletionAt(grams: Double): Double =
        ((wetGrams - grams) / rangeGrams).coerceIn(0.0, 1.2)

    /** The weight at which this plant should be watered, for a given trigger fraction. */
    fun triggerWeight(depletionTrigger: Double): Double =
        wetGrams - depletionTrigger * rangeGrams

    /**
     * Below this, drying is not distinguishable from noise.
     *
     * Two floors, and they answer different questions. The fraction of the
     * range asks whether the change matters for this pot, and scales with it.
     * [stepGrams] asks whether the instrument could even have seen it, and does
     * not scale: a daily loss smaller than one increment of the scale is a
     * rounding artefact whatever the pot weighs. A bathroom scale in 100 g
     * steps needs a floor a hundred times a kitchen scale's, or the model fits
     * a curve to quantisation.
     */
    fun minMeaningfulSlope(stepGrams: Double = MIN_SLOPE_GRAMS_PER_DAY): Double =
        max(max(stepGrams, MIN_SLOPE_GRAMS_PER_DAY), MIN_SLOPE_RANGE_FRACTION * rangeGrams)

    companion object {
        /**
         * First calibration: a measured wet anchor and an estimated dry one.
         * The estimate is deliberately conservative — it errs toward predicting
         * dry early, which is the safe direction.
         */
        fun fromWetAnchor(wetGrams: Double): Anchors = Anchors(
            wetGrams = wetGrams,
            dryGrams = wetGrams * (1.0 - PROVISIONAL_DEPLETION_FRACTION),
            dryIsProvisional = true,
        )
    }
}

/**
 * Folds a new reading into the anchors.
 *
 * - POST_WATER / CALIBRATION re-anchor the wet end.
 * - PRE_WATER is evidence about where "dry enough for this person and this
 *   plant" actually is: take the running minimum, guarded against mis-weighs.
 *
 * Returns the anchors unchanged when the reading carries no anchor information.
 */
fun Anchors.withReading(reading: WeightReading): Anchors = when (reading.context) {
    ReadingContext.POST_WATER, ReadingContext.CALIBRATION ->
        // A wet end at or below the dry end leaves nothing to deplete. It also
        // means the pot itself got lighter than the dry weight already measured
        // for it, which a watering cannot do: something physical changed (the
        // plant was pruned back, soil came out with a division, the scale was
        // tared differently). The measured dry end describes a pot that no
        // longer exists, so it goes back to an estimate rather than being kept
        // as half of an impossible pair.
        if (reading.grams - dryGrams < MIN_ANCHOR_RANGE_GRAMS) Anchors.fromWetAnchor(reading.grams)
        else copy(wetGrams = reading.grams)

    ReadingContext.PRE_WATER -> {
        val tooLight = reading.grams < DRY_ANCHOR_FLOOR_FRACTION * wetGrams
        // The floor's mirror. A pot weighed just before watering and still at
        // container capacity says nothing about how dry this person lets it
        // get; it is a mis-weigh or a reading filed under the wrong context,
        // and believing it would collapse the range to nothing.
        val tooHeavy = reading.grams > DRY_ANCHOR_CEILING_FRACTION * wetGrams ||
            wetGrams - reading.grams < MIN_ANCHOR_RANGE_GRAMS
        when {
            tooLight || tooHeavy -> this

            // The provisional anchor is a guess (wet x 0.60). The first real
            // observation of "dry enough for this person and this plant"
            // replaces it outright, even when it is HIGHER - a running minimum
            // against a guess would let the guess win forever.
            dryIsProvisional -> copy(dryGrams = reading.grams, dryIsProvisional = false)

            // From here on it is a measured value, so take the running minimum.
            reading.grams < dryGrams -> copy(dryGrams = reading.grams)

            else -> this
        }
    }

    ReadingContext.ROUTINE -> this
}
