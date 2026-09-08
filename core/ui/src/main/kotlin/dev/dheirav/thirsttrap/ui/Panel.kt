package dev.dheirav.thirsttrap.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Card as M3Card

/**
 * A boxed note, ruled rather than filled.
 *
 * A Material card is a raised, tinted, rounded container - three signals that
 * all say "this is a separate surface floating above the page". An almanac has
 * the same idea and expresses it with one: a box drawn with a rule, on the same
 * paper as everything else.
 *
 * Shadows the Material name so a screen converts by changing an import. The
 * container is transparent by default: the paper shows through, which is the
 * whole point. A caller that passes its own [colors] - the diagnosis screen
 * tints one - still gets them.
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(containerColor = Color.Transparent),
    border: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    content: @Composable ColumnScope.() -> Unit,
) = M3Card(
    modifier = modifier,
    shape = MaterialTheme.shapes.small,
    colors = colors,
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    border = border,
    content = content,
)
