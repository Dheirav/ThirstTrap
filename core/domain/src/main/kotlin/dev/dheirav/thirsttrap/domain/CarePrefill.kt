package dev.dheirav.thirsttrap.domain

/**
 * What a species entry can supply that the form has not already answered.
 *
 * This is the precedence rule, and it lives here rather than in the view model
 * because it is a rule about care data, not about UI state, and because this is
 * the layer that has tests. A field the user has touched always wins: the
 * prefill can only ever supply a missing answer, never replace a given one.
 *
 * Each null means "the form already has this, leave it alone".
 */
data class CarePrefill(
    val light: String? = null,
    val dryness: String? = null,
    val medium: Medium? = null,
    val depletionTriggerPct: Int? = null,
) {
    /** The field names, in reading order, for the one line that says what happened. */
    val filled: List<String>
        get() = listOfNotNull(
            light?.let { "light" },
            dryness?.let { "dryness" },
            medium?.let { "growing medium" },
            depletionTriggerPct?.let { "the depletion trigger" },
        )

    val isEmpty: Boolean get() = filled.isEmpty()
}

/** The default trigger a new plant starts on, and therefore the one we may replace. */
const val DEFAULT_DEPLETION_TRIGGER_PCT = 50

fun carePrefill(
    care: SpeciesCare,
    currentLight: String,
    currentDryness: String,
    currentMedium: Medium,
    currentTriggerPct: Int,
): CarePrefill = CarePrefill(
    light = care.light.takeIf { currentLight.isBlank() },
    dryness = care.asTargetDryness().takeIf { currentDryness.isBlank() },
    // SOIL is the form's default, so it is the one value we can treat as unset.
    // A user who picked WATER for a cutting in a jar must not have it undone by
    // typing the species afterwards.
    medium = care.medium.takeIf { currentMedium == Medium.SOIL && care.medium != Medium.SOIL },
    depletionTriggerPct = (care.depletionTrigger * 100).toInt()
        .takeIf { currentTriggerPct == DEFAULT_DEPLETION_TRIGGER_PCT },
)
