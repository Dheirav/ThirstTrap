package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosisTest {

    private fun leaves(node: DiagnosisNode): List<DiagnosisNode.Outcome> = when (node) {
        is DiagnosisNode.Outcome -> listOf(node)
        is DiagnosisNode.Question -> node.answers.flatMap { leaves(it.next) }
    }

    private fun depth(node: DiagnosisNode): Int = when (node) {
        is DiagnosisNode.Outcome -> 0
        is DiagnosisNode.Question -> 1 + node.answers.maxOf { depth(it.next) }
    }

    @Test
    fun `the four the requirements name are all present`() {
        assertEquals(
            setOf("fuzz", "browning", "cutface", "leafdrop"),
            diagnosisTrees.map { it.id }.toSet(),
        )
    }

    @Test
    fun `every path ends in something to do`() {
        diagnosisTrees.forEach { tree ->
            leaves(tree.root).forEach { outcome ->
                assertTrue(
                    "${tree.id} has an outcome with no advice: ${outcome.verdict}",
                    outcome.whatToDo.isNotEmpty(),
                )
            }
        }
    }

    @Test
    fun `no tree asks more than three questions deep`() {
        // Someone is holding a plant. A long questionnaire gets abandoned.
        diagnosisTrees.forEach { tree ->
            assertTrue("${tree.id} is ${depth(tree.root)} deep", depth(tree.root) <= 3)
        }
    }

    @Test
    fun `every question offers at least two ways out`() {
        fun check(node: DiagnosisNode) {
            if (node is DiagnosisNode.Question) {
                assertTrue("only ${node.answers.size} answers: ${node.prompt}", node.answers.size >= 2)
                node.answers.forEach { check(it.next) }
            }
        }
        diagnosisTrees.forEach { check(it.root) }
    }

    @Test
    fun `each tree can reach a benign outcome`() {
        // A checklist that always concludes something is wrong trains alarm.
        diagnosisTrees.forEach { tree ->
            assertTrue(
                "${tree.id} never says the plant is fine",
                leaves(tree.root).any { it.benign },
            )
        }
    }

    @Test
    fun `the fuzz tree opens with the dunk test`() {
        val root = diagnosisTreeById("fuzz")!!.root as DiagnosisNode.Question
        assertTrue(root.prompt.contains("Dunk", true))
        // The distinguishing detail has to be there, or the question is useless.
        assertTrue(root.how!!.contains("Root hairs"))
    }

    @Test
    fun `unknown ids resolve to nothing rather than a default`() {
        assertNull(diagnosisTreeById("nope"))
        assertNotNull(diagnosisTreeById("browning"))
    }
}
