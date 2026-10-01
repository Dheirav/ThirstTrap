package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keeps the help from falling behind the code.
 *
 * Twice in two days a help page described an app that had changed under it, and
 * both times the user found it rather than the suite. The `when` in helpFor is
 * exhaustive, so an eighth reason will not compile until somebody writes its
 * explanation; these tests cover what the compiler cannot see, which is whether
 * the text that got written actually says anything.
 */
class SuppressionHelpTest {

    @Test
    fun `every refusal the app can show has an explanation`() {
        SuppressionReason.entries.forEach { reason ->
            val h = helpFor(reason)
            assertTrue("$reason has no 'shown' text", h.shown.isNotBlank())
            assertTrue("$reason has no 'why'", h.why.length > 40)
            assertTrue("$reason has no 'what to do'", h.whatToDo.length > 30)
        }
    }

    @Test
    fun `the wording matches what the screens actually print`() {
        // The page is recognisable only if its heading is the sentence the user
        // just read somewhere else. These five strings are duplicated in
        // WeightScreen's headline block, and a rename there without a rename
        // here would leave somebody looking at a page about a different thing.
        assertEquals("Not calibrated", helpFor(SuppressionReason.NOT_CALIBRATED).shown)
        assertEquals("Needs recalibrating", helpFor(SuppressionReason.NEEDS_RECALIBRATION).shown)
        assertEquals(
            "Not drying measurably yet",
            helpFor(SuppressionReason.NO_MEASURABLE_DRYING).shown,
        )
        assertEquals(
            "Weigh it to start the new cycle",
            helpFor(SuppressionReason.WATERED_SINCE_LAST_READING).shown,
        )
        // Both "need another reading" cases print the same headline on purpose,
        // because the distinction between them is not one a user can act on
        // differently.
        assertEquals(
            helpFor(SuppressionReason.NO_READINGS).shown,
            helpFor(SuppressionReason.ONE_READING_NO_HISTORY).shown,
        )
    }

    @Test
    fun `no explanation blames the user or hedges`() {
        // The anti-goals apply to help text too. "You should have" and "you
        // forgot" are the voice this app does not use, and "maybe" in a
        // what-to-do is an instruction that does not instruct.
        val banned = listOf("you forgot", "you should have", "you failed", "simply", "just do")
        SuppressionReason.entries.forEach { reason ->
            val all = (helpFor(reason).why + " " + helpFor(reason).whatToDo).lowercase()
            banned.forEach { phrase ->
                assertFalse("$reason says '$phrase'", all.contains(phrase))
            }
        }
    }
}
