package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NeighborsTest {

    private val ids = listOf("a", "b", "c", "d")

    @Test
    fun `middle of the line has both neighbors`() {
        assertEquals(Neighbors("a", "c"), neighborsOf(ids, "b"))
    }

    @Test
    fun `first has no previous`() {
        assertEquals(Neighbors(null, "b"), neighborsOf(ids, "a"))
    }

    @Test
    fun `last has no next - no wraparound`() {
        assertEquals(Neighbors("c", null), neighborsOf(ids, "d"))
    }

    @Test
    fun `unknown id is nowhere, not an error`() {
        // An archived or deleted plant can still be the open page.
        assertEquals(Neighbors(null, null), neighborsOf(ids, "ghost"))
    }

    @Test
    fun `single plant has no neighbors`() {
        assertEquals(Neighbors(null, null), neighborsOf(listOf("only"), "only"))
    }

    @Test
    fun `empty order has no neighbors`() {
        assertEquals(Neighbors(null, null), neighborsOf(emptyList(), "a"))
    }
}
