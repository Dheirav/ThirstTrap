package dev.dheirav.thirsttrap.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.FilledTonalButton as M3FilledTonalButton
import androidx.compose.material3.FilterChip as M3FilterChip
import androidx.compose.material3.FloatingActionButton as M3FloatingActionButton
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.material3.OutlinedButton as M3OutlinedButton
import androidx.compose.material3.TextButton as M3TextButton

/**
 * Buttons as printed blocks.
 *
 * Material 3 takes a button's shape from the `CornerFull` *token*, not from
 * `MaterialTheme.shapes`, so squaring the shape scale left every button in the
 * app a pill - the one control that most gives away a Material app, and the
 * detail most at odds with a page that is otherwise all right angles.
 *
 * These shadow the Material versions by name, so a screen adopts them by
 * changing an import rather than by editing every call site. Three things are
 * fixed here rather than per screen:
 *
 *  - **Square.** `MaterialTheme.shapes.small`, which is 0dp.
 *  - **One height.** The app had buttons at 40dp, 56dp and 60dp on a single
 *    screen. [BlockHeight] is the only one now, and it is above the 48dp
 *    accessibility floor.
 *  - **A rule, not a fill, for the quiet ones.** An outlined button in an
 *    almanac is a box drawn with a rule.
 */
/*
 * THE VOCABULARY. Which affordance means what.
 *
 * The shapes were fixed here before the meanings were, so the same committing
 * action was a filled Button on nine screens and a FilledTonalButton on two. A
 * filled and a tonal button are two emphasis levels, so using both for one
 * semantic role tells the reader there is a difference and then declines to say
 * what it is. That, not the palette, is what "like buttons" was pointing at.
 *
 *   Button              writes the record. One per surface.
 *   FilledTonalButton   starts a job that commits elsewhere, or acts in place.
 *   OutlinedButton      the alternative, or the non-writing action on the same
 *                       surface.
 *   TextButton          explains, dismisses, or acts inside a dialog.
 *   IconButton          top bar only.
 *   FilterChip          filters or picks among existing data. Never an action.
 *
 * ONE EXCEPTION, and it is load-bearing. Where two actions are both valid
 * answers to the same question they take equal weight, whatever the rule above
 * says. "Watered" and "Still wet" are the two taps this product exists to
 * collect, and promoting either to the filled primary would score watering
 * above restraint. That is the guilt mechanic the app refuses by design.
 */
val BlockHeight = 56.dp

/**
 * The quiet variants sit at the accessibility floor rather than at [BlockHeight].
 *
 * A text button is a word in a row of words, not a block, so giving it a block's
 * height puts a 56dp gap around "History". 48dp is the touch-target minimum and
 * the right answer for something that is only type.
 */
val TextBlockHeight = 48.dp

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit,
) = M3Button(
    onClick = onClick,
    modifier = modifier.heightIn(min = BlockHeight),
    enabled = enabled,
    shape = MaterialTheme.shapes.small,
    colors = colors,
    content = content,
)

@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = M3FilledTonalButton(
    onClick = onClick,
    modifier = modifier.heightIn(min = BlockHeight),
    enabled = enabled,
    shape = MaterialTheme.shapes.small,
    content = content,
)

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = M3OutlinedButton(
    onClick = onClick,
    modifier = modifier.heightIn(min = BlockHeight),
    enabled = enabled,
    shape = MaterialTheme.shapes.small,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    content = content,
)

@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    // Filled, and with no border, so a chip stops looking like an empty text
    // field. Material's default unselected chip is a transparent container
    // inside a hairline outline, which at this corner radius is the same
    // drawing as an OutlinedTextField: on the edit form that put four chips and
    // five empty fields on one screen as nine identical rectangles, with
    // nothing to say which you tap to choose and which you tap to type in.
    //
    // surfaceContainerHigh places it on the ramp Theme.kt measures: 1.30:1
    // against the background, clearly a filled surface and still below the
    // filled Card at 1.52:1, because a chip is smaller than a card and should
    // not shout louder than one.
    colors: SelectableChipColors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ),
) = M3FilterChip(
    selected = selected,
    onClick = onClick,
    label = label,
    modifier = modifier,
    enabled = enabled,
    shape = MaterialTheme.shapes.extraSmall,
    colors = colors,
    border = null,
)

/**
 * Text buttons were never wrapped, so they resolved to Material's pill.
 *
 * This is the shape the review actually reacted to: the quick-log sheet put two
 * fully rounded text buttons directly under a row of square blocks, in the same
 * sheet, four lines apart in the source.
 */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit,
) = M3TextButton(
    onClick = onClick,
    modifier = modifier.heightIn(min = TextBlockHeight),
    enabled = enabled,
    shape = MaterialTheme.shapes.small,
    contentPadding = contentPadding,
    content = content,
)

/**
 * Material's icon button is a circle, and its ripple is a circle whatever you
 * draw inside it. M3 gives no shape parameter here, so the square comes from
 * clipping the button itself, which is the idiom the screens already use for
 * images and swatches.
 */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) = M3IconButton(
    onClick = onClick,
    modifier = modifier.clip(MaterialTheme.shapes.small),
    enabled = enabled,
    content = content,
)

/**
 * Square and flat. The elevation is dropped because a shadow is the one thing
 * that cannot be printed, and the app's surfaces are otherwise separated by
 * rules rather than by lift.
 */
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = M3FloatingActionButton(
    onClick = onClick,
    modifier = modifier,
    shape = MaterialTheme.shapes.small,
    elevation = FloatingActionButtonDefaults.elevation(
        defaultElevation = 0.dp,
        pressedElevation = 0.dp,
        focusedElevation = 0.dp,
        hoveredElevation = 0.dp,
    ),
    content = content,
)
