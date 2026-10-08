package dev.dheirav.thirsttrap.domain

/**
 * What the usage rows add up to, per flow.
 *
 * [UsageEventEntity][dev.dheirav.thirsttrap.domain.UsageEvent] has been written
 * since D31 and never read: its own docstring says the rows exist "for the
 * friction report to read" and no such report existed, so the table grew and
 * told nobody anything.
 *
 * Counts, not a score. No grade, no colour, no threshold at which a flow is
 * "bad". The app refuses to score the person using it and there is no reason to
 * score the person who wrote it either; the numbers are here to be looked at
 * next to a complaint, not to be ranked.
 */
data class FlowFriction(
    val flow: String,
    val opened: Int,
    val completed: Int,
    val abandoned: Int,
    val overridden: Int,
) {
    /**
     * Opens that reached neither a save nor a deliberate exit.
     *
     * A flow the app was killed in the middle of leaves FLOW_OPENED with no
     * terminal event, so [opened] is not [completed] plus [abandoned]. Exposed
     * rather than absorbed, because quietly folding it into either one would
     * invent an outcome that was never recorded.
     */
    val unresolved: Int get() = opened - completed - abandoned

    /**
     * Of the times this flow ended one way or the other, the share that ended
     * without a save. Null when nothing has ended yet.
     *
     * The denominator is resolved outcomes, not [opened]. Dividing by opens
     * would let a few app kills look like giving up, which is the opposite of
     * what this number is for.
     */
    val abandonRate: Double?
        get() = (completed + abandoned).takeIf { it > 0 }?.let { abandoned.toDouble() / it }

    /**
     * Suggested values changed per save. Null when nothing has been saved.
     *
     * Can exceed 1: one save may override several suggestions.
     */
    val overrideRate: Double?
        get() = completed.takeIf { it > 0 }?.let { overridden.toDouble() / it }
}

/**
 * Groups usage rows by flow, most-abandoned first.
 *
 * Ordered by abandonments rather than by rate, because a flow abandoned nine
 * times out of thirty is a bigger thing to go and look at than one abandoned
 * once out of one, and a rate alone cannot tell those apart.
 */
fun frictionReport(events: List<UsageEvent>): List<FlowFriction> =
    events.groupBy { it.flow }
        .map { (flow, rows) ->
            FlowFriction(
                flow = flow,
                opened = rows.count { it.kind == UsageKind.FLOW_OPENED },
                completed = rows.count { it.kind == UsageKind.FLOW_COMPLETED },
                abandoned = rows.count { it.kind == UsageKind.FLOW_ABANDONED },
                overridden = rows.count { it.kind == UsageKind.SUGGESTION_OVERRIDDEN },
            )
        }
        .sortedWith(compareByDescending<FlowFriction> { it.abandoned }.thenBy { it.flow })

/**
 * Which suggestions got changed, and how often, for one flow.
 *
 * The detail field carries enum names only, "PRE_WATER->ROUTINE" and the like,
 * so this can be counted and shown without ever surfacing something the person
 * typed.
 */
fun overrideBreakdown(events: List<UsageEvent>, flow: String): List<Pair<String, Int>> =
    events.asSequence()
        .filter { it.kind == UsageKind.SUGGESTION_OVERRIDDEN && it.flow == flow }
        .mapNotNull { it.detail }
        .groupingBy { it }
        .eachCount()
        .toList()
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
