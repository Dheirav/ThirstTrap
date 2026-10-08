package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrictionTest {

    private var n = 0
    private fun ev(kind: UsageKind, flow: String, detail: String? = null) = UsageEvent(
        id = "u${n++}",
        timestampMillis = 1_700_000_000_000L + n,
        tzOffsetMinutes = 330,
        kind = kind,
        flow = flow,
        detail = detail,
    )

    @Test
    fun `no events is no report rather than a report of zeroes`() {
        assertEquals(emptyList<FlowFriction>(), frictionReport(emptyList()))
    }

    @Test
    fun `counts each kind within its own flow`() {
        val r = frictionReport(
            listOf(
                ev(UsageKind.FLOW_OPENED, "log_event"),
                ev(UsageKind.FLOW_COMPLETED, "log_event"),
                ev(UsageKind.FLOW_OPENED, "log_event"),
                ev(UsageKind.FLOW_ABANDONED, "log_event"),
                ev(UsageKind.FLOW_OPENED, "weigh"),
                ev(UsageKind.FLOW_COMPLETED, "weigh"),
                ev(UsageKind.SUGGESTION_OVERRIDDEN, "weigh", "ROUTINE->PRE_WATER"),
            ),
        ).associateBy { it.flow }

        assertEquals(2, r.getValue("log_event").opened)
        assertEquals(1, r.getValue("log_event").completed)
        assertEquals(1, r.getValue("log_event").abandoned)
        assertEquals(0, r.getValue("log_event").overridden)
        assertEquals(1, r.getValue("weigh").overridden)
    }

    @Test
    fun `abandon rate is over resolved outcomes, not over opens`() {
        // Three opens, one save, one give-up, and one the app died in.
        val f = frictionReport(
            listOf(
                ev(UsageKind.FLOW_OPENED, "weigh"),
                ev(UsageKind.FLOW_OPENED, "weigh"),
                ev(UsageKind.FLOW_OPENED, "weigh"),
                ev(UsageKind.FLOW_COMPLETED, "weigh"),
                ev(UsageKind.FLOW_ABANDONED, "weigh"),
            ),
        ).single()

        assertEquals(1, f.unresolved)
        // 1 of 2 that ended, not 1 of 3 that started. Dividing by opens would
        // report 0.33 and blame the user for a process death.
        assertEquals(0.5, f.abandonRate!!, 1e-9)
    }

    @Test
    fun `rates are null rather than zero when there is nothing to divide by`() {
        val f = frictionReport(listOf(ev(UsageKind.FLOW_OPENED, "weigh"))).single()
        assertNull("no outcome yet is not a 0% abandon rate", f.abandonRate)
        assertNull("no save yet is not a 0 override rate", f.overrideRate)
    }

    @Test
    fun `override rate can exceed one, because one save can override several`() {
        val f = frictionReport(
            listOf(
                ev(UsageKind.FLOW_COMPLETED, "log_event"),
                ev(UsageKind.SUGGESTION_OVERRIDDEN, "log_event", "A->B"),
                ev(UsageKind.SUGGESTION_OVERRIDDEN, "log_event", "C->D"),
            ),
        ).single()
        assertEquals(2.0, f.overrideRate!!, 1e-9)
    }

    @Test
    fun `ordered by abandonments, so the biggest thing to look at is first`() {
        val r = frictionReport(
            listOf(
                ev(UsageKind.FLOW_ABANDONED, "rarely_used"),
                ev(UsageKind.FLOW_COMPLETED, "rarely_used"),
                ev(UsageKind.FLOW_ABANDONED, "painful"),
                ev(UsageKind.FLOW_ABANDONED, "painful"),
                ev(UsageKind.FLOW_ABANDONED, "painful"),
            ),
        )
        // "painful" has the lower rate of the two (1.0 vs 0.5 is actually
        // higher, so compare counts): three give-ups beats one, which is the
        // ordering that sends you to the right screen.
        assertEquals("painful", r.first().flow)
        assertTrue(r.first().abandoned > r.last().abandoned)
    }

    @Test
    fun `ties between flows break by name, so the order is stable`() {
        val a = frictionReport(
            listOf(ev(UsageKind.FLOW_ABANDONED, "zebra"), ev(UsageKind.FLOW_ABANDONED, "alpha")),
        ).map { it.flow }
        assertEquals(listOf("alpha", "zebra"), a)
    }

    @Test
    fun `override breakdown counts the details of one flow only`() {
        val events = listOf(
            ev(UsageKind.SUGGESTION_OVERRIDDEN, "weigh", "ROUTINE->PRE_WATER"),
            ev(UsageKind.SUGGESTION_OVERRIDDEN, "weigh", "ROUTINE->PRE_WATER"),
            ev(UsageKind.SUGGESTION_OVERRIDDEN, "weigh", "ROUTINE->DRY"),
            ev(UsageKind.SUGGESTION_OVERRIDDEN, "log_event", "ROUTINE->PRE_WATER"),
            ev(UsageKind.FLOW_ABANDONED, "weigh"),
        )
        assertEquals(
            listOf("ROUTINE->PRE_WATER" to 2, "ROUTINE->DRY" to 1),
            overrideBreakdown(events, "weigh"),
        )
    }

    @Test
    fun `override breakdown ignores rows with no detail`() {
        val events = listOf(
            ev(UsageKind.SUGGESTION_OVERRIDDEN, "weigh", null),
            ev(UsageKind.SUGGESTION_OVERRIDDEN, "weigh", "A->B"),
        )
        assertEquals(listOf("A->B" to 1), overrideBreakdown(events, "weigh"))
    }
}
