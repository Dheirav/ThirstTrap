package dev.dheirav.thirsttrap.feature.locations

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.ColumnHead
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.ScreenTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
fun LocationsScreen(onBack: () -> Unit, viewModel: LocationsViewModel = hiltViewModel()) {
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
            )
        },
    ) { padding ->
        if (state.rows.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (state.loaded) "No places yet" else "",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Give a plant a location when you add or edit it, and the place appears " +
                        "here to be described.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                Text(
                    "Tap a place to describe it. Measuring the light from a plant that lives " +
                        "there records it here too.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
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
                        .padding(vertical = 12.dp),
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
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    row.note?.note?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                Rule()
            }
        }
    }

    editing?.let { row ->
        var text by remember(row.name) { mutableStateOf(row.note?.note.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(row.name) },
            text = {
                Column {
                    Text(
                        "What is this spot like? Which way it faces, when the sun reaches it, " +
                            "whether the radiator is under it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setNote(row.name, text)
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}
