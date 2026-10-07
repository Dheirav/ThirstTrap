package dev.dheirav.thirsttrap.feature.help

import dev.dheirav.thirsttrap.ui.Flank

import dev.dheirav.thirsttrap.ui.Space

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import dev.dheirav.thirsttrap.ui.AlmanacSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.dheirav.thirsttrap.domain.SuppressionHelp
import dev.dheirav.thirsttrap.domain.SuppressionReason
import dev.dheirav.thirsttrap.domain.helpFor
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.SectionHead

/**
 * Why the app is refusing to give you a date.
 *
 * Reached from the refusal itself, with [asked] set to the one you tapped, so
 * the answer arrives in the same breath as the question. Also browsable from
 * Help with [asked] null, for somebody who wants to know what the app can and
 * cannot do before meeting it.
 *
 * The one you asked about is shown first and in full. The rest follow, because
 * the seven refusals together are the most honest description of the model's
 * limits that exists anywhere in the app, and somebody who has hit one is the
 * person most likely to care about the others.
 *
 * A sheet, not a page: it is reached from a refusal, and the answer to "why
 * won't you tell me" belongs over the thing that refused rather than somewhere
 * you have to navigate to and come back from.
 *
 * `asked` used to arrive as a route argument, which made this look like it had
 * to be a destination. It did not: both callers already hold the reason as a
 * value, so passing it directly is simpler than encoding it in an address and
 * parsing it back out at the other end.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhyNoPredictionSheet(asked: SuppressionReason?, onDismiss: () -> Unit) {
    // Both "need another reading" cases print one headline, so showing both
    // would repeat the same heading twice on one page.
    val all = SuppressionReason.entries
        .distinctBy { helpFor(it).shown }
    val others = all.filter { it != asked && helpFor(it).shown != asked?.let { a -> helpFor(a).shown } }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AlmanacSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                // Both insets: this one is long enough to reach the top of the
                // window, and without the status bar padding its title renders
                // under the clock.
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.Block),
        ) {
            ScreenTitle("Why no date")
            Text(
                "The app would rather say nothing than name a day it cannot stand " +
                    "behind. A confident wrong date teaches watering by the calendar, " +
                    "which is the habit weighing exists to replace. There are seven " +
                    "things it can say instead, and each one has something you can do " +
                    "about it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.Block),
            )

            asked?.let {
                SectionHead("What you tapped")
                Explanation(helpFor(it), emphasised = true)
                SectionHead("The others it can say")
            } ?: SectionHead("What it can say")

            others.forEach { Explanation(helpFor(it), emphasised = false) }
            Rule(flank = Flank.Section)
        }
    }
}

@Composable
private fun Explanation(help: SuppressionHelp, emphasised: Boolean) {
    Column(Modifier.fillMaxWidth().padding(top = Space.Block)) {
        Text(
            "\"${help.shown}\"",
            style = if (emphasised) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            help.why,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Space.Line),
        )
        Text(
            help.whatToDo,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = Space.Line),
        )
        Rule(flank = Flank.Section)
    }
}
