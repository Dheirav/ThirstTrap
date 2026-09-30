package dev.dheirav.thirsttrap.feature.plantedit

import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.AppIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import dev.dheirav.thirsttrap.ui.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import dev.dheirav.thirsttrap.ui.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import dev.dheirav.thirsttrap.ui.AlmanacDialog
import dev.dheirav.thirsttrap.ui.DialogText
import dev.dheirav.thirsttrap.domain.Medium
import dev.dheirav.thirsttrap.domain.hasSpeciesCare
import dev.dheirav.thirsttrap.ui.Card
import dev.dheirav.thirsttrap.domain.WeighingMethod
import dev.dheirav.thirsttrap.domain.PlantSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlantEditScreen(
    onDone: () -> Unit,
    onPlantGone: () -> Unit,
    onMarkDied: (String) -> Unit,
    onOpenCare: (String) -> Unit,
    viewModel: PlantEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val offerCare by viewModel.offerCareOnAdd.collectAsStateWithLifecycle()
    // The plant that was just added, held only long enough to ask about it.
    var justAdded by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Add plant" else "Edit plant") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
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
            // The name is the only thing save is gated on, and seven labels
            // reading "(optional)" said that seven times without ever saying
            // which one was not. One asterisk and one line at the top says it
            // once, and the rest of the form stops looking like a form.
            if (state.isNew) {
                Text(
                    "Only the name is needed. Everything else can wait, or stay empty, " +
                        "because the app fills most of it in from what you log.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onName,
                label = { Text("Name *") },
                placeholder = { Text("marbled pothos") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.species,
                onValueChange = viewModel::onSpecies,
                label = { Text("Species") },
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
                label = { Text("Container") },
                placeholder = { Text("6-inch terracotta") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.checkIntervalDays,
                onValueChange = viewModel::onCheckInterval,
                label = { Text("Remind me to check every N days") },
                placeholder = { Text("leave empty and it works this out from your log") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.defaultWaterMl,
                onValueChange = viewModel::onDefaultWater,
                label = { Text("Usual amount of water (ml)") },
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
                label = { Text("How dry before watering") },
                placeholder = { Text("top 2-3 cm dry, nearly weightless, keep damp") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.lightNeeds,
                onValueChange = viewModel::onLightNeeds,
                label = { Text("Light") },
                placeholder = { Text("bright indirect, shade") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.fertilizerCadenceDays,
                onValueChange = viewModel::onFertilizerCadence,
                label = { Text("How often to feed, in days") },
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
                        label = { Text(m.label) },
                    )
                }
            }

            // Only where the medium has not already settled it: a cutting in a
            // jar of water weighs what the jar weighs, and that is not a choice.
            if (state.medium != Medium.WATER) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Weigh this pot", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Turn this off for a pot where weight says nothing, like a closed " +
                                "terrarium that recycles its own water. It leaves the weighing " +
                                "round and stops being asked about.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Switch(
                        checked = state.weightTracked,
                        onCheckedChange = viewModel::onWeightTracked,
                    )
                }
            }

            // The per-plant depletion trigger, docs/WATERING-MODEL.md §2. This
            // is its only control since the calibration dialog went (D28) - a
            // field the model reads but no screen can set is the app lying
            // about being configurable.
            if (state.medium != Medium.WATER && state.weightTracked) {
                Text(
                    "Waters at ${state.depletionTriggerPct}% depleted",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    "How much of the pot's water range is used up before this plant " +
                        "wants watering. Around 30% for moisture-lovers like ferns and " +
                        "fittonia, 50% for most foliage plants, 70% or more for " +
                        "succulents and other drought-lovers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = state.depletionTriggerPct.toFloat(),
                    onValueChange = { viewModel.onDepletionTrigger(it.toInt()) },
                    valueRange = 20f..80f,
                    steps = 11,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Only once the pot is actually being weighed. The method and the
            // scale's step are what make one reading comparable to the next.
            if (state.medium != Medium.WATER && state.weightTracked) {
                Text("How you weigh it", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeighingMethod.entries.forEach { m ->
                        FilterChip(
                            selected = state.weighingMethod == m,
                            onClick = { viewModel.onWeighingMethod(m) },
                            label = { Text(m.label) },
                        )
                    }
                }
                Text(
                    state.weighingMethod.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.weighingStep,
                    onValueChange = viewModel::onWeighingStep,
                    label = { Text("Smallest step your scale shows") },
                    placeholder = { Text("1 for a kitchen scale, 100 for a bathroom one") },
                    suffix = { Text("g") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "A daily loss smaller than one step is rounding, not drying, so this " +
                        "decides when the app stays quiet rather than guessing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Said before saving, not discovered afterwards. Same reasoning
                // as the repot warning on the log form.
                if (state.weighingChanged) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "This clears the weight setup",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                "Measuring it a different way changes every reading by a " +
                                    "constant, so the full and dry marks describe a " +
                                    "measurement that no longer exists. Weigh it once after " +
                                    "watering and they set themselves again.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            Text("Where it came from", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlantSource.entries.filter { it != PlantSource.UNKNOWN }.forEach { s ->
                    FilterChip(
                        selected = state.source == s,
                        onClick = { viewModel.onSource(s) },
                        label = {
                            Column {
                                Text(s.label)
                                s.hint?.let {
                                    Text(it, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        },
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
                onClick = {
                    viewModel.save { plantId, wasNew ->
                        // Only for a new plant, only when the catalogue has
                        // something to show, and only while the user has not
                        // said stop. Anything else and saving just closes the
                        // form, which is what it has always done.
                        val hasNotes = hasSpeciesCare(state.species) || hasSpeciesCare(state.name)
                        if (wasNew && hasNotes && offerCare) justAdded = plantId else onDone()
                    }
                },
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (state.isNew) "Add plant" else "Save") }

            if (!state.isNew) {
                state.id?.let { id ->
                    TextButton(
                        onClick = { onMarkDied(id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("It died") }
                }

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

    justAdded?.let { plantId ->
        val what = state.species.trim().ifEmpty { state.name.trim() }
        AlmanacDialog(
            title = what,
            onDismissRequest = { justAdded = null; onDone() },
            body = {
                DialogText(
                    "There are care notes on file for this one: how much light it wants, " +
                        "how dry to let it get, and what usually goes wrong.",
                )
                // The opt-out reads as a line of the page rather than a third
                // button competing with the two that matter.
                TextButton(
                    onClick = {
                        // Turning it off should also not show this one, or the
                        // switch would look like it had not worked.
                        viewModel.stopOfferingCare()
                        justAdded = null
                        onDone()
                    },
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        "Don't offer this again",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            },
            dismiss = {
                TextButton(onClick = { justAdded = null; onDone() }) { Text("Not now") }
            },
            confirm = {
                TextButton(onClick = { justAdded = null; onOpenCare(plantId) }) {
                    Text("Read them")
                }
            },
        )
    }

    if (confirmDelete) {
        AlmanacDialog(
            title = "Delete ${state.name}?",
            onDismissRequest = { confirmDelete = false },
            // Archiving is the reversible option and is said first, because a
            // plant's history is the thing that is expensive to lose.
            body = {
                DialogText(
                    "This removes the plant and every event logged against it. " +
                        "Archiving keeps the history instead.",
                )
            },
            dismiss = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            confirm = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete(onPlantGone) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}
