package dev.dheirav.thirsttrap.feature.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Export and import. Requirements item 10, and F10.6.
 *
 * The destination is chosen through the Storage Access Framework, so the
 * backup lands wherever the user keeps things - their own Drive, an SD card,
 * anywhere - and the app needs no storage permission to put it there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(onBack: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val status by viewModel.status.collectAsStateWithLifecycle()

    val createDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let(viewModel::export) }

    val openDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::import) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup") },
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
            Text(
                "Everything - plants, every entry, every photo - into one zip you keep. " +
                    "Nothing leaves this phone unless you put it somewhere.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                onClick = { createDoc.launch(viewModel.suggestedFileName()) },
                enabled = status !is BackupStatus.Working,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Export a backup") }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text("Restore", style = MaterialTheme.typography.titleMedium)
            Text(
                "Importing merges a backup into what is already here. Entries are matched " +
                    "by their id, so importing the same file twice changes nothing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { openDoc.launch(arrayOf("application/zip", "application/octet-stream")) },
                enabled = status !is BackupStatus.Working,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Import a backup") }

            Spacer(Modifier.height(8.dp))

            val announce = Modifier.semantics { liveRegion = LiveRegionMode.Polite }

            when (val s = status) {
                BackupStatus.Idle -> Unit
                BackupStatus.Working -> CircularProgressIndicator()

                is BackupStatus.Exported -> Text(
                    modifier = announce,
                    text = "Backup written, including ${s.photoCount} " +
                        if (s.photoCount == 1) "photo." else "photos.",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )

                is BackupStatus.Imported -> Column {
                    Text(
                        "Imported ${s.result.plants} plants, ${s.result.events} entries, " +
                            "${s.result.photoFiles} photo files.",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                    s.result.warnings.forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary)
                    }
                }

                // Specific reason and a retry. Swallowing an export failure
                // would be the worst possible silence in this app.
                is BackupStatus.Failed -> Column {
                    Text(
                        modifier = announce,
                        text = "${s.what} failed: ${s.reason}",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                    )
                    OutlinedButton(onClick = viewModel::clearStatus) { Text("Try again") }
                }
            }
        }
    }
}
