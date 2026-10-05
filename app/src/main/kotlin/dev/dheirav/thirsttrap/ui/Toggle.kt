package dev.dheirav.thirsttrap.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A switch with square corners, because Material's is a pill with a disc in it.
 *
 * This was the last pill in the app and the one that gave the game away worst:
 * the settings page put fully rounded toggles directly beside blocks with hard
 * corners, so a single screen spoke two shape languages and neither of them
 * meant anything. Buttons.kt squared every other control by passing a shape,
 * but [androidx.compose.material3.Switch] takes no shape parameter at all. Its
 * track and thumb both come from Material's own tokens, so the only way to
 * square it is to draw it.
 *
 * It lives in the app module rather than beside the buttons in core/ui because
 * it animates with [Motion], which reads the system's reduce-motion setting and
 * is an app-level concern. Both modules use this package, so a screen imports
 * it exactly as it imports the buttons.
 *
 * What is kept from Material is everything that is not the shape. The semantics
 * are still `Role.Switch`, so a screen reader announces a switch rather than a
 * button, and the touch target is still the 48dp minimum even though the drawn
 * track is 32dp high.
 *
 * On is a filled block and off is a ruled one, which is the same filled-versus-
 * ruled distinction the buttons already use for emphasis, rather than a second
 * vocabulary invented for toggles.
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // Material's own switch is 52x32 with a 24dp thumb, and those proportions
    // were carried over unchanged when this was squared. They should not have
    // been: a disc of a given size reads smaller than a square of the same
    // size, because the square's corners carry area the circle does not, so
    // squaring Material's numbers made a control that measured the same and
    // looked heavier. Scaled down until it sits with the type beside it rather
    // than over it.
    //
    // The touch target is NOT scaled. It stays at the 48dp minimum through
    // minimumInteractiveComponentSize below, so this is a change to what is
    // drawn and not to what can be hit.
    val trackW = 44.dp
    val trackH = 26.dp
    val thumb = 18.dp
    val pad = 4.dp
    val offset by animateDpAsState(
        targetValue = if (checked) trackW - thumb - pad else pad,
        animationSpec = Motion.settle(),
        label = "switch",
    )
    val on = MaterialTheme.colorScheme.primary
    val rule = MaterialTheme.colorScheme.outline
    val shape = MaterialTheme.shapes.small

    Box(
        modifier
            .minimumInteractiveComponentSize()
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(
                        value = checked,
                        enabled = enabled,
                        role = Role.Switch,
                        onValueChange = onCheckedChange,
                    )
                } else {
                    Modifier
                },
            )
            .size(trackW, trackH)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(shape)
            .background(if (checked) on else Color.Transparent)
            .border(1.dp, if (checked) on else rule, shape),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = offset)
                .size(thumb)
                .clip(shape)
                .background(if (checked) MaterialTheme.colorScheme.onPrimary else rule),
        )
    }
}
