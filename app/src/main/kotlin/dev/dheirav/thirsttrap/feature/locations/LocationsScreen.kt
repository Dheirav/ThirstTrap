package dev.dheirav.thirsttrap.feature.locations

import dev.dheirav.thirsttrap.ui.Flank

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import dev.dheirav.thirsttrap.ui.EmptyState
import dev.dheirav.thirsttrap.ui.IconButton
import androidx.compose.material3.MaterialTheme
import dev.dheirav.thirsttrap.ui.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import dev.dheirav.thirsttrap.ui.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.ColumnHead
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.AlmanacDialog
import dev.dheirav.thirsttrap.ui.DialogText
import dev.dheirav.thirsttrap.ui.ScreenTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dev.dheirav.thirsttrap.ui.Space

private val measured = SimpleDateFormat("d MMM", Locale.getDefault())

/**
 * The places, rather than the plants.
 *
 * A location was free text on a plant and nothing else - four pots could all
 * say "windowsill" and the app knew nothing about the windowsill. This is the
 * gazetteer: what the light is like there, how many things live in it, and
 * whatever you want to remember about the aspect.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(
    onBack: () -> Unit,
    onMeasure: (String) -> Unit,
    onOpenConditions: () -> Unit,
    advanced: Boolean,
    viewModel: LocationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<LocationRow?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle("Places") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
                    }
                },
                // In the bar rather than only inside a row, because a row needs
                // a place to exist and this screen has to work when empty.
                actions = {
                    if (advanced) {
                        TextButton(onClick = onOpenConditions) { Text("Conditions") }
                    }
                },
            )
        },
    ) { padding ->
        if (state.rows.isEmpty()) {
            EmptyState(
                title = if (state.loaded) "No places yet" else "",
                body = "Give a plant a location when you add or edit it, or record the room " +
                        "conditions somewhere, and the place appears here to be described.",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Space.Block),
        ) {
            item {
                Text(
                    "Everything the app knows about a spot: what the light is, what the room " +
                        "has been doing, and what lives there. Tap a place to describe it or " +
                        "to measure it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Space.Block),
                )
                Row(Modifier.fillMaxWidth().padding(bottom = Space.Tight)) {
                    ColumnHead("Place", Modifier.weight(1f))
                    ColumnHead("Plants", Modifier.weight(0.28f))
                    ColumnHead("Light", Modifier.weight(0.42f))
                }
                Rule()
            }
            items(state.rows, key = { it.name.lowercase() }) { row ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { editing = row }
                        .padding(vertical = Space.Entry),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            row.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (row.plantCount == 0) "-" else row.plantCount.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (row.plantCount == 0) MaterialTheme.colorScheme.outline
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(0.28f),
                        )
                        Text(
                            row.note?.level?.label ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.42f),
                        )
                    }
                    row.note?.lux?.let { lux ->
                        Text(
                            "${lux.toInt()} lux" +
                                (row.note.luxMeasuredAtMillis?.let {
                                    ", measured ${measured.format(Date(it))}"
                                } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = Space.Hair),
                        )
                    }
                    row.latestAmbient?.let { a ->
                        val bits = listOfNotNull(
                            a.temperatureC?.let { t -> "${t.toInt()} C" },
                            a.humidityPercent?.let { h -> "${h.toInt()}% humidity" },
                        )
                        if (bits.isNotEmpty()) {
                            Text(
                                bits.joinToString(", ") +
                                    ", ${measured.format(Date(a.timestampMillis))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = Space.Hair),
                            )
                        }
                    }
                    row.note?.note?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Space.Tight),
                        )
                    }
                }
                Rule()
            }
        }
    }

    editing?.let { row ->
        var text by remember(row.name) { mutableStateOf(row.note?.note.orEmpty()) }
        AlmanacDialog(
            title = row.name,
            onDismissRequest = { editing = null },
            body = {
                Column {
                    DialogText(
                        "What is this spot like? Which way it faces, when the sun reaches it, " +
                            "whether the radiator is under it.",
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth().padding(top = Space.Entry),
                    )
                    // Light is a property of the spot, not of whichever pot
                    // happens to be standing in it, so you should not have to
                    // pick a plant first in order to ask about a window.
                    Rule(flank = Flank.Section)
                    TextButton(
                        onClick = {
                            // Only if there is something to keep. Measuring a
                            // place you have not described should not leave an
                            // empty note behind as a side effect.
                            if (text.isNotBlank() || row.note?.note != null) {
                                viewModel.setNote(row.name, text)
                            }
                            editing = null
                            onMeasure(row.name)
                        },
                        modifier = Modifier.padding(top = Space.Tight),
                    ) {
                        Text(
                            row.note?.lux?.let { "Measure the light again" }
                                ?: "Measure the light here",
                        )
                    }
                    if (advanced) {
                        TextButton(onClick = { editing = null; onOpenConditions() }) {
                            Text("Temperature and humidity")
                        }
                    }
                }
            },
            dismiss = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
            confirm = {
                TextButton(onClick = {
                    viewModel.setNote(row.name, text)
                    editing = null
                }) { Text("Save") }
            },
        )
    }
}
