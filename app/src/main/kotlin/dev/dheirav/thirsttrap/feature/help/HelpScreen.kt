package dev.dheirav.thirsttrap.feature.help

import dev.dheirav.thirsttrap.ui.Space
import dev.dheirav.thirsttrap.ui.GoChevron

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import dev.dheirav.thirsttrap.ui.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.Button
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.ScreenTitle
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Two screens, because they answer two different questions.
 *
 * This was one screen called Help, reached from a Settings button labelled
 * "Something is not working", and five of its six entries explained how the app
 * works. The button promised troubleshooting and delivered documentation.
 *
 * Splitting it also settles where one entry belongs. "Why it sometimes won't
 * give a date" reads like documentation and is not: somebody meets it because
 * the app is refusing to do the thing they expected, which is the definition of
 * troubleshooting. It sits with the reminders, not with the explainers.
 *
 * The rule from D50 still holds for both: if an entry *does* something rather
 * than explaining something, it belongs on the thing it acts on and not here.
 * docs/NAVIGATION.md section 8.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowItWorksScreen(
    onBack: () -> Unit,
    onStartTour: () -> Unit,
    onOpenIntro: () -> Unit,
    onOpenWayfinding: () -> Unit,
    onOpenBackupHelp: () -> Unit,
) {
    // Weighing help is not listed here. It is a sheet rather than a page,
    // because WeightScreen opens the same sheet from a Help button in its own
    // app bar and from its diagnostic card, which is help at the moment of
    // use. Listed among three pages it was the one entry that opened a
    // different kind of thing, and a list whose items behave differently is
    // the thing this whole pass was fixing.
    //
    // Nothing is lost by dropping it. It is offered every time somebody
    // actually weighs a pot, and the tipping trick for a pot too heavy to
    // lift is also point 1 of "What this app is for", which is still here.
    HelpPage("How the app works", onBack) {
        // Reading order rather than alphabetical: why it exists, the one
        // mechanic it rests on, where things are, and then the thing you only
        // care about once you have data worth keeping.
        // First, and before the reading. The app it was ported from replaced
        // four pages of explanation with this, and "Finding your way around"
        // below is still those four pages: a text index of what is on each tab
        // and what is behind the two dots menus. Somebody who taps the weighing
        // round has found it; somebody who read that it exists has not.
        Entry(
            title = "Show me around",
            body = "A short walk through the app, pointing at the real thing each " +
                "time. Nothing is logged on the way.",
            onClick = onStartTour,
        )
        Entry(
            title = "What this app is for",
            body = "The first-run page again: why it weighs pots, and why there are no " +
                "streaks or counts of what you missed.",
            onClick = onOpenIntro,
        )
        Entry(
            // Not "finding your way around" any more: that is what the tour
            // does, and two entries offering the same thing is the choice
            // nobody wants to make. This one is the reference for the parts a
            // spotlight can point at but cannot enumerate.
            title = "What is in the menus",
            body = "What is behind the two dots menus, what the photo viewer can do, and " +
                "the only two gestures the app cannot tell you about itself.",
            onClick = onOpenWayfinding,
        )
        Entry(
            title = "Backups, and what you lose without one",
            body = "What is in a backup, what restoring does and does not do, and why " +
                "photos are the part that cannot be replaced.",
            onClick = onOpenBackupHelp,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TroubleshootScreen(
    onBack: () -> Unit,
    onOpenReminderHelp: () -> Unit,
) {
    // No particular reason asked about from here, so the sheet lists them all.
    var whyNoDate by remember { mutableStateOf(false) }
    if (whyNoDate) WhyNoPredictionSheet(asked = null, onDismiss = { whyNoDate = false })
    HelpPage("Something is wrong", onBack) {
        Entry(
            title = "Reminders are not arriving",
            body = "Almost always battery optimisation or a notification channel. This " +
                "checks the specific settings Android hides, and can run the real " +
                "background sweep rather than just posting a notification.",
            onClick = onOpenReminderHelp,
        )
        Entry(
            title = "It won't tell me when to water",
            body = "The seven things the app says instead of a date, what each one means, " +
                "and what to do about it.",
            onClick = { whyNoDate = true },
        )
        // Signposting rather than a route, because the answer is genuinely
        // somewhere else and somebody who remembers it being here needs telling
        // once. D50 moved it onto the plant it is about.
        Text(
            "A plant that does not look right is handled on the plant itself: open it, " +
                "then \"Something looks wrong\" in its menu. It needs to know which " +
                "plant you mean, which is why it is not here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The frame both share, so they cannot drift apart. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Entry(title: String, body: String, onClick: () -> Unit) {
    // The whole entry is the target, not a button under it.
    //
    // This drew a full-width filled Button reading "Open" beneath every item,
    // six of them across the two help pages. By the vocabulary in Buttons.kt a
    // filled button means "writes the record, one per surface", so a help page
    // was carrying six primaries, each the same visual weight as "Log it", for
    // the act of reading a page. A list of things to read is a list, and a list
    // row is tapped.
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = title) { onClick() }
            .padding(top = Space.Entry, bottom = Space.Entry),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.Tight),
            )
        }
        GoChevron(Modifier.padding(start = Space.Block))
    }
    Rule()
}
