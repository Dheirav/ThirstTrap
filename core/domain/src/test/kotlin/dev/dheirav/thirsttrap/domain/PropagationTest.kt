package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PropagationTest {

    @Test
    fun `stages run forward in the order a cutting actually travels`() {
        assertEquals(PropagationStage.CALLUSING, PropagationStage.CUTTING.next)
        assertEquals(PropagationStage.ESTABLISHED, PropagationStage.POTTED.next)
        assertNull(PropagationStage.ESTABLISHED.next)
        assertNull(PropagationStage.CUTTING.previous)
    }

    @Test
    fun `days in stage counts from when it arrived there`() {
        val card = PropagationCard(plant(), PropagationStage.ROOTING, T0, T0)
        assertEquals(5, card.daysInStage(T0 + 5 * DAY))
    }

    @Test
    fun `a card with no stage history reports no duration rather than zero`() {
        assertNull(PropagationCard(plant(), PropagationStage.CUTTING, null, null).daysInStage(T0))
    }

    @Test
    fun `rooting is given six weeks before it is worth a look`() {
        assertFalse(stageIsStale(PropagationStage.ROOTING, 30))
        assertTrue(stageIsStale(PropagationStage.ROOTING, 45))
    }

    @Test
    fun `an established plant is never stale`() {
        assertFalse(stageIsStale(PropagationStage.ESTABLISHED, 3650))
    }
}
