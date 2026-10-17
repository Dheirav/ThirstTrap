package dev.dheirav.thirsttrap.domain

import kotlin.math.roundToInt

/**
 * What to say back when someone reports a pot still heavy.
 *
 * The line this replaces was "Good call - <name> checked, not thirsty yet", in
 * four places, and it was wrong twice over. It graded a judgement the app had
 * not checked, and "not thirsty yet" is a claim about the pot that is false
 * whenever the scale puts it past its own mark - which is precisely the case
 * worth saying anything about. The app was praising the user for being right at
 * the one moment it could see they were not.
 *
 * So the rule here is: no verdict, ever. When there is a measurement and it
 * disagrees, this states the number and the mark and lets the gap speak. The
 * user supplied a judgement, the scale supplies a fact, and the app does not
 * adjudicate between them out loud.
 *
 * Three constraints it has to satisfy at once, each from a different direction.
 *
 * **Parity.** `DashboardScreen` and `ReminderActionReceiver` both carry the same
 * comment next to this message: the confirmation for restraint has to be as warm
 * as the one for watering, because if watering felt rewarding and restraint felt
 * like a dismissal the app would be teaching that watering is the right answer.
 * Hence "Logged - ", the same opening as [the watered message][wateredMessage
 * in DashboardScreen], rather than a softer word for the quieter action.
 *
 * **Say it back.** The watering confirmation echoes the amount "so a one-tap log
 * is never a surprise". The same courtesy applied to a check is echoing the
 * measurement, which is the whole of this change.
 *
 * **Quiet when it agrees.** A confirmation teaches nothing: committing to a
 * judgement improves what you remember only for outcomes that violate it, and
 * a number recited on every check trains people to stop reading the line. So
 * the measurement appears only when it contradicts the judgement just given.
 *
 * Found by council review, 2026-10-17. See `docs/GAMES-COUNCIL.md`.
 *
 * @param depletion where the pot sits in its own range, 0.0 just watered and
 *        1.0 at the dry anchor, or null when the plant is not calibrated and
 *        the app has nothing to compare against.
 */
fun checkedMessage(name: String, depletion: Double?, depletionTrigger: Double): String {
    val logged = "Logged - $name checked"
    if (depletion == null || depletion < depletionTrigger) return logged
    // The same words DepletionLine uses on the plant's own page. A lesson
    // phrased differently from the screen it refers to is two lessons.
    val pct = (depletion * 100).toInt()
    val mark = (depletionTrigger * 100).roundToInt()
    return "$logged. The scale has it $pct% down, past its $mark% mark"
}
