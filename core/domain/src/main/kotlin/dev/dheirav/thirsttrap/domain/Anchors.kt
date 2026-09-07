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
    val rangeGrams: Double get() = wetGrams - dryGrams

    /** 0.0 = just watered, 1.0 = at the dry anchor. Past 1.0 is real and worth showing. */
    fun depletionAt(grams: Double): Double =
        ((wetGrams - grams) / rangeGrams).coerceIn(0.0, 1.2)

    /** The weight at which this plant should be watered, for a given trigger fraction. */
    fun triggerWeight(depletionTrigger: Double): Double =
        wetGrams - depletionTrigger * rangeGrams

    /** Below this, drying is not distinguishable from scale noise. */
    val minMeaningfulSlope: Double
        get() = max(MIN_SLOPE_GRAMS_PER_DAY, MIN_SLOPE_RANGE_FRACTION * rangeGrams)

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
        copy(wetGrams = reading.grams)

    ReadingContext.PRE_WATER -> {
        val implausible = reading.grams < DRY_ANCHOR_FLOOR_FRACTION * wetGrams
        when {
            implausible -> this

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
