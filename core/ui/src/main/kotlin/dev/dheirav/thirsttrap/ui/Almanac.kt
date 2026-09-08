package dev.dheirav.thirsttrap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

/**
 * The furniture of a printed page.
 *
 * An almanac is built from three things: rules, running heads, and entries. Not
 * cards. A card is a container that says "this is a separate object"; a rule
 * says "this is the same page, further down", which is the truer statement
 * about a list of plants you are keeping.
 *
 * This also quietly removes the last piece of gamification grammar. A card can
 * be full or empty, done or undone. A dated entry cannot: it either happened or
 * the line is blank, and a blank line in an almanac is not a failure, it is a
 * day when nothing needed doing.
 */

/** A hairline. The workhorse - it replaces every card border in the app. */
@Composable
fun Rule(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
) = HorizontalDivider(modifier, thickness = 1.dp, color = color)

/**
 * The double rule under a masthead.
 *
 * A thick rule with a thin one below it, which is how a title block was set
 * when the alternative was hand-cutting a decorative border.
 */
@Composable
fun DoubleRule(modifier: Modifier = Modifier) {
    Column(modifier) {
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface)
        androidx.compose.foundation.layout.Spacer(Modifier.height(2.dp))
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * The running head: what this page is, and what day it is.
 *
 * The date is the spine of an almanac. Putting it at the top of the list is not
 * decoration - it is the reason the rest of the page is arranged the way it is.
 */
@Composable
fun Masthead(
    title: String,
    date: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    // Caps at wide tracking is the almanac title voice. It only
                    // works on a short word, which is why it is reserved for the
                    // masthead and section heads and used nowhere else.
                    letterSpacing = 0.22.em,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                date?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        letterSpacing = 0.08.em,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            trailing()
        }
        DoubleRule(Modifier.padding(top = 8.dp))
    }
}

/** A section head: letterspaced caps over a hairline. */
@Composable
fun SectionHead(text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 20.dp)) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.18.em,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Rule(Modifier.padding(top = 4.dp))
    }
}

/**
 * A column heading over a table, in the same voice as a section head but
 * quieter - it labels data rather than announcing a part of the page.
 */
@Composable
fun ColumnHead(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 0.14.em,
        textAlign = align,
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier,
    )
}

/**
 * A screen's title, in the running-head voice.
 *
 * Every `TopAppBar` in the app used to set its title as plain sentence-case
 * text, which left the dashboard shouting in letterspaced caps while eleven
 * other screens whispered. One composable so they cannot drift apart again.
 */
@Composable
fun ScreenTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.titleMedium,
        letterSpacing = 0.18.em,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
