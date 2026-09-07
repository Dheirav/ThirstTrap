package dev.dheirav.thirsttrap.domain

import kotlinx.coroutines.flow.Flow

/**
 * Everything the weight model needs about one plant, already assembled.
 *
 * The UI reads this and renders; it does no fitting of its own.
 */
data class WeightState(
    val plant: Plant,
    val readings: List<WeightReading> = emptyList(),
    val segments: List<DryingSegment> = emptyList(),
    val prediction: Prediction = Prediction.NeedAnotherReading(SuppressionReason.NOT_CALIBRATED),
    val diagnostic: DryingDiagnostic? = null,
    /** 0..1.2, or null when there is nothing to place on the bar. */
    val depletion: Double? = null,
    val slopeGramsPerDay: Double? = null,
    val closedSegmentCount: Int = 0,
) {
    val isCalibrated: Boolean get() = plant.anchors != null && !plant.needsRecalibration
    val currentSegment: DryingSegment? get() = segments.lastOrNull()
    val latest: WeightReading? get() = currentSegment?.readings?.lastOrNull { !it.excluded }
}

interface WeightRepository {

    fun observeWeightState(plantId: String): Flow<WeightState>

    suspend fun addReading(
        plantId: String,
        grams: Double,
        context: ReadingContext,
    )

    suspend fun setExcluded(readingId: String, excluded: Boolean)

    suspend fun deleteReading(readingId: String)

    /** Records the wet anchor and starts the plant's first drying cycle. */
    suspend fun calibrate(plantId: String, wetGrams: Double, depletionTrigger: Double)

    /** Clears the anchors after a repot or a change of medium. */
    suspend fun markNeedsRecalibration(plantId: String)
}
