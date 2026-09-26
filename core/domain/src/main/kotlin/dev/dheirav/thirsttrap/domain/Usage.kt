package dev.dheirav.thirsttrap.domain

import kotlinx.serialization.Serializable

/**
 * What the app's own controls got used for - never what was typed into them.
 *
 * D31 and D32 were found by reading absences out of the diary, and the biggest
 * absences are invisible there: a sheet opened and closed without saving
 * leaves no row anywhere. This records exactly that class of event, locally,
 * for the friction report to read. Actions only: no notes, no amounts, no
 * grams, no photo paths. The strongest privacy rule is not collecting the
 * thing in the first place.
 */
@Serializable
enum class UsageKind {
    /** A logging flow was opened. */
    FLOW_OPENED,

    /** It ended in a save. */
    FLOW_COMPLETED,

    /**
     * It ended without one. The strongest "this is too much work" signal the
     * app can record - the watering-log gap ran for two weeks precisely
     * because giving up leaves no row of its own.
     */
    FLOW_ABANDONED,

    /**
     * A suggested value was changed before saving. A high override rate means
     * the suggestion logic is wrong for how this person actually works.
     */
    SUGGESTION_OVERRIDDEN,
}

@Serializable
data class UsageEvent(
    val id: String,
    val timestampMillis: Long,
    val tzOffsetMinutes: Int,
    val kind: UsageKind,
    /** Which flow: "log_event", "plant_edit", "weigh". A name, not content. */
    val flow: String,
    val plantId: String? = null,
    /** For overrides: "PRE_WATER->ROUTINE". Enum names only, never user text. */
    val detail: String? = null,
)

interface UsageRepository {
    suspend fun record(
        kind: UsageKind,
        flow: String,
        plantId: String? = null,
        detail: String? = null,
    )

    suspend fun all(): List<UsageEvent>
}
