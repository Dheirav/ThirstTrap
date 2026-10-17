package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckFeedbackTest {

    @Test
    fun `an uncalibrated pot gets no claim about the pot`() {
        // The app has no measurement, so it says what it did and nothing else.
        assertEquals(
            "Logged - Peperomia checked",
            checkedMessage("Peperomia", depletion = null, depletionTrigger = 0.5),
        )
    }

    @Test
    fun `agreeing with the scale stays quiet`() {
        // Still heavy, and the scale agrees it is still heavy. Reciting the
        // number here would be noise, and a confirmation teaches nothing.
        assertEquals(
            "Logged - Peperomia checked",
            checkedMessage("Peperomia", depletion = 0.31, depletionTrigger = 0.5),
        )
    }

    @Test
    fun `past the mark, the measurement is stated`() {
        // The case the old copy got wrong: it said "not thirsty yet" about a
        // pot the scale put past its trigger.
        assertEquals(
            "Logged - Peperomia checked. The scale has it 68% down, past its 50% mark",
            checkedMessage("Peperomia", depletion = 0.68, depletionTrigger = 0.5),
        )
    }

    @Test
    fun `exactly on the mark counts as past it`() {
        // Prediction treats the trigger as reached, not approached, so this
        // must not disagree with the rest of the app by half a gram.
        assertTrue(
            checkedMessage("Fig", depletion = 0.5, depletionTrigger = 0.5).contains("past its 50% mark"),
        )
    }

    @Test
    fun `the numbers are worded exactly as the plant's own page words them`() {
        // DepletionLine truncates the depletion and rounds the trigger. Same
        // arithmetic here, or the snackbar and the screen disagree by a point.
        assertTrue(
            checkedMessage("Fig", depletion = 0.689, depletionTrigger = 0.55).contains("68% down"),
        )
        assertTrue(
            checkedMessage("Fig", depletion = 0.689, depletionTrigger = 0.555).contains("past its 56% mark"),
        )
    }

    @Test
    fun `a pot past its dry anchor is still reported, not clamped out of sight`() {
        // depletionAt allows up to 1.2 because past the dry anchor is real, so
        // a message about such a pot must still name a figure over 100.
        val m = checkedMessage("Fig", depletion = 1.15, depletionTrigger = 0.5)
        assertTrue("\"$m\" should report a figure past 100%", Regex("1\\d\\d% down").containsMatchIn(m))
    }

    @Test
    fun `the figure always matches what DepletionLine would print`() {
        // The invariant, rather than a hand-picked number: DepletionLine does
        // (depletion * 100).toInt(), which truncates, and 1.15 * 100 is
        // 114.999... in binary floating point. So the page says 114% and this
        // must say 114% too. Agreeing with the screen matters more than being
        // arithmetically generous, because the two are read seconds apart.
        listOf(0.5, 0.501, 0.689, 0.999, 1.0, 1.15, 1.2).forEach { d ->
            val expected = (d * 100).toInt()
            assertTrue(
                "depletion $d should print as $expected%",
                checkedMessage("Fig", d, 0.5).contains("$expected% down"),
            )
        }
    }

    @Test
    fun `no version of this message grades the person`() {
        // The anti-goals apply to a snackbar too, and this is the one string in
        // the app that previously broke them: it praised a judgement it had not
        // checked. "Good call" is the phrase to keep out, and the evaluative
        // register with it, in both the agreeing and disagreeing cases.
        val banned = listOf(
            "good call", "well done", "nice", "correct", "wrong", "not quite",
            "almost", "should have", "you forgot", "oops", "careful",
        )
        val everyShape = listOf(
            checkedMessage("Fig", null, 0.5),
            checkedMessage("Fig", 0.1, 0.5),
            checkedMessage("Fig", 0.5, 0.5),
            checkedMessage("Fig", 0.99, 0.5),
            checkedMessage("Fig", 1.2, 0.5),
        )
        everyShape.forEach { message ->
            banned.forEach { phrase ->
                assertFalse("\"$message\" says '$phrase'", message.lowercase().contains(phrase))
            }
        }
    }

    @Test
    fun `restraint is logged in the same word as watering`() {
        // Both confirmations open "Logged - ". DashboardScreen and
        // ReminderActionReceiver both carry the comment explaining why: an
        // asymmetry here would teach that watering is the right answer.
        everyShape().forEach {
            assertTrue("\"$it\" should open like the watered message", it.startsWith("Logged - "))
        }
    }

    private fun everyShape() = listOf(
        checkedMessage("Fig", null, 0.5),
        checkedMessage("Fig", 0.1, 0.5),
        checkedMessage("Fig", 0.9, 0.5),
    )
}
