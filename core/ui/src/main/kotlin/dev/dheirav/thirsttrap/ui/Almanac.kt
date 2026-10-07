package dev.dheirav.thirsttrap.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import dev.dheirav.thirsttrap.ui.Space

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

/**
 * How much air a rule gets. There are exactly two answers.
 *
 * Eighteen call sites used to pass their own padding and between them picked
 * from seven different values: 4, 8, 12, 16, and three different pairs for the
 * two sides. A rule's job is to say where one thing stops and the next starts,
 * and it cannot say that consistently if the distance it holds is decided
 * separately each time it is drawn.
 *
 * [Flank.Entry] is the common case and the default: a rule between entries in a
 * list, which carries no padding at all because the entries own their own
 * height. [Flank.Section] is a rule that ends a section, and takes a section's
 * worth of air on both sides.
 */
enum class Flank { Entry, Section }

/** A hairline. The workhorse - it replaces every card border in the app. */
@Composable
fun Rule(
    modifier: Modifier = Modifier,
    flank: Flank = Flank.Entry,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    // A hairline by default, and one caller legitimately wants heavier: the
    // plant timeline marks a life event (repotted, medium changed, died) with a
    // 2dp tertiary rule. Leaving that as a raw HorizontalDivider would have kept
    // a second divider vocabulary alive for the sake of one deliberate exception.
    thickness: Dp = 1.dp,
) = HorizontalDivider(
    modifier.then(
        if (flank == Flank.Section) Modifier.padding(vertical = Space.Section) else Modifier,
    ),
    thickness = thickness,
    color = color,
)

/**
 * The same hairline turned on its side, for a column boundary.
 *
 * A printed table separates its columns with a rule, not with whitespace,
 * because whitespace between two lists of the same thing reads as one list with
 * a gap in it. That is exactly what two adjacent filmstrips looked like.
 */
@Composable
fun VerticalRule(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
) = VerticalDivider(modifier, thickness = 1.dp, color = color)

/**
 * The double rule under a masthead.
 *
 * A thick rule with a thin one below it, which is how a title block was set
 * when the alternative was hand-cutting a decorative border.
 */
@Composable
fun DoubleRule(modifier: Modifier = Modifier) {
    // The flank is baked in, because a DoubleRule only ever does one job: it is
    // the rule under a running head. Eight call sites each passed their own and
    // between them used four different pairs, which made the same piece of
    // furniture sit differently on every screen that used it.
    Column(modifier.padding(top = Space.Line, bottom = Space.Entry)) {
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface)
        androidx.compose.foundation.layout.Spacer(Modifier.height(Space.Hair))
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * The almanac's title voice: caps at wide tracking.
 *
 * `titleLarge` at 0.22em was written out by hand in three places, and the tracking
 * is the whole character of it, so three copies is three chances to drift. It only
 * works on a short word, which is why it belongs to the masthead, the two screen
 * titles that are the app's own name, and nothing else. [SectionHead] is the same
 * idea a size down at 0.18em.
 *
 * Deliberately a text style and not a layout: the dashboard's title has to sit
 * inside a `TopAppBar` title slot and the intro's is followed by a subtitle, so
 * neither can take a component that owns its own column.
 */
@Composable
fun AlmanacTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.titleLarge,
        letterSpacing = 0.22.em,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
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
                AlmanacTitle(title)
                date?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        letterSpacing = 0.08.em,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Space.Hair),
                    )
                }
            }
            trailing()
        }
        DoubleRule()
    }
}

/**
 * The label over one control or one group of controls inside a form.
 *
 * This is NOT [SectionHead] and the difference is the point. A section head
 * divides a page and earns a rule; a field label names the thing directly under
 * it and must not, or a five-field form grows five horizontal rules and reads as
 * five pages. The app had this role in three voices before it had a name for it:
 * bare labelLarge in eleven places, titleSmall with SemiBold in six, and a
 * private composable in Settings.
 */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

/** A section head: letterspaced caps over a hairline. */
@Composable
fun SectionHead(text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = Space.Section)) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 0.18.em,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Rule()
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

/**
 * A dialog set like a page of the same book.
 *
 * Material's `AlertDialog` is a lifted tonal container with a shadow, a
 * sentence-case headline and no rules, which is three of the four things the
 * rest of this app deliberately does not do. Next to a screen built from paper,
 * hairlines and letterspaced caps it reads as a different application, and
 * prompts are where people actually stop and read.
 *
 * So: the surface colour of the page rather than a raised one, a hairline
 * border instead of a shadow, the title in the running-head voice over a double
 * rule, and a rule above the actions. Built on [BasicAlertDialog] because
 * `AlertDialog` gives no way to turn its shadow off.
 *
 * [dismiss] is drawn first and to the left of [confirm], which is the order the
 * platform uses, so muscle memory still works.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlmanacDialog(
    title: String,
    onDismissRequest: () -> Unit,
    confirm: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismiss: @Composable (() -> Unit)? = null,
    body: @Composable ColumnScope.() -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = modifier) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            // Less at the bottom than the top, because the action row carries
            // its own button padding and a symmetric 20 left the dialog looking
            // bottom-heavy.
            Column(Modifier.padding(start = Space.Section, end = Space.Section, top = Space.Section, bottom = Space.Line)) {
                ScreenTitle(title)
                DoubleRule()
                body()
                Rule(flank = Flank.Section)
                Row(
                    Modifier.fillMaxWidth().padding(top = Space.Tight),
                    horizontalArrangement = Arrangement.End,
                ) {
                    dismiss?.invoke()
                    confirm()
                }
            }
        }
    }
}

/**
 * The body of a prompt: one paragraph, in the voice the pages use.
 *
 * A convenience, because every dialog wants exactly this and passing the style
 * at twelve call sites is how twelve call sites drift apart.
 */
@Composable
fun DialogText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * A menu set like the rest of the app, for the same reason as [AlmanacDialog].
 *
 * Material's menu is a raised tonal container with a shadow. Against a page
 * made of paper and hairlines that reads as a panel from somewhere else, and a
 * menu is a list of lines, which is exactly what this app already knows how to
 * set: the page's own surface, a hairline round it, and no lift.
 */
@Composable
fun AlmanacMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        containerColor = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        content = content,
    )
}

/**
 * A bottom sheet in the same voice as [AlmanacDialog].
 *
 * The three sheets in the app had already given up their drag handles, on the
 * grounds that a printed page does not have one, and then kept Material's
 * raised tonal container, its shadow and a rounded top. This finishes the job:
 * the page's own surface, a hairline along the top edge where the rule belongs,
 * and no lift.
 *
 * Square, because `shapes.large` was the one place in the app still rounding a
 * corner by choice rather than by Material's default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlmanacSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        // A printed page has no drag handle; the rule below says "this panel
        // starts here", which is the same job done in the app's own grammar.
        dragHandle = null,
    ) {
        Rule()
        content()
    }
}

/**
 * A section that can be folded away, in the almanac's own voice.
 *
 * The app had no collapse component at all, which is why every screen that
 * wanted progressive disclosure reached for an `if` on state instead. That
 * hides a field without telling anyone it exists, which is concealment rather
 * than disclosure: the point of the pattern is that the reader can see there is
 * more and choose to look.
 *
 * The header is [SectionHead]'s typography so a folded section and an open one
 * are the same kind of thing, with the current value on the right. That summary
 * is what the section HOLDS, never how much of it is missing. No count, no
 * "3 of 7", no meter: a completeness indicator is a score, and a bar filling
 * toward full is the grammar of task completion this design already refuses.
 *
 * The chevron is drawn rather than imported. core/ui has no icon source, the
 * project having dropped material-icons-extended for shipping several thousand
 * glyphs to use sixteen, and two hairlines suit the page better than a filled
 * Material arrow would.
 */
@Composable
fun Disclosure(
    label: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    initiallyOpen: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Keyed, because an unkeyed remember captures whatever initiallyOpen was on
    // the FIRST composition and never looks again. The edit screen composes
    // while the plant is still loading, when its id is null and so isNew is
    // true, which made every section fold shut and stay shut after the plant
    // arrived. "Folded when adding, open when editing" had therefore never
    // worked for editing, which is the half it was written for.
    //
    // The cost is that toggling a section by hand is undone if initiallyOpen
    // later changes. It changes exactly once, from loading to loaded, inside a
    // moment of the screen opening, so there is nothing yet to undo.
    var open by remember(initiallyOpen) { mutableStateOf(initiallyOpen) }
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .semantics {
                    contentDescription =
                        if (open) "$label, open. Tap to fold away." else "$label, folded. Tap to open."
                }
                .padding(top = Space.Section, bottom = Space.Tight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.18.em,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (summary != null) {
                Text(
                    summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = Space.Entry),
                    textAlign = TextAlign.End,
                )
            } else {
                Column(Modifier.weight(1f)) {}
            }
            Chevron(open, Modifier.padding(start = Space.Line))
        }
        Rule()
        AnimatedVisibility(open) { Column(content = content) }
    }
}

/**
 * A foldable whose label is a sentence rather than a section name.
 *
 * [Disclosure] sets its label in the letterspaced caps of a section head, which
 * is right for WHAT IT IS and CARE PROFILE and wrong for "Why weight beats a
 * calendar": caps at wide tracking only work on a short word, which is what
 * [AlmanacTitle] says about the same voice a size up.
 *
 * So this is a sibling rather than a parameter on that one. It shares the
 * grammar deliberately, the same chevron and the same rule under the row, so a
 * reader learns one gesture; what differs is the heading voice, because a help
 * topic is a question somebody has and a form section is a name for a group of
 * fields.
 */
@Composable
fun Topic(
    title: String,
    modifier: Modifier = Modifier,
    initiallyOpen: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by remember(initiallyOpen) { mutableStateOf(initiallyOpen) }
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .semantics {
                    contentDescription =
                        if (open) "$title, open. Tap to fold away." else "$title, folded. Tap to open."
                }
                .padding(top = Space.Entry, bottom = Space.Line),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Chevron(open, Modifier.padding(start = Space.Line))
        }
        Rule()
        AnimatedVisibility(open) { Column(content = content) }
    }
}

/** Two hairlines meeting at a point, pointing down when folded. */
@Composable
private fun Chevron(open: Boolean, modifier: Modifier = Modifier) {
    val turn by animateFloatAsState(if (open) 180f else 0f, label = "chevron")
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.size(12.dp).rotate(turn)) {
        val w = size.width
        val h = size.height
        drawLine(ink, Offset(0f, h * 0.3f), Offset(w / 2f, h * 0.75f), 1.5.dp.toPx(), StrokeCap.Round)
        drawLine(ink, Offset(w / 2f, h * 0.75f), Offset(w, h * 0.3f), 1.5.dp.toPx(), StrokeCap.Round)
    }
}
