package dev.dheirav.thirsttrap.feature.experiments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import dev.dheirav.thirsttrap.ui.AlmanacDialog
import dev.dheirav.thirsttrap.ui.DialogText
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.domain.experimentDayNumber
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.Button
import dev.dheirav.thirsttrap.ui.ScreenTitle

/**
 * One experiment: the arms, the subjects in each, the conclusion.
 *
 * The observations themselves stay on the subject plants - this page is the
 * index card on the front of the folder, not the folder. Tapping a subject
 * goes to its timeline, which is where the daily photos and notes already are.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExperimentDetailScreen(
    onBack: () -> Unit,
    onOpenPlant: (String) -> Unit,
    viewModel: ExperimentDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exp = state.experiment?.experiment
    var confirmDelete by remember { mutableStateOf(false) }
    var conclusion by remember { mutableStateOf("") }
    var addingFor by remember { mutableStateOf<String?>(null) } // candidate plant id

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exp?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (exp == null) {
                if (state.loaded) Text("This experiment no longer exists.")
                return@Column
            }

            Text(exp.variable, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (exp.isConcluded) {
                    "Concluded"
                } else {
                    "Day ${experimentDayNumber(exp.startedAtMillis, exp.tzOffsetMinutes, System.currentTimeMillis())}"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()

            Text("Subjects", style = MaterialTheme.typography.labelLarge)
            val byArm = state.experiment!!.subjects.groupBy { it.label }
            if (byArm.isEmpty()) {
                Text(
                    "No subjects yet. Each subject is a plant, and its arm says what " +
                        "it gets - \"banana water\", \"control\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            byArm.forEach { (arm, subjects) ->
                Text(arm, style = MaterialTheme.typography.titleSmall)
                subjects.forEach { s ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPlant(s.plantId) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            state.plantsById[s.plantId]?.name ?: "(deleted plant)",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (!exp.isConcluded) {
                            TextButton(onClick = { viewModel.removeSubject(s.plantId) }) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }

            if (!exp.isConcluded && state.candidates.isNotEmpty()) {
                Text("Add a subject", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.candidates.forEach { p ->
                        FilterChip(
                            selected = false,
                            onClick = { addingFor = p.id },
                            label = { Text(p.name) },
                        )
                    }
                }
            }

            HorizontalDivider()

            if (exp.isConcluded) {
                Text("Conclusion", style = MaterialTheme.typography.labelLarge)
                Text(exp.conclusion.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Conclude", style = MaterialTheme.typography.labelLarge)
                Text(
                    "One way, on purpose: a conclusion that can be rewritten later is a " +
                        "lab notebook in pencil. Getting it wrong is what the next " +
                        "experiment is for.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = conclusion,
                    onValueChange = { conclusion = it },
                    label = { Text("What was learned") },
                    placeholder = { Text("banana water matched or beat plain; no toxicity") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { viewModel.conclude(conclusion) {} },
                    enabled = conclusion.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Conclude the experiment") }
            }

            TextButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        }
    }

    addingFor?.let { plantId ->
        var label by remember { mutableStateOf("") }
        AlmanacDialog(
            title = "Which arm?",
            onDismissRequest = { addingFor = null },
            body = {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Arm") },
                    placeholder = { Text("banana water / control") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            dismiss = { TextButton(onClick = { addingFor = null }) { Text("Cancel") } },
            confirm = {
                TextButton(
                    enabled = label.isNotBlank(),
                    onClick = {
                        viewModel.addSubject(plantId, label)
                        addingFor = null
                    },
                ) { Text("Add") }
            },
        )
    }

    if (confirmDelete) {
        AlmanacDialog(
            title = "Delete this experiment?",
            onDismissRequest = { confirmDelete = false },
            body = {
                DialogText(
                    "The subjects are plants and stay exactly as they are. Only the " +
                        "experiment and its arm labels go.",
                )
            },
            dismiss = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            confirm = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete(onBack) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}
