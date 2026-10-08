package dev.dheirav.thirsttrap.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tour script, which is the part of a tour that can be wrong without
 * anybody noticing on a device.
 *
 * These are the app module's first JVM tests. Until now every view model and
 * every screen-level decision in `app/` had nowhere to be checked, and two of
 * this session's defects lived there: the pot list wiped by a state race, and a
 * reading that was its own predecessor.
 */
class TourScriptTest {

    private val steps = tourSteps()

    @Test
    fun itStartsOnTheDashboardAndVisitsEveryScreenItTalksAbout() {
        assertEquals(TourScreen.DASHBOARD, steps.first().screen)
        val visited = steps.map { it.screen }.toSet()
        assertTrue(
            "the tour never reaches: ${TourScreen.entries.toSet() - visited}",
            visited.containsAll(listOf(TourScreen.DASHBOARD, TourScreen.PLANT, TourScreen.DUE)),
        )
    }

    @Test
    fun nothingThatWritesIsEverATapThroughStep() {
        // The rule the whole design rests on. A tap-through step leaves the real
        // control exposed so the real tap lands, so a tour step on the droplet
        // would water somebody's plant for them.
        val writes = setOf(TourTarget.WATER_BUTTON, TourTarget.STILL_WET_BUTTON)
        val offenders = steps.filter { it.tapToContinue && it.target in writes }
        assertTrue("tap-through steps that would write an entry: $offenders", offenders.isEmpty())
    }

    @Test
    fun everyTapThroughStepSaysWhereItLeads() {
        assertTrue(steps.filter { it.tapToContinue }.all { it.leadsTo != null })
        assertTrue("a step that leads nowhere is not a tap-through step",
            steps.none { it.leadsTo != null && !it.tapToContinue })
    }

    @Test
    fun eachStepIsOnTheScreenThePreviousOneLeadsTo() {
        steps.zipWithNext().forEachIndexed { i, (a, b) ->
            val expected = a.leadsTo ?: a.screen
            assertEquals(
                "step ${i + 2} is on ${b.screen} but step ${i + 1} leaves you on $expected",
                expected, b.screen,
            )
        }
    }

    @Test
    fun everyCaptionIsShortEnoughToReadBesideTheThing() {
        // A caption is a label, not a paragraph. Past about 110 characters the
        // bubble is taller than the thing it points at.
        val long = steps.filter { it.caption.length > 110 }.map { it.target to it.caption.length }
        assertTrue("captions too long to sit beside their target: $long", long.isEmpty())
    }

    @Test
    fun captionsOnStepsYouCannotPressAreNotInstructions() {
        // "Save." read as an order on a step where the button could not be
        // pressed, in the app this was ported from. A looking step describes;
        // only a tap-through step tells you to tap.
        val bossy = steps.filter { !it.tapToContinue && it.caption.startsWith("Tap ") }
        assertTrue("looking steps written as instructions: ${bossy.map { it.caption }}", bossy.isEmpty())
    }

    @Test
    fun theTourAdvancesWhenYouReachTheScreenATapStepLeadsTo() {
        val i = steps.indexOfFirst { it.tapToContinue }
        val to = steps[i].leadsTo!!
        assertEquals(i + 1, advanceOnScreen(steps, i, to))
    }

    @Test
    fun aLookingStepDoesNotAdvanceOnAScreenChange() {
        val i = steps.indexOfFirst { !it.tapToContinue }
        assertEquals(i, advanceOnScreen(steps, i, TourScreen.MORE))
    }
}
