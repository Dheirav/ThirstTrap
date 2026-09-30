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
 * One door for "something is not working".
 *
 * There were three, loose in Settings between the reminder hour and the room
 * section: the reminder troubleshooter, the plant troubleshooter and the scale
 * notes. Three entry points for one intention is how a settings screen stops
 * being scannable. docs/NAVIGATION.md.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onOpenReminderHelp: () -> Unit,
    onOpenDiagnose: () -> Unit,
    onOpenScaleHelp: () -> Unit,
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
            Entry(
                title = "A plant does not look right",
                body = "Walks through what the log and the weight readings can and cannot " +
                    "tell you, and what to rule out first.",
                onClick = onOpenDiagnose,
            )
            Entry(
                title = "Reminders are not arriving",
                body = "Almost always battery optimisation or a notification channel. This " +
                    "checks the specific settings Android hides.",
                onClick = onOpenReminderHelp,
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
