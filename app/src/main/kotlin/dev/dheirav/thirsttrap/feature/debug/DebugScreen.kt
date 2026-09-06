package dev.dheirav.thirsttrap.feature.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(onBack: () -> Unit, viewModel: DebugViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Use system colours")
                    Text(
                        "On, the phone's wallpaper theme wins. Off, you see the app's own " +
                            "muted green palette.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor,
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
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

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            FilledTonalButton(
                onClick = { viewModel.dumpWorkQueue(context) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Dump the work queue") }

            if (status.isNotBlank()) {
                Text(
                    status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
