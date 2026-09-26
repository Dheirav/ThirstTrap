package dev.dheirav.thirsttrap.domain

/**
 * The plants either side of [currentId] in a browse order.
 *
 * Swiping through plants from the detail page walks the dashboard's line
 * without the trip back. The order is a snapshot taken when the page opened,
 * not a live sort: attention re-ranks a plant the moment it is watered, and a
 * ribbon that reshuffles under the user's thumb turns "next" into "somewhere".
 *
 * No wraparound. The ends are ends - a ring gives "next" no meaning on a
 * four-plant shelf, and the stop is itself the signal that the line is done.
 */
data class Neighbors(val previousId: String? = null, val nextId: String? = null)

fun neighborsOf(orderedIds: List<String>, currentId: String): Neighbors {
    val i = orderedIds.indexOf(currentId)
    if (i < 0) return Neighbors()
    return Neighbors(
        previousId = orderedIds.getOrNull(i - 1),
        nextId = orderedIds.getOrNull(i + 1),
    )
}
