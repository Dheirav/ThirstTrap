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
import androidx.compose.material3.ExperimentalMaterial3Api
import dev.dheirav.thirsttrap.ui.AlmanacDialog
import dev.dheirav.thirsttrap.ui.DialogText
import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import dev.dheirav.thirsttrap.ui.OutlinedButton
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
    onOpenHowItWorks: () -> Unit,
    onOpenTroubleshoot: () -> Unit,
    onOpenDebug: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val storage by viewModel.storage.collectAsStateWithLifecycle()
    val cleanup by viewModel.cleanupMessage.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { ScreenTitle("Settings") }) }) { padding ->
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

            SettingRow(
                title = "Offer care notes for a new plant",
                subtitle = "Just after you add a plant, if there are notes on file for its " +
                    "species, the app offers them. On by default: the moment you have typed " +
                    "the species name is the moment they are worth reading, and nobody goes " +
                    "looking in a menu for something they do not know is there. It only ever " +
                    "asks once per plant, and \"Don't ask again\" in that prompt turns this off.",
            ) {
                Switch(
                    checked = settings.offerCareOnAdd,
                    onCheckedChange = viewModel::setOfferCareOnAdd,
                    modifier = Modifier.semantics {
                        contentDescription = "Offer care notes for a new plant"
                    },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader("Network")
            SettingRow(
                title = "Look up unknown plant names online",
                subtitle = "Off by default, and the only thing in the app that can send " +
                    "anything anywhere. On, the care screen can resolve a name it does not " +
                    "recognise and link the Wikipedia article - it sends the name you typed " +
                    "and nothing else. It never fetches care advice, and never runs on its own.",
            ) {
                Switch(
                    checked = settings.onlineSpeciesLookup,
                    onCheckedChange = viewModel::setOnlineSpeciesLookup,
                    modifier = Modifier.semantics {
                        contentDescription = "Look up unknown plant names online"
                    },
                )
            }
            Text(
                "Everything else - the catalogue, the predictions, the reminders, your whole " +
                    "diary - works with no network at all, and always will.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

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
            // "Remind me at a precise time" used to sit here. D30a left it open
            // with "either honour the setting or remove the toggle", and it could
            // not be honoured: nothing read useExactAlarms, the scheduler takes no
            // settings dependency, and neither manifest declares
            // SCHEDULE_EXACT_ALARM, so canScheduleExactAlarms() cannot return true
            // on API 31+. A switch that could not latch on, would have gated
            // nothing if it had, and sent the user to a system page this app cannot
            // appear on. The loose scheduling described above is the decision, D4,
            // and it is now the only thing the screen claims.


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
            SectionHeader("Extras")
            SettingRow(
                title = "Show the specialist tools",
                subtitle = "Pot stickers and the scanner, experiments, and logging room " +
                    "temperature by hand. None of them is useless and none of them is for " +
                    "everybody: stickers pay off at thirty pots and a printer, and " +
                    "experiments assume you want to run a controlled test on a houseplant.",
            ) {
                Switch(
                    checked = settings.advancedFeatures,
                    onCheckedChange = viewModel::setAdvancedFeatures,
                    modifier = Modifier.semantics {
                        contentDescription = "Show the specialist tools"
                    },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader("Help")
            // Two buttons, because there were two intentions behind one. The
            // single button read "Something is not working" and opened a list
            // of five pages explaining how the app works, so it promised
            // troubleshooting and delivered documentation.
            OutlinedButton(onClick = onOpenHowItWorks, modifier = Modifier.fillMaxWidth()) {
                Text("How the app works")
            }
            OutlinedButton(onClick = onOpenTroubleshoot, modifier = Modifier.fillMaxWidth()) {
                Text("Something is wrong")
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader("Your data")
            OutlinedButton(onClick = onOpenBackup, modifier = Modifier.fillMaxWidth()) {
                Text("Backup and restore")
            }
            // D33 made the user the backup system; this is the one fact that
            // system runs on. Quiet text, never a badge - the diary is theirs
            // to protect on their own schedule, and this just says when.
            Text(
                when (val at = settings.lastExportAtMillis) {
                    null -> "Never exported. The diary lives only on this phone."
                    else -> {
                        val days = ((System.currentTimeMillis() - at) / 86_400_000L).toInt()
                        when {
                            days <= 0 -> "Last export: today."
                            days == 1 -> "Last export: yesterday."
                            else -> "Last export: $days days ago. The diary lives only on this phone."
                        }
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )


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
