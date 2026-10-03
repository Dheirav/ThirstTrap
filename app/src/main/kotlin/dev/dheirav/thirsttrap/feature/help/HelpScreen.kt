package dev.dheirav.thirsttrap.feature.help

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.Button
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.ScreenTitle

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
    onOpenIntro: () -> Unit,
    onOpenScaleHelp: () -> Unit,
    onOpenWayfinding: () -> Unit,
    onOpenBackupHelp: () -> Unit,
) {
    HelpPage("How the app works", onBack) {
        // Reading order rather than alphabetical: why it exists, the one
        // mechanic it rests on, where things are, and then the thing you only
        // care about once you have data worth keeping.
        Entry(
            title = "What this app is for",
            body = "The first-run page again: why it weighs pots, and why there are no " +
                "streaks or counts of what you missed.",
            onClick = onOpenIntro,
        )
        Entry(
            title = "Weighing a pot",
            body = "What to weigh, when, and what to do about a pot too heavy to lift.",
            onClick = onOpenScaleHelp,
        )
        Entry(
            title = "Finding your way around",
            body = "What is on each tab, what is behind the two dots menus, and the only " +
                "two gestures the app cannot tell you about itself.",
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
    onOpenWhyNoDate: () -> Unit,
) {
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
            onClick = onOpenWhyNoDate,
        )
        // Signposting rather than a route, because the answer is genuinely
        // somewhere else and somebody who remembers it being here needs telling
        // once. D50 moved it onto the plant it is about.
        Text(
            "A plant that does not look right is handled on the plant itself: open it, " +
                "then \"Something looks wrong\" in its menu. It needs to know which " +
                "plant you mean, which is why it is not here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The frame both share, so they cannot drift apart. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { ScreenTitle(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) { content() }
    }
}

@Composable
private fun Entry(title: String, body: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text("Open") }
        Rule(Modifier.padding(top = 18.dp))
    }
}
