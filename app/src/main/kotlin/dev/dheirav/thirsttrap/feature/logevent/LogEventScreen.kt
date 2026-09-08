package dev.dheirav.thirsttrap.feature.logevent

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.plantName.ifBlank { "Log an event" }) },
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
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("When?", style = MaterialTheme.typography.labelLarge)
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

            Text("What happened?", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CareEventType.entries.filter { it != CareEventType.UNKNOWN }.forEach { t ->
                    FilterChip(
                        selected = state.type == t,
                        onClick = { viewModel.onType(t) },
                        label = { Text(t.label) },
                    )
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
                Text("How did it feel?", style = MaterialTheme.typography.labelLarge)
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
                Text("Moved into", style = MaterialTheme.typography.labelLarge)
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
