package dev.dheirav.thirsttrap.domain

/**
 * What to tell somebody who is looking at a refusal instead of a date.
 *
 * The app has seven ways to say "I do not know yet", and that terseness is the
 * point: section 6 of docs/WATERING-MODEL.md argues that stating a wrong date
 * confidently is worse than admitting ignorance, because the first teaches the
 * calendar-watering habit the whole weight method exists to replace.
 *
 * But a refusal is also the moment somebody most wants a reason, and none of the
 * seven was explained anywhere in the app. They were worded in two screens and
 * that was all. "Needs recalibrating" is honest and completely opaque if you do
 * not already know what an anchor is.
 *
 * This lives in the domain rather than in a composable for two reasons. The
 * `when` below is exhaustive, so adding an eighth reason will not compile until
 * somebody writes its explanation, and a test can assert the text is actually
 * there. Help that silently falls behind the code is worse than no help: it is
 * confidently wrong, which is the thing this app refuses to be about dates.
 */
data class SuppressionHelp(
    /** What the screen said, so the page can be recognised as the right one. */
    val shown: String,
    /** Why the app is refusing. The mechanism, briefly. */
    val why: String,
    /** The next action, or an honest "nothing" where there is none. */
    val whatToDo: String,
)

fun helpFor(reason: SuppressionReason): SuppressionHelp = when (reason) {
    SuppressionReason.NOT_CALIBRATED -> SuppressionHelp(
        shown = "Not calibrated",
        why = "The app does not know what this pot weighs when it is full. Depletion " +
            "is a fraction of the span between full and dry, so without the full end " +
            "there is no fraction to report and no weight to count down to.",
        whatToDo = "Water it thoroughly, let it drain for half an hour, then weigh it " +
            "and file the reading as \"just watered\". That one reading is the " +
            "calibration; there is no separate setup step.",
    )
    SuppressionReason.NEEDS_RECALIBRATION -> SuppressionHelp(
        shown = "Needs recalibrating",
        why = "A repot or a change of medium was logged. The pot is physically a " +
            "different object now, so every weight recorded before it describes " +
            "something that no longer exists.",
        whatToDo = "Weigh it after the next watering and file that as \"just watered\". " +
            "The old readings stay in the record and stop being used for prediction.",
    )
    SuppressionReason.NO_READINGS -> SuppressionHelp(
        shown = "Weigh once more to predict",
        why = "This pot has never been weighed, so there is nothing to fit a line " +
            "to. The app predicts from how fast a pot is actually losing water, " +
            "which means it needs weights and the times they were taken, and has " +
            "neither yet.",
        whatToDo = "Put the pot on a kitchen scale and save the number. Two readings " +
            "a day or two apart are enough to start, and the first one is most " +
            "useful taken just after watering.",
    )
    SuppressionReason.ONE_READING_NO_HISTORY -> SuppressionHelp(
        shown = "Weigh once more to predict",
        why = "One reading is a point, not a slope. A drying rate needs two weights " +
            "and the time between them, and this plant has no earlier cycles to " +
            "borrow a rate from either.",
        whatToDo = "Weigh it again in a day or two. After the first full cycle the app " +
            "can fall back on the plant's own history and will predict from a single " +
            "reading, saying so.",
    )
    SuppressionReason.NO_MEASURABLE_DRYING -> SuppressionHelp(
        shown = "Not drying measurably yet",
        why = "The change between readings is too small to separate from the scale's " +
            "own rounding. A loss smaller than one increment of your scale is a " +
            "rounding artefact whatever the pot weighs, and fitting a line to it " +
            "would invent a date.",
        whatToDo = "Leave it longer between weighings. A pot in a cool room in winter " +
            "can genuinely take a week to show a measurable change, and that is " +
            "information rather than a fault.",
    )
    SuppressionReason.WEIGHT_MEANINGLESS_FOR_MEDIUM -> SuppressionHelp(
        shown = "Weight won't help here",
        why = "This one is in water. A cutting in a jar does not dry out the way " +
            "substrate does, so its weight says nothing about when it needs " +
            "attention.",
        whatToDo = "Nothing. Change the water when it looks tired. If it goes into " +
            "soil later, switch the medium and weighing starts to mean something.",
    )
    SuppressionReason.WATERED_SINCE_LAST_READING -> SuppressionHelp(
        shown = "Weigh it to start the new cycle",
        why = "A watering was logged after the most recent weighing, so the newest " +
            "weight on file describes the pot before the can. Everything derived " +
            "from it would be about a plant that has already been watered.",
        whatToDo = "Weigh it now, ideally half an hour after watering so it has " +
            "drained, and file it as \"just watered\". That both clears this and " +
            "re-captures the full end of the range.",
    )
}
