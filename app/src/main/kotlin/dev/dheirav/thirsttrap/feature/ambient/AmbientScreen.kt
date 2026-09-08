package dev.dheirav.thirsttrap.feature.ambient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.domain.AmbientReading
import dev.dheirav.thirsttrap.domain.AmbientSource
import dev.dheirav.thirsttrap.ui.AppIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Record what the room is doing, by location rather than by plant.
 *
 * Four pots on one windowsill share a windowsill. Logging the same reading
 * against each of them would be four times the work for the same fact, and
 * would lose it entirely when one of those plants is deleted - which is
 * precisely when the history of the spot becomes interesting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbientScreen(onBack: () -> Unit, viewModel: AmbientViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    var location by rememberSaveable { mutableStateOf("") }
    var temp by rememberSaveable { mutableStateOf("") }
    var humidity by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    // Clear the numbers after a save but keep the location: the next reading is
    // usually the same shelf a week later.
    val lastSaved = remember { mutableStateOf(0) }
    if (saved != lastSaved.value) {
        lastSaved.value = saved
        temp = ""; humidity = ""; note = ""
    }

    val tempValue = temp.trim().toDoubleOrNull()
    val humidityValue = humidity.trim().toDoubleOrNull()
    val canSave = location.isNotBlank() && (tempValue != null || humidityValue != null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Room conditions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            item {
                Text(
                    "A pot dries faster in a warm dry room than a cool damp one. Recording " +
                        "the room now and then lets the app tell the difference between a " +
                        "plant that changed and a season that changed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Nothing here changes a prediction. It only explains one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (state.knownLocations.isNotEmpty()) {
                item {
                    Text(
                        "Where",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.knownLocations.take(4).forEach { known ->
                            FilterChip(
                                selected = location.equals(known, ignoreCase = true),
                                onClick = { location = known },
                                label = { Text(known) },
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = temp,
                        onValueChange = { temp = it },
                        label = { Text("Temp °C") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = humidity,
                        onValueChange = { humidity = it },
                        label = { Text("Humidity %") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    "Either one on its own is useful. Both is better.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                FilledTonalButton(
                    onClick = { viewModel.record(location, tempValue, humidityValue, note) },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) { Text("Record") }

                HorizontalDivider(Modifier.padding(vertical = 20.dp))
            }

            if (state.readings.isEmpty()) {
                item {
                    Text(
                        if (state.loaded) "Nothing recorded yet." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                items(state.readings, key = { it.id }) { reading ->
                    AmbientRow(reading, onDelete = { viewModel.delete(reading.id) })
                }
            }
        }
    }
}

private val stamp = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())

@Composable
private fun AmbientRow(reading: AmbientReading, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    listOfNotNull(
                        reading.temperatureC?.let { "%.1f °C".format(it) },
                        reading.humidityPercent?.let { "%.0f%% RH".format(it) },
                    ).joinToString("  ·  "),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    listOfNotNull(
                        reading.location,
                        stamp.format(Date(reading.timestampMillis)),
                        // Outdoor weather stays labelled as outdoor weather all
                        // the way to the row: indoors is not outdoors.
                        reading.source.takeIf { it != AmbientSource.MANUAL }?.label,
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                reading.note?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}
