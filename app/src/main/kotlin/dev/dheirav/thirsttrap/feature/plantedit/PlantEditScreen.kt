package dev.dheirav.thirsttrap.feature.plantedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.domain.Medium
import dev.dheirav.thirsttrap.domain.PlantSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlantEditScreen(
    onDone: () -> Unit,
    onPlantGone: () -> Unit,
    viewModel: PlantEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Add plant" else "Edit plant") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onName,
                label = { Text("Name") },
                placeholder = { Text("marbled pothos") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.species,
                onValueChange = viewModel::onSpecies,
                label = { Text("Species (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.location,
                onValueChange = viewModel::onLocation,
                label = { Text("Location") },
                placeholder = { Text("desk, windowsill, balcony") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.containerDesc,
                onValueChange = viewModel::onContainer,
                label = { Text("Container (optional)") },
                placeholder = { Text("6-inch terracotta") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.checkIntervalDays,
                onValueChange = viewModel::onCheckInterval,
                label = { Text("Remind me to check every N days (optional)") },
                placeholder = { Text("leave empty and it works this out from your log") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.defaultWaterMl,
                onValueChange = viewModel::onDefaultWater,
                label = { Text("Usual amount of water (ml, optional)") },
                placeholder = { Text("75") },
                supportingText = {
                    Text("Watering logs this by default, so you never retype it.")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.targetDryness,
                onValueChange = viewModel::onTargetDryness,
                label = { Text("How dry before watering (optional)") },
                placeholder = { Text("top 2-3 cm dry, nearly weightless, keep damp") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.lightNeeds,
                onValueChange = viewModel::onLightNeeds,
                label = { Text("Light (optional)") },
                placeholder = { Text("bright indirect, shade") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.fertilizerCadenceDays,
                onValueChange = viewModel::onFertilizerCadence,
                label = { Text("How often to feed, in days (optional)") },
                placeholder = { Text("30") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Growing medium", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Medium.entries.filter { it != Medium.UNKNOWN }.forEach { m ->
                    FilterChip(
                        selected = state.medium == m,
                        onClick = { viewModel.onMedium(m) },
                        label = { Text(m.name.lowercase().replace('_', ' ')) },
                    )
                }
            }

            Text("Where it came from", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlantSource.entries.filter { it != PlantSource.UNKNOWN }.forEach { s ->
                    FilterChip(
                        selected = state.source == s,
                        onClick = { viewModel.onSource(s) },
                        label = { Text(s.name.lowercase()) },
                    )
                }
            }

            if (state.isNew) {
                Text("When did you last water it?", style = MaterialTheme.typography.labelLarge)
                Text(
                    "A plant you add today already has a history. This starts its first " +
                        "reminder from the right day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LastWatered.entries.forEach { w ->
                        FilterChip(
                            selected = state.lastWatered == w,
                            onClick = { viewModel.onLastWatered(w) },
                            label = { Text(w.label) },
                        )
                    }
                }
            }

            Button(
                onClick = { viewModel.save(onDone) },
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (state.isNew) "Add plant" else "Save") }

            if (!state.isNew) {
                TextButton(
                    onClick = { viewModel.archive(onPlantGone) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Archive (keeps its history)") }

                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete permanently", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${state.name}?") },
            // Archiving is the reversible option and is offered first, because
            // a plant's history is the thing that is expensive to lose.
            text = {
                Text(
                    "This removes the plant and every event logged against it. " +
                        "Archiving keeps the history instead.",
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete(onPlantGone) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
