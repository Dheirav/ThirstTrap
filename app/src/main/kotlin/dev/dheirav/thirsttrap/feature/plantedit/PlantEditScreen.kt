package dev.dheirav.thirsttrap.feature.plantedit

import dev.dheirav.thirsttrap.ui.Disclosure
import dev.dheirav.thirsttrap.ui.FieldLabel
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
import dev.dheirav.thirsttrap.ui.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import dev.dheirav.thirsttrap.ui.TextButton
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
            // Says so, once, quietly. A value sitting in a field the user did
            // not type is otherwise indistinguishable from one they typed and
            // forgot, and the fix for that is attribution, not a badge.
            state.prefillFrom?.let { from ->
                Text(
                    "Filled ${joinNaturally(state.prefilled)} from the $from notes. " +
                        "Change any of them below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            OutlinedTextField(
                value = state.location,
                onValueChange = viewModel::onLocation,
                label = { Text("Location") },
                placeholder = { Text("desk, windowsill, balcony") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Folded when adding, open when editing. The form's own opening
            // line says only the name is needed and then presented nine more
            // fields as if it had not; this is the form agreeing with itself.
            // The summary on each header says what the section HOLDS. Never a
            // count and never how much is missing: a completeness meter is a
            // score, which this design refuses on purpose.
            Disclosure(
                "What it is",
                summary = listOfNotNull(
                    state.containerDesc.takeIf { it.isNotBlank() },
                    state.medium.takeIf { it != Medium.UNKNOWN }?.label,
                    state.source.takeIf { it != PlantSource.UNKNOWN }?.label,
                ).joinToString(" · ").ifBlank { null },
                initiallyOpen = !state.isNew,
            ) {
                OutlinedTextField(
                    value = state.containerDesc,
                    onValueChange = viewModel::onContainer,
                    label = { Text("Container") },
                    placeholder = { Text("6-inch terracotta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                FieldLabel("Growing medium")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Medium.entries.filter { it != Medium.UNKNOWN }.forEach { m ->
                        FilterChip(
                            selected = state.medium == m,
                            onClick = { viewModel.onMedium(m) },
                            label = { Text(m.label) },
                        )
                    }
                }

                FieldLabel("Where it came from")
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
            }

            Disclosure(
                "Care profile",
                summary = listOfNotNull(
                    state.lightNeeds.takeIf { it.isNotBlank() },
                    state.targetDryness.takeIf { it.isNotBlank() },
                ).joinToString(" · ").ifBlank { null },
                initiallyOpen = !state.isNew,
            ) {
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
            }

            Disclosure(
                "Weighing",
                summary = if (state.weightTracked) "on" else "off",
                initiallyOpen = !state.isNew,
            ) {
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
                    FieldLabel("How you weigh it")
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
            }

            if (state.isNew) {
                FieldLabel("When did you last water it?")
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
                        // Not when the form already filled them while the
                        // species was typed: the dialog would offer what is
                        // sitting in the fields behind it.
                        val already = state.prefillFrom != null
                        if (wasNew && hasNotes && offerCare && !already) {
                            justAdded = plantId
                        } else {
                            onDone()
                        }
                    }
                },
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (state.isNew) "Add plant" else "Save") }

            if (!state.isNew) {
                // Three identically drawn full-width targets stacked in a
                // column, two of them irreversible, differing only by their
                // words. That is the highest-confusion geometry there is: the
                // reader aims by position, and position is the one thing these
                // three do not distinguish. This codebase has already learned
                // that twice in its own comments, at PlantDetailScreen and
                // FertilizerScreen. Folded away, because none of it is part of
                // editing a plant you intend to keep.
                Disclosure("This plant is finished") {
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

/** "light, dryness and the depletion trigger" rather than a comma-spliced list. */
private fun joinNaturally(parts: List<String>): String = when (parts.size) {
    0 -> ""
    1 -> parts[0]
    else -> parts.dropLast(1).joinToString(", ") + " and " + parts.last()
}
