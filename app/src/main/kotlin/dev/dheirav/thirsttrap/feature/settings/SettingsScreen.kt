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
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.AlmanacDialog
import dev.dheirav.thirsttrap.ui.DialogText
import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.SectionHead
import dev.dheirav.thirsttrap.ui.FilterChip
import androidx.compose.material3.MaterialTheme
import dev.dheirav.thirsttrap.ui.OutlinedButton
import androidx.compose.material3.Scaffold
import dev.dheirav.thirsttrap.ui.Switch
import androidx.compose.material3.Text
import dev.dheirav.thirsttrap.ui.TextButton
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
import dev.dheirav.thirsttrap.ui.Space

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
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Space.Block),
            verticalArrangement = Arrangement.spacedBy(Space.Line),
        ) {
            SectionHead("New plants")
            SettingRow(
                title = "Offer care notes for a new plant",
                subtitle = "Offers the notes on file for a species just after you add a " +
                    "plant. Once per plant, and \"Don't ask again\" there turns this off.",
            ) {
                Switch(
                    checked = settings.offerCareOnAdd,
                    onCheckedChange = viewModel::setOfferCareOnAdd,
                    modifier = Modifier.semantics {
                        contentDescription = "Offer care notes for a new plant"
                    },
                )
            }

            SectionHead("Network")
            SettingRow(
                title = "Look up unknown plant names online",
                // Cut least, on purpose. This is the only switch that lets
                // anything leave the phone, so every fact stays: what sends,
                // what it sends, what for, and that it is never automatic.
                subtitle = "The only thing here that sends anything out: the species name, " +
                    "nothing else, to link its Wikipedia article. Never runs on its own.",
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionHead("Reminders")
            Text(
                "When to check for plants that need a look",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth().padding(vertical = Space.Tight),
                horizontalArrangement = Arrangement.spacedBy(Space.Line),
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
                "Reminders are not timed to the minute, so a 9:00 one may arrive at " +
                    "9:15. That keeps battery use tiny.",
                style = MaterialTheme.typography.bodyMedium,
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


            SectionHead("Watering")
            Text(
                "How dry a new plant should get before watering. Succulents more, " +
                    "ferns less. Each plant can have its own.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth().padding(vertical = Space.Tight),
                horizontalArrangement = Arrangement.spacedBy(Space.Line),
            ) {
                listOf(0.3 to "ferns", 0.5 to "most", 0.75 to "succulents").forEach { (v, label) ->
                    FilterChip(
                        selected = kotlin.math.abs(settings.defaultDepletionTrigger - v) < 0.01,
                        onClick = { viewModel.setDefaultTrigger(v) },
                        label = { Text("${(v * 100).toInt()}% · $label") },
                    )
                }
            }

            SectionHead("Extras")
            SettingRow(
                title = "Show the specialist tools",
                subtitle = "Pot stickers and the scanner, experiments, and logging room " +
                    "temperature by hand.",
            ) {
                Switch(
                    checked = settings.advancedFeatures,
                    onCheckedChange = viewModel::setAdvancedFeatures,
                    modifier = Modifier.semantics {
                        contentDescription = "Show the specialist tools"
                    },
                )
            }

            SectionHead("Help")
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

            SectionHead("Your data")
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.Tight),
            )


            storage?.let { s ->
                Text(
                    "${s.photoCount} ${if (s.photoCount == 1) "photo" else "photos"} · " +
                        "${formatBytes(s.photoBytes)} of photos · " +
                        "${formatBytes(s.databaseBytes)} of entries",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Space.Line),
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
                Rule()
                TextButton(onClick = onOpenDebug) { Text("Debug tools") }
            }

            // The name only. The version code is the git commit count, which
            // is the right way to derive it and the wrong thing to put in front
            // of somebody keeping a plant diary: it reads as a build number
            // from a machine rather than anything about their plants. Every
            // backup file still records the version, so a build is still
            // identifiable from the data when it matters.
            Text(
                "ThirstTrap ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.Section),
            )
        }
    }
}


@Composable
private fun SettingRow(title: String, subtitle: String, control: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(vertical = Space.Tight)
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
                style = MaterialTheme.typography.bodyMedium,
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
