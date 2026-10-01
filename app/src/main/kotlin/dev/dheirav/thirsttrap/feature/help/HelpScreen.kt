package dev.dheirav.thirsttrap.feature.help

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
 * Documentation, and only documentation.
 *
 * It began as one door for three things loose in Settings: the reminder
 * troubleshooter, the plant troubleshooter and the scale notes. Collecting them
 * was right. What was wrong is that one of the three was not documentation at
 * all: "A plant does not look right" is the diagnosis tree, a thing somebody
 * reaches for at the moment they are worried about a plant, and it sat four
 * taps deep behind Settings and a word meaning "I am confused". It is on the
 * plant now.
 *
 * So the rule for this screen: if an entry *does* something rather than
 * explaining something, it does not belong here. docs/NAVIGATION.md.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onOpenReminderHelp: () -> Unit,
    onOpenScaleHelp: () -> Unit,
    onOpenIntro: () -> Unit,
    onOpenWayfinding: () -> Unit,
) {
    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { ScreenTitle("Help") },
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
        ) {
            // First, and the only entry here that is not about something being
            // broken. The screen is called Help because three troubleshooters
            // needed one door; "where is the thing I want" is a fair question
            // to arrive with too, and it has nowhere else to go.
            Entry(
                title = "Finding your way around",
                body = "What is on each tab, what is behind the two dots menus, and the " +
                    "only two gestures the app cannot tell you about itself.",
                onClick = onOpenWayfinding,
            )
            Entry(
                title = "Reminders are not arriving",
                body = "Almost always battery optimisation or a notification channel. This " +
                    "checks the specific settings Android hides.",
                onClick = onOpenReminderHelp,
            )
            Entry(
                title = "What this app is for",
                body = "The first-run page again, including what to do about a pot too " +
                    "heavy to lift.",
                onClick = onOpenIntro,
            )
            Entry(
                title = "Weighing a pot",
                body = "What to weigh, when, and what to do about a pot too heavy to lift. " +
                    "Also why the app sometimes declines to predict.",
                onClick = onOpenScaleHelp,
            )
        }
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
