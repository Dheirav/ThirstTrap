package dev.dheirav.thirsttrap.feature.experiments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import dev.dheirav.thirsttrap.ui.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import dev.dheirav.thirsttrap.ui.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import dev.dheirav.thirsttrap.ui.AlmanacDialog
import dev.dheirav.thirsttrap.ui.DialogText
import androidx.compose.material3.Text
import dev.dheirav.thirsttrap.ui.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.domain.ExperimentWithSubjects
import dev.dheirav.thirsttrap.domain.experimentDayNumber
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.Button
import dev.dheirav.thirsttrap.ui.ScreenTitle

/**
 * F11. The list is split running/concluded rather than sorted together,
 * because the two are read for different reasons: running ones to act on,
 * concluded ones to look up what was learned.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentsScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: ExperimentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Experiments") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Button(
                    onClick = { creating = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) { Text("Start an experiment") }
            }

            if (state.loaded && state.running.isEmpty() && state.concluded.isEmpty()) {
                item {
                    Text(
                        "One variable, some subjects, a dated conclusion. The flax " +
                            "germination test - banana water against plain, thirty seeds, " +
                            "verdict on day five - is the shape this is for.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            if (state.running.isNotEmpty()) {
                item { SectionLabel("Running") }
                items(state.running, key = { it.experiment.id }) {
                    ExperimentCard(it, onOpen)
                }
            }
            if (state.concluded.isNotEmpty()) {
                item { SectionLabel("Concluded") }
                items(state.concluded, key = { it.experiment.id }) {
                    ExperimentCard(it, onOpen)
                }
            }
        }
    }

    if (creating) {
        var name by remember { mutableStateOf("") }
        var variable by remember { mutableStateOf("") }
        AlmanacDialog(
            title = "New experiment",
            onDismissRequest = { creating = false },
            body = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name *") },
                        placeholder = { Text("flax germination") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = variable,
                        onValueChange = { variable = it },
                        label = { Text("What varies *") },
                        placeholder = { Text("banana water 1:10 vs plain") },
                        singleLine = true,
                    )
                }
            },
            dismiss = { TextButton(onClick = { creating = false }) { Text("Cancel") } },
            confirm = {
                TextButton(
                    enabled = name.isNotBlank() && variable.isNotBlank(),
                    onClick = {
                        viewModel.create(name, variable) { id ->
                            creating = false
                            onOpen(id)
                        }
                    },
                ) { Text("Start") }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun ExperimentCard(e: ExperimentWithSubjects, onOpen: (String) -> Unit) {
    val exp = e.experiment
    Card(Modifier.fillMaxWidth().clickable { onOpen(exp.id) }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(exp.name, style = MaterialTheme.typography.titleMedium)
            Text(exp.variable, style = MaterialTheme.typography.bodySmall)
            val status = if (exp.isConcluded) {
                "Concluded"
            } else {
                "Day ${experimentDayNumber(exp.startedAtMillis, exp.tzOffsetMinutes, System.currentTimeMillis())}"
            }
            Text(
                "$status - ${e.subjects.size} subject${if (e.subjects.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
