package dev.dheirav.thirsttrap.domain

import kotlin.math.abs

sealed interface SlopeFit {
    /** Grams per day. Negative means drying. */
    data class Fitted(
        val gramsPerDay: Double,
        val method: SlopeMethod,
        val readingCount: Int,
    ) : SlopeFit

    /** Fewer than two usable readings in the segment. */
    data object Insufficient : SlopeFit

    /** A slope inside the dead band: not enough time has passed, or something is wrong. */
    data class NoMeasurableDrying(val gramsPerDay: Double) : SlopeFit
}

enum class SlopeMethod { THEIL_SEN, TWO_POINT, EWMA_PRIOR }

/**
 * Median of pairwise slopes.
 *
 * Chosen over least squares because the real failure mode here is one bad
 * weigh-in — the pot set down half on the scale, a wet saucer included, a
 * misting five minutes earlier. Least squares lets a single outlier drag the
 * whole fit, and does it invisibly. Theil-Sen tolerates roughly 29% corrupted
 * points, which with 3-5 readings matters far more than the efficiency it gives
 * up on clean data. docs/WATERING-MODEL.md §4.
 *
 * @param points (days, grams). Pairs sharing an x are skipped rather than
 *        dividing by zero.
 */
fun theilSenSlope(points: List<Pair<Double, Double>>): Double? {
    if (points.size < 2) return null
    val slopes = mutableListOf<Double>()
    for (i in points.indices) {
        for (j in i + 1 until points.size) {
            val dx = points[j].first - points[i].first
            if (dx == 0.0) continue
            slopes += (points[j].second - points[i].second) / dx
        }
    }
    if (slopes.isEmpty()) return null
    slopes.sort()
    val mid = slopes.size / 2
    return if (slopes.size % 2 == 1) slopes[mid] else (slopes[mid - 1] + slopes[mid]) / 2.0
}

/**
 * Fits the drying rate of one segment over its most recent readings.
 *
 * Returns [SlopeFit.NoMeasurableDrying] for a slope inside the dead band, which
 * includes any positive slope — pots do not gain weight on their own, so that
 * is either noise or a watering the segmentation missed.
 */
fun fitSegmentSlope(segment: DryingSegment, anchors: Anchors?): SlopeFit {
    val readings = segment.readings.filterNot { it.excluded }.takeLast(MAX_FIT_READINGS)
    if (readings.size < 2) return SlopeFit.Insufficient

    val origin = readings.first().timestampMillis
    val points = readings.map { (it.timestampMillis - origin) / MILLIS_PER_DAY to it.grams }

    val slope = theilSenSlope(points) ?: return SlopeFit.Insufficient
    val method = if (readings.size == 2) SlopeMethod.TWO_POINT else SlopeMethod.THEIL_SEN

    val deadBand = anchors?.minMeaningfulSlope ?: MIN_SLOPE_GRAMS_PER_DAY
    if (slope > -deadBand) return SlopeFit.NoMeasurableDrying(slope)

    return SlopeFit.Fitted(slope, method, readings.size)
}

/**
 * Folds a closed segment's slope into the plant's cross-segment prior.
 *
 * The prior is what lets day 1 after watering show an estimate at all, and it
 * tracks the 2-5x seasonal swing in drying rate without anyone tuning anything.
 */
fun updateEwma(previous: Double?, closedSegmentSlope: Double): Double =
    if (previous == null) closedSegmentSlope
    else EWMA_ALPHA * closedSegmentSlope + (1 - EWMA_ALPHA) * previous

internal fun magnitude(slope: Double): Double = abs(slope)
