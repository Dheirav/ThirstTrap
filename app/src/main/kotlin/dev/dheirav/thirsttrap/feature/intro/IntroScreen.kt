package dev.dheirav.thirsttrap.feature.intro

import dev.dheirav.thirsttrap.ui.Flank

import dev.dheirav.thirsttrap.ui.Space

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.navigation.compose.hiltViewModel
import dev.dheirav.thirsttrap.ui.Button
import dev.dheirav.thirsttrap.ui.DoubleRule
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.AlmanacTitle
import androidx.compose.foundation.layout.ColumnScope
import dev.dheirav.thirsttrap.feature.help.HelpPage

/**
 * The first-run page. One page, three things, one button.
 *
 * Deliberately not a swipeable carousel. Those get skipped, and the one thing
 * this app has to explain before anyone can use it is not a feature tour but a
 * premise: it weighs pots. Someone who does not learn that has a worse plant
 * diary than a paper notebook.
 *
 * Every line here is a claim the app actually keeps, which is why there is no
 * screenshot and no arrow: the teaching that matters happens later, at the
 * moment of use, and the weight screen already does it well.
 */
@Composable
fun IntroScreen(
    onDone: () -> Unit,
    /**
     * False when it is being re-read on purpose from Help. The page holds the
     * tipping trick, which is reference rather than a greeting, so it has to be
     * reachable again without pretending it had never been read.
     */
    markSeen: Boolean = true,
    viewModel: IntroViewModel = hiltViewModel(),
) {
    // Two shapes, because this page has two jobs. On first run it is a
    // greeting: its own masthead, no app bar, nothing to go back to, and one
    // button out. Re-read from Help it is the fourth item in a list of pages,
    // and it used to arrive with no app bar and no back arrow while every
    // other page in that list had both, which made it look like a different
    // app had opened. So the reference reading gets the standard help page.
    if (!markSeen) {
        HelpPage("What this app is for", onBack = onDone) { Points() }
        return
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Space.Section),
        ) {
            AlmanacTitle("ThirstTrap")
            Text(
                "A plant diary that weighs the pot",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = Space.Line),
            )
            DoubleRule()

            Points()

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { viewModel.dismiss(onDone) },
                modifier = Modifier.fillMaxWidth(),
                // Says what it does. "Add your first plant" was a label for a
                // button that only dismisses, and wrong twice over for anyone
                // who already has plants and is seeing this after an update.
            ) { Text("Open the diary") }
        }
    }
}

/** The three claims, identical in both readings of this page. */
@Composable
private fun ColumnScope.Points() {
            Point(
                n = "1",
                title = "It weighs pots, and that is the point",
                body = "A pot loses weight as it dries, steadily enough to fit a line to. " +
                    "Put one on a kitchen scale now and then and the app will tell you the " +
                    "day it actually needs water, instead of guessing from a calendar.\n\n" +
                    "Too heavy to lift? Tip it onto one edge with the scale under that side. " +
                    "Only the change matters, so tip it the same way each time and the " +
                    "number works just as well.",
            )
            Point(
                n = "2",
                title = "It is a diary, not a scoreboard",
                body = "No streaks, no counts of what you missed, no tone of voice about it. " +
                    "A reminder asks you to look at a plant; it never tells you off. " +
                    "\"Still wet, left it alone\" is a real answer and is logged as one.",
            )
            Point(
                n = "3",
                title = "It stays on this phone",
                body = "No account, no server. Backup writes one file you keep, wherever you " +
                    "keep things. The only thing that can ever leave is a plant name you " +
                    "ask it to look up, and that is off until you turn it on.",
                last = true,
            )

            Text(
                "Everything beyond the daily loop lives behind the three dots on the plants " +
                    "list: feeding, the propagation board, places, figures and experiments.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.Section),
            )

}

@Composable
private fun Point(n: String, title: String, body: String, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(top = Space.Tight)) {
        Text(
            n,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.Line),
            )
        }
    }
    if (!last) Rule(flank = Flank.Section)
}
