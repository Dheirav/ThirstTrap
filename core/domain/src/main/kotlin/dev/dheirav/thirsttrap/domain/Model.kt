package dev.dheirav.thirsttrap.domain

import kotlinx.serialization.Serializable

/**
 * Domain entities. Plain Kotlin: no Room annotations, no Android types.
 * The data layer keeps its own persistence entities and maps to these.
 */

@Serializable
enum class Medium { SOIL, WATER, SPHAGNUM, SEMI_HYDRO, UNKNOWN }

@Serializable
enum class PlantStatus { ACTIVE, DORMANT, DEAD, GIVEN_AWAY, UNKNOWN }

@Serializable
enum class PlantSource { BOUGHT, CUTTING, GIFT, VOLUNTEER, UNKNOWN }

@Serializable
enum class WateringMethod { TOP, BOTTOM_SOAK, UNKNOWN }

/** The result of lifting the pot. A check that ends in *not* watering is worth logging. */
@Serializable
enum class CheckResult { STILL_HEAVY, GETTING_LIGHT, DRY_WATERED, UNKNOWN }

@Serializable
enum class CareEventType {
    WATERED, CHECKED, FERTILIZED, WATER_CHANGED, REPOTTED, MEDIUM_CHANGED,
    PRUNED, TREATED, PEST_OR_DISEASE, WEEDED, OBSERVATION, MILESTONE, MOVED, DIED,
    UNKNOWN,
}

/** Where a weight reading sits in the watering cycle. */
@Serializable
enum class ReadingContext {
    /** A mid-cycle weigh. These are what sharpen the prediction. */
    ROUTINE,

    /** Immediately before watering. Evidence about where "dry enough" really is. */
    PRE_WATER,

    /** After watering and draining. Re-anchors the wet anchor. */
    POST_WATER,

    /** The initial wet-anchor capture during calibration. */
    CALIBRATION,
}

@Serializable
data class Plant(
    val id: String,
    val name: String,
    val species: String? = null,
    val medium: Medium = Medium.SOIL,
    val location: String? = null,
    val status: PlantStatus = PlantStatus.ACTIVE,
    val containerDesc: String? = null,
    val potDiameterCm: Double? = null,
    val hasDrainage: Boolean? = null,
    val source: PlantSource = PlantSource.UNKNOWN,
    val acquiredEpochDay: Long? = null,
    val targetDryness: String? = null,
    /**
     * The plant's usual watering amount in ml. Null means none has been set,
     * and the last amount actually logged stands in - so a standard emerges
     * from use even if it is never configured.
     */
    val defaultWaterMl: Double? = null,
    val lightNeeds: String? = null,
    val fertilizerCadenceDays: Int? = null,
    /** Fraction of the wet-dry range to deplete before watering. 0.5 is the commercial MAD default. */
    val depletionTrigger: Double = DEFAULT_DEPLETION_TRIGGER,
    val anchors: Anchors? = null,
    /** Cross-segment drying-rate prior, grams/day, negative. Null until one segment has closed. */
    val slopeEwmaGramsPerDay: Double? = null,
    val needsRecalibration: Boolean = false,
    val archived: Boolean = false,
    /** Explicitly chosen cover. Null means "use the most recent photo". */
    val coverPhotoId: String? = null,
) {
    val isWeightTrackable: Boolean
        get() = medium != Medium.WATER
}

@Serializable
data class CareEvent(
    val id: String,
    val plantId: String,
    val timestampMillis: Long,
    val tzOffsetMinutes: Int,
    val type: CareEventType,
    val note: String? = null,
    val amountMl: Double? = null,
    val method: WateringMethod? = null,
    val checkResult: CheckResult? = null,
    val fertilizerName: String? = null,
    val dilution: String? = null,
    val fromMedium: Medium? = null,
    val toMedium: Medium? = null,
    val cause: String? = null,
)

@Serializable
data class WeightReading(
    val id: String,
    val plantId: String,
    val timestampMillis: Long,
    val grams: Double,
    val context: ReadingContext,
    val excluded: Boolean = false,
)
