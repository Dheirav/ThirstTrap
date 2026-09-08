package dev.dheirav.thirsttrap.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/**
 * Lets one item in a padded list reach both screen edges.
 *
 * A `LazyColumn` with `contentPadding` applies that gutter to every item, so a
 * full-bleed hero inside one cannot simply cancel it: `Modifier.padding(-16.dp)`
 * throws `IllegalArgumentException: Padding must be non-negative` at runtime,
 * not at compile time, which is how it reached a device.
 *
 * Measuring wider than the incoming constraints and placing back by the same
 * amount is the supported way to do it.
 */
fun Modifier.fullBleed(gutter: Dp): Modifier = layout { measurable, constraints ->
    val extra = gutter.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(
            maxWidth = constraints.maxWidth + extra,
            minWidth = (constraints.minWidth + extra).coerceAtMost(constraints.maxWidth + extra),
        ),
    )
    layout(placeable.width, placeable.height) {
        placeable.place(-gutter.roundToPx(), 0)
    }
}
