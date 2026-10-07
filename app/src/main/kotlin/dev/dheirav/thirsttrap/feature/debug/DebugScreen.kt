package dev.dheirav.thirsttrap.feature.debug

import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.AppIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import dev.dheirav.thirsttrap.ui.FilledTonalButton
import androidx.compose.material3.Icon
import dev.dheirav.thirsttrap.ui.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.ui.Space

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(onBack: () -> Unit, viewModel: DebugViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle("Debug") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Space.Block),
            verticalArrangement = Arrangement.spacedBy(Space.Entry),
        ) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Text("Reminders", style = MaterialTheme.typography.titleMedium)

            FilledTonalButton(
                onClick = { viewModel.fireReminderNow(context) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Fire a reminder now") }

            FilledTonalButton(
                onClick = { viewModel.makeAllDueNow() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Make every reminder due now") }

            FilledTonalButton(
                onClick = { viewModel.runSweepNow(context) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Run the real WorkManager sweep") }

            FilledTonalButton(
                onClick = { viewModel.resetReminders() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Reset all reminders to +7 days") }

            Rule()

            FilledTonalButton(
                onClick = { viewModel.exportToCache() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Export to cache (no picker)") }

            FilledTonalButton(
                onClick = { viewModel.importFromCache() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Re-import that export (idempotency check)") }

            FilledTonalButton(
                onClick = { viewModel.dumpWorkQueue(context) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Dump the work queue") }

            if (status.isNotBlank()) {
                Text(
                    status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Space.Line),
                )
            }
        }
    }
}
