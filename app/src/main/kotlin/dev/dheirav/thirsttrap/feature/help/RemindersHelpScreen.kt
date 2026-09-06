package dev.dheirav.thirsttrap.feature.help

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Feature X7. An M1 deliverable rather than later polish, because the target
 * phone is a Redmi on HyperOS: Autostart is off by default and the system will
 * kill scheduled work whichever way the app schedules it. Without this screen,
 * reminders simply appear broken. See handover D6.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersHelpScreen(
    onBack: () -> Unit,
    viewModel: RemindersHelpViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val manufacturer = remember { Build.MANUFACTURER.lowercase() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders not arriving?") },
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
        ) {
            Text(
                "Android lets phone makers stop background apps, and this app cannot " +
                    "override that. Two settings usually fix it.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(24.dp))

            if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi") ||
                manufacturer.contains("poco")
            ) {
                Section(
                    "1. Turn on Autostart",
                    "Settings → Apps → Manage apps → ThirstTrap → Autostart.\n\n" +
                        "On Xiaomi phones this is off by default, and without it the daily " +
                        "check never runs.",
                )
                Section(
                    "2. Remove the battery restriction",
                    "Settings → Apps → Manage apps → ThirstTrap → Battery saver → " +
                        "No restrictions.",
                )
            } else {
                Section(
                    "1. Allow background activity",
                    "Find ThirstTrap in your phone's app settings and allow it to run in " +
                        "the background, or exclude it from battery optimisation.",
                )
            }

            Section(
                "Then test it",
                "Use the button below. A reminder should arrive within a few seconds. " +
                    "If it does, background work is allowed.",
            )

            FilledTonalButton(
                onClick = { viewModel.fireTestReminder(context) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Send a test reminder now") }

            Spacer(Modifier.height(16.dp))

            FilledTonalButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Open this app's settings") }

            Spacer(Modifier.height(24.dp))
            Text(
                "One honest caveat: reminders are scheduled loosely rather than to the " +
                    "exact minute. A 9am reminder may arrive at 9:15. That keeps battery " +
                    "use negligible and avoids a permission Android grants sparingly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text(
        body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
    )
}
