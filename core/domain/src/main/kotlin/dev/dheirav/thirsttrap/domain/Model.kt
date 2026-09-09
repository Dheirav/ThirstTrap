package dev.dheirav.thirsttrap.domain

import kotlinx.serialization.Serializable

/**
 * Domain entities. Plain Kotlin: no Room annotations, no Android types.
 * The data layer keeps its own persistence entities and maps to these.
 */

@Serializable
enum class Medium(val label: String) {
    SOIL("Soil"),
    WATER("Water"),
    SPHAGNUM("Sphagnum"),
    SEMI_HYDRO("Semi-hydro"),
    UNKNOWN("Unknown"),
}

@Serializable
enum class PlantStatus(val label: String) {
    ACTIVE("Active"),
    DORMANT("Dormant"),
    DEAD("Died"),
    GIVEN_AWAY("Given away"),
    UNKNOWN("Unknown"),
}

@Serializable
enum class PlantSource(val label: String, val hint: String?) {
    BOUGHT("Bought", null),
    CUTTING("Cutting", "tracked on the propagation board"),
    GIFT("Gift", null),
    VOLUNTEER("Volunteer", "turned up on its own"),
    UNKNOWN("Not sure", null),
}

@Serializable
enum class WateringMethod(val label: String) {
    TOP("From the top"),
    BOTTOM_SOAK("Bottom soak"),
    UNKNOWN("Unknown"),
}

/** The result of lifting the pot. A check that ends in *not* watering is worth logging. */
@Serializable
enum class CheckResult(val label: String) {
    STILL_HEAVY("Still heavy"),
    GETTING_LIGHT("Getting light"),
    DRY_WATERED("Dry, so I watered"),
    UNKNOWN("Unknown"),
}

@Serializable
enum class CareEventType(val label: String) {
    WATERED("Watered"),
    CHECKED("Checked"),
    FERTILIZED("Fertilised"),
    WATER_CHANGED("Water changed"),
    REPOTTED("Repotted"),
    MEDIUM_CHANGED("Medium changed"),
    PRUNED("Pruned"),
    TREATED("Treated"),
    PEST_OR_DISEASE("Pest or disease"),
    WEEDED("Weeded"),
    OBSERVATION("Observation"),
    MILESTONE("Milestone"),
    MOVED("Moved"),
    DIED("Died"),
    UNKNOWN("Other"),
}

/** Where a weight reading sits in the watering cycle. */
@Serializable
enum class ReadingContext(val label: String) {
    /** A mid-cycle weigh. These are what sharpen the prediction. */
    ROUTINE("routine"),

    /** Immediately before watering. Evidence about where "dry enough" really is. */
    PRE_WATER("dry"),

    /**
     * After watering and draining. Re-anchors the wet anchor.
     *
     * Deliberately NOT labelled "wet". PRE_WATER only nudges the dry end, which
     * is a guarded running minimum, so a loose reading of "dry" costs little.
     * This one resets the wet anchor outright, and "wet" would invite somebody
     * to pick it because the soil felt damp rather than because they had just
     * watered - which would silently recalibrate the plant against a pot that
     * was never full.
     */
    POST_WATER("just watered"),

    /**
     * Legacy. Nothing writes this any more: calibration used to be a separate
     * ceremony with its own dialog, and a post-water weigh does the same job
     * from wherever you happen to take it. Kept because databases written
     * before that change still contain these readings, and they are treated
     * exactly like POST_WATER wherever the anchor is derived.
     */
    CALIBRATION("calibration"),
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
    /**
     * The user's own answer to "is weighing this pot meaningful?". Some pots
     * the medium cannot rule out are still hopeless: a closed terrarium
     * recycles its water, so it loses almost nothing and the model would say
     * "not drying measurably" forever while the weighing round kept asking.
     */
    val weightTracked: Boolean = true,
    val archived: Boolean = false,
    /** Explicitly chosen cover. Null means "use the most recent photo". */
    val coverPhotoId: String? = null,
    val propagationStage: PropagationStage? = null,
    val propagationStageSinceMillis: Long? = null,
) {
    /**
     * On the propagation board if it has a stage, or if it arrived as a cutting
     * and has not been given one yet - so taking a cutting is enough to make it
     * appear, with no extra step to forget.
     */
    val isPropagating: Boolean
        get() = propagationStage != null ||
            (source == PlantSource.CUTTING && status == PlantStatus.ACTIVE)

    val effectiveStage: PropagationStage?
        get() = propagationStage ?: if (isPropagating) PropagationStage.CUTTING else null

    val isWeightTrackable: Boolean
        get() = weightTracked && medium != Medium.WATER
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
    /**
     * Set on the MILESTONE event a stage change writes.
     *
     * The note already said "Moved to rooting", but deriving a statistic by
     * parsing that prose would break silently the first time somebody reworded
     * it, and a wrong average is worse than no average. Null on every event
     * written before this column existed, which the stats screen states rather
     * than quietly averaging over.
     */
    val propagationStage: PropagationStage? = null,
)

@Serializable
data class WeightReading(
    val id: String,
    val plantId: String,
    val timestampMillis: Long,
    /** Paired with the timestamp, as care events are - see docs/DATA-MODEL.md. */
    val tzOffsetMinutes: Int = 0,
    val grams: Double,
    val context: ReadingContext,
    val excluded: Boolean = false,
)
