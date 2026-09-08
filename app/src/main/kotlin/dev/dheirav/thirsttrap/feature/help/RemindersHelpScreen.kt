package dev.dheirav.thirsttrap.feature.help

import dev.dheirav.thirsttrap.ui.AppIcons
import android.content.ComponentName
import android.content.Context
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
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
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
                    "Autostart is NOT on the app's own info page - it is a separate list:\n" +
                        "Settings → Apps → Permissions → Autostart → ThirstTrap.\n\n" +
                        "It is off by default on Xiaomi phones, and without it the daily " +
                        "check never runs. The button below goes straight there.",
                )
                FilledTonalButton(
                    onClick = { openXiaomiAutostart(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Open the Autostart list") }
                Spacer(Modifier.height(20.dp))

                Section(
                    "2. Turn OFF \"Pause app activity if unused\"",
                    "On the app's info page. Android turns this on by default; it stops " +
                        "notifications and revokes permissions when an app has not been " +
                        "opened for a while - which is exactly what a reminder app must not " +
                        "have done to it.",
                )
                Section(
                    "3. Remove the battery restriction",
                    "App info → Battery saver → No restrictions.",
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
            ) { Text("Open this app's info page") }

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

/**
 * Deep-links to MIUI's Autostart list.
 *
 * The standard ACTION_APPLICATION_DETAILS_SETTINGS page does NOT carry the
 * Autostart toggle on HyperOS - confirmed on a Note 15 Pro - so sending the
 * user there and telling them to find it is sending them somewhere it is not.
 * Falls back to the app info page if the component is missing on some build.
 */
private fun openXiaomiAutostart(context: Context) {
    val candidates = listOf(
        Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ),
        ),
        Intent("miui.intent.action.OP_AUTO_START")
            .addCategory(Intent.CATEGORY_DEFAULT),
    )
    for (intent in candidates) {
        if (runCatching { context.startActivity(intent) }.isSuccess) return
    }
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            },
        )
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
