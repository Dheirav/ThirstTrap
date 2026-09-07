package dev.dheirav.thirsttrap.domain

/**
 * Domain entities. Plain Kotlin: no Room annotations, no Android types.
 * The data layer keeps its own persistence entities and maps to these.
 */

enum class Medium { SOIL, WATER, SPHAGNUM, SEMI_HYDRO, UNKNOWN }

enum class PlantStatus { ACTIVE, DORMANT, DEAD, GIVEN_AWAY, UNKNOWN }

enum class PlantSource { BOUGHT, CUTTING, GIFT, VOLUNTEER, UNKNOWN }

enum class WateringMethod { TOP, BOTTOM_SOAK, UNKNOWN }

/** The result of lifting the pot. A check that ends in *not* watering is worth logging. */
enum class CheckResult { STILL_HEAVY, GETTING_LIGHT, DRY_WATERED, UNKNOWN }

enum class CareEventType {
    WATERED, CHECKED, FERTILIZED, WATER_CHANGED, REPOTTED, MEDIUM_CHANGED,
    PRUNED, TREATED, PEST_OR_DISEASE, WEEDED, OBSERVATION, MILESTONE, MOVED, DIED,
    UNKNOWN,
}

/** Where a weight reading sits in the watering cycle. */
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

data class WeightReading(
    val id: String,
    val plantId: String,
    val timestampMillis: Long,
    val grams: Double,
    val context: ReadingContext,
    val excluded: Boolean = false,
)
