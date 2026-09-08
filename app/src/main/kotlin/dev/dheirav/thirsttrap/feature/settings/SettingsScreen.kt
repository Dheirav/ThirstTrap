package dev.dheirav.thirsttrap.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenBackup: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenDebug: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val storage by viewModel.storage.collectAsStateWithLifecycle()
    val cleanup by viewModel.cleanupMessage.collectAsStateWithLifecycle()
    val needsExact by viewModel.needsExactPermission.collectAsStateWithLifecycle()

    if (needsExact) {
        AlertDialog(
            onDismissRequest = viewModel::dismissExactPermissionPrompt,
            title = { Text("Android has to allow this") },
            text = {
                Text(
                    "Precise reminders need the \"Alarms & reminders\" permission. Android " +
                        "withholds it by default, and can take it back later - if that " +
                        "happens the app quietly falls back to loose scheduling rather than " +
                        "going silent.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::openExactAlarmSettings) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissExactPermissionPrompt) { Text("Not now") }
            },
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionHeader("Appearance")
            SettingRow(
                title = "Use system colours",
                subtitle = "Off shows the app's own green. On follows your wallpaper.",
            ) {
                Switch(
                    checked = settings.dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor,
                    modifier = Modifier.semantics { contentDescription = "Use system colours" },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader("Reminders")
            Text(
                "When to check for plants that need a look",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(7, 9, 12, 18).forEach { hour ->
                    FilterChip(
                        selected = settings.reminderHour == hour,
                        onClick = { viewModel.setReminderHour(hour) },
                        label = { Text("%02d:00".format(hour)) },
                    )
                }
            }
            Text(
                "Reminders are scheduled loosely rather than to the minute, so a 9:00 " +
                    "reminder may arrive at 9:15. That keeps battery use negligible.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingRow(
                title = "Remind me at a precise time",
                subtitle = if (viewModel.canScheduleExact()) {
                    "Off keeps reminders loose and battery-cheap. On makes them land on " +
                        "the minute."
                } else {
                    "Needs the \"Alarms & reminders\" permission, which Android withholds " +
                        "by default. Turning this on will ask for it."
                },
            ) {
                Switch(
                    checked = settings.useExactAlarms,
                    onCheckedChange = viewModel::setUseExactAlarms,
                    modifier = Modifier.semantics { contentDescription = "Remind me at a precise time" },
                )
            }

            TextButton(onClick = onOpenHelp) { Text("Reminders not arriving?") }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader("Watering")
            Text(
                "How dry a new plant is allowed to get before it is worth watering. " +
                    "Succulents want more, ferns want less. Each plant can override this.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(0.3 to "ferns", 0.5 to "most", 0.75 to "succulents").forEach { (v, label) ->
                    FilterChip(
                        selected = kotlin.math.abs(settings.defaultDepletionTrigger - v) < 0.01,
                        onClick = { viewModel.setDefaultTrigger(v) },
                        label = { Text("${(v * 100).toInt()}% · $label") },
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader("Your data")
            OutlinedButton(onClick = onOpenBackup, modifier = Modifier.fillMaxWidth()) {
                Text("Backup and restore")
            }

            storage?.let { s ->
                Text(
                    "${s.photoCount} ${if (s.photoCount == 1) "photo" else "photos"} · " +
                        "${formatBytes(s.photoBytes)} of photos · " +
                        "${formatBytes(s.databaseBytes)} of entries",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (s.orphanFiles > 0 || s.orphanRows > 0) {
                    Text(
                        buildString {
                            if (s.orphanFiles > 0) {
                                append("${s.orphanFiles} stray ${if (s.orphanFiles == 1) "file" else "files"} " +
                                    "(${formatBytes(s.orphanFileBytes)})")
                            }
                            if (s.orphanFiles > 0 && s.orphanRows > 0) append(" and ")
                            if (s.orphanRows > 0) {
                                append("${s.orphanRows} ${if (s.orphanRows == 1) "photo" else "photos"} " +
                                    "whose file has gone")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                } else {
                    Text(
                        "Nothing stray.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = viewModel::cleanUp, modifier = Modifier.fillMaxWidth()) {
                    Text("Clean up storage")
                }
            }

            cleanup?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = viewModel::dismissCleanupMessage) { Text("OK") }
            }

            if (BuildConfig.DEBUG) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                TextButton(onClick = onOpenDebug) { Text("Debug tools") }
            }

            Text(
                "ThirstTrap ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun SettingRow(title: String, subtitle: String, control: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(vertical = 4.dp)
            // One node for the whole row carrying title AND the sentence that
            // explains it. Clearing the column's semantics stopped the double
            // reading but also deleted the explanation.
            .semantics(mergeDescendants = true) {
                contentDescription = "$title. $subtitle"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).clearAndSetSemantics { }) {
            Text(title)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        control()
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%d KB".format(bytes / 1_000)
    else -> "$bytes B"
}
