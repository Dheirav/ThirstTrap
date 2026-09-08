package dev.dheirav.thirsttrap.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.FilledTonalButton as M3FilledTonalButton
import androidx.compose.material3.FilterChip as M3FilterChip
import androidx.compose.material3.OutlinedButton as M3OutlinedButton

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
val BlockHeight = 52.dp

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
    colors: SelectableChipColors = androidx.compose.material3.FilterChipDefaults.filterChipColors(),
) = M3FilterChip(
    selected = selected,
    onClick = onClick,
    label = label,
    modifier = modifier,
    enabled = enabled,
    shape = MaterialTheme.shapes.extraSmall,
    colors = colors,
)
