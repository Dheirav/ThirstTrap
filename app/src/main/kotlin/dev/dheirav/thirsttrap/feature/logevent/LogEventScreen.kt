package dev.dheirav.thirsttrap.feature.logevent

import dev.dheirav.thirsttrap.ui.Space

import dev.dheirav.thirsttrap.ui.FieldLabel
import dev.dheirav.thirsttrap.ui.AppIcons
import androidx.compose.foundation.layout.Arrangement
import dev.dheirav.thirsttrap.ui.Card
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import dev.dheirav.thirsttrap.ui.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import dev.dheirav.thirsttrap.ui.FilterChip
import androidx.compose.material3.Icon
import dev.dheirav.thirsttrap.ui.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import dev.dheirav.thirsttrap.ui.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.WhenLogged
import dev.dheirav.thirsttrap.domain.CheckResult
import dev.dheirav.thirsttrap.domain.Medium
import dev.dheirav.thirsttrap.domain.WateringMethod

/** Features F2.3 and F2.4 - the full event picker behind the sheet's "More". */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogEventScreen(onDone: () -> Unit, viewModel: LogEventViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cupboard by viewModel.cupboard.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.plantName.ifBlank { "Log an event" }) },
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
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FieldLabel("When?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WhenLogged.entries.forEach { w ->
                    FilterChip(
                        selected = state.whenLogged == w,
                        onClick = {
                            if (w == WhenLogged.PICK) showDatePicker = true else viewModel.onWhen(w)
                        },
                        label = {
                            Text(
                                if (w == WhenLogged.PICK && state.pickedDateMillis != null) {
                                    formatDate(state.pickedDateMillis!!)
                                } else {
                                    w.label
                                },
                            )
                        },
                    )
                }
            }
            if (state.isBackdated) {
                Text(
                    "Backdated entries are filed at midday, so they cannot sort ahead of " +
                        "something you logged that morning.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FieldLabel("What happened?")
            // Fourteen options of equal weight in one undifferentiated row,
            // "Died" among them. Hick's Law says decision time rises with the
            // log of the number of equally weighted choices, and the cost falls
            // hardest on the step that gates everything after it, which this is.
            //
            // The cost comes down through categorisation, not through a shorter
            // list, and the grouping is carried by SPACING rather than by four
            // more labels. Proximity is the law doing the work here, and adding
            // a fifth heading voice to fix a grouping problem would undo the
            // pass that got the app down to two.
            EVENT_GROUPS.forEachIndexed { i, group ->
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = if (i == 0) 0.dp else Space.Line),
                ) {
                    group.forEach { t ->
                        FilterChip(
                            selected = state.type == t,
                            onClick = { viewModel.onType(t) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }

            // The consequence, at the moment of the decision. The app used to
            // clear the anchors on save and let the user discover it days later
            // on a weight screen that had gone back to asking to be set up.
            if (state.clearsWeightSetup) {
                Card(Modifier.fillMaxWidth()) {
                    Column() {
                        Text(
                            "This clears the weight setup",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            "The pot itself now weighs something different, so every reading " +
                                "so far is measured against the wrong thing. Water it in and " +
                                "weigh it once afterwards, and the full mark sets itself again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            if (state.showAmount) {
                OutlinedTextField(
                    value = state.amountMl,
                    onValueChange = viewModel::onAmount,
                    label = { Text("Amount (ml, optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(WateringMethod.TOP, WateringMethod.BOTTOM_SOAK).forEach { m ->
                        FilterChip(
                            selected = state.method == m,
                            onClick = { viewModel.onMethod(m) },
                            label = { Text(if (m == WateringMethod.TOP) "from the top" else "bottom soak") },
                        )
                    }
                }
            }

            if (state.showCheckResult) {
                FieldLabel("How did it feel?")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        CheckResult.STILL_HEAVY,
                        CheckResult.GETTING_LIGHT,
                        CheckResult.DRY_WATERED,
                    ).forEach { r ->
                        FilterChip(
                            selected = state.checkResult == r,
                            onClick = { viewModel.onCheckResult(r) },
                            label = { Text(r.label) },
                        )
                    }
                }
            }

            if (state.showFertilizer) {
                // Pick from the cupboard rather than retyping it. Free text
                // stays underneath, because a one-off feed is a real thing and
                // not everything you pour has to be inventoried first.
                if (cupboard.isNotEmpty()) {
                    FieldLabel("From the cupboard")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cupboard.forEach { f ->
                            FilterChip(
                                selected = state.fertilizerName == f.name,
                                onClick = { viewModel.onPickFertilizer(f) },
                                label = { Text(f.name) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = state.fertilizerName,
                    onValueChange = viewModel::onFertilizer,
                    label = { Text("What did you use?") },
                    placeholder = { Text("banana tea, NPK") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.dilution,
                    onValueChange = viewModel::onDilution,
                    label = { Text("Dilution") },
                    placeholder = { Text("1:5, a pinch per litre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.showMedium) {
                FieldLabel("Moved into")
                Text(
                    "This clears the weight calibration - the pot itself changed weight, " +
                        "so every earlier reading is now meaningless.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Medium.entries.filter { it != Medium.UNKNOWN }.forEach { m ->
                        FilterChip(
                            selected = state.toMedium == m,
                            onClick = { viewModel.onToMedium(m) },
                            label = { Text(m.label) },
                        )
                    }
                }
            }

            if (state.showCause) {
                OutlinedTextField(
                    value = state.cause,
                    onValueChange = viewModel::onCause,
                    label = { Text("What do you think happened?") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNote,
                label = { Text("Note (optional)") },
                placeholder = { Text("new leaf, browning on the stem, fuzz on the roots") },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(onClick = { viewModel.save(onDone) }, modifier = Modifier.fillMaxWidth()) {
                Text("Log it")
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.pickedDateMillis ?: System.currentTimeMillis(),
            // A watering cannot have happened tomorrow.
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis <= System.currentTimeMillis() + 86_400_000L
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onPickedDate(pickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text("Use this day") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = pickerState) }
    }
}



private fun formatDate(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))

/**
 * The event types, in four groups the hand already makes.
 *
 * Routine is what gets logged most and comes first. Tending is work done to the
 * plant. Trouble runs from noticing to treating to losing it, which is the
 * order that actually happens. Notes are the two that record rather than act.
 * UNKNOWN ("Other") stays out, as it did before.
 */
private val EVENT_GROUPS: List<List<CareEventType>> = listOf(
    listOf(
        CareEventType.WATERED, CareEventType.CHECKED,
        CareEventType.FERTILIZED, CareEventType.WATER_CHANGED,
    ),
    listOf(
        CareEventType.REPOTTED, CareEventType.MEDIUM_CHANGED,
        CareEventType.PRUNED, CareEventType.WEEDED, CareEventType.MOVED,
    ),
    listOf(
        CareEventType.PEST_OR_DISEASE, CareEventType.TREATED, CareEventType.DIED,
    ),
    listOf(CareEventType.OBSERVATION, CareEventType.MILESTONE),
)
