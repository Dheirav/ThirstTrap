package dev.dheirav.thirsttrap.feature.fertilizer

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
import androidx.compose.material3.FloatingActionButton
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
import dev.dheirav.thirsttrap.domain.DoseAdvice
import dev.dheirav.thirsttrap.domain.Fertilizer
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.ColumnHead
import dev.dheirav.thirsttrap.ui.FilterChip
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.ScreenTitle
import kotlin.math.roundToInt

/** 1.2 ml rather than 1.234567 ml. Nobody pours to a microlitre. */
private fun Double.ml(): String =
    if (this >= 10) roundToInt().toString() else ((this * 10).roundToInt() / 10.0).toString()

/**
 * Feature F14. The cupboard and the arithmetic, on one page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FertilizerScreen(onBack: () -> Unit, viewModel: FertilizerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Fertilizer?>(null) }
    var adding by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle("Feeding") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(AppIcons.add, contentDescription = "Add a fertiliser")
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                Text(
                    "What is in the cupboard, and how much of each goes in the can you are " +
                        "holding. Pick the can size and read the last column.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(250.0, 500.0, 1000.0, 2000.0).forEach { ml ->
                        FilterChip(
                            selected = state.canMl == ml,
                            onClick = { viewModel.onCan(ml) },
                            label = { Text(if (ml >= 1000) "${(ml / 1000).toInt()} L" else "${ml.toInt()} ml") },
                        )
                    }
                }
            }

            if (state.rows.isEmpty()) {
                item {
                    Text(
                        if (state.loaded) {
                            "Nothing here yet. Add a bottle and type its dilution exactly as " +
                                "the label writes it."
                        } else {
                            ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    )
                }
                return@LazyColumn
            }

            item {
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    ColumnHead("Fertiliser", Modifier.weight(1f))
                    ColumnHead("Label", Modifier.weight(0.34f))
                    ColumnHead("Pour", Modifier.weight(0.34f))
                }
                Rule()
            }

            items(state.rows, key = { it.fertilizer.id }) { row ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { editing = row.fertilizer }
                        .padding(vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(row.fertilizer.name, style = MaterialTheme.typography.bodyMedium)
                            row.fertilizer.npk?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                        Text(
                            row.fertilizer.dilutionText ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.34f),
                        )
                        Text(
                            when (val d = row.dose) {
                                is DoseAdvice.Pour -> "${d.concentrateMl.ml()} ml"
                                is DoseAdvice.TooSmall -> "too little"
                                DoseAdvice.Unknown -> "-"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (row.dose) {
                                is DoseAdvice.Pour -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.weight(0.34f),
                        )
                    }
                    // The refusals explain themselves rather than leaving a dash.
                    when (val d = row.dose) {
                        is DoseAdvice.TooSmall -> Text(
                            "That is ${d.concentrateMl.ml()} ml, too little to measure. Mix " +
                                "${(d.suggestedWaterMl / 1000).toInt()} L and keep what is left over.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        DoseAdvice.Unknown -> if (row.fertilizer.dilutionText != null) {
                            Text(
                                "The dilution is not in a form this can work with. Try 1:200 " +
                                    "or 5 ml/L.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        } else Unit
                        else -> Unit
                    }
                    row.fertilizer.note?.takeIf { it.isNotBlank() }?.let {
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

    if (adding || editing != null) {
        FertilizerDialog(
            existing = editing,
            onDismiss = { adding = false; editing = null },
            onSave = { n, d, k, note ->
                viewModel.save(editing, n, d, k, note)
                adding = false; editing = null
            },
            onDelete = editing?.let { f -> { viewModel.delete(f.id); editing = null } },
        )
    }
}

@Composable
private fun FertilizerDialog(
    existing: Fertilizer?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var dilution by remember(existing?.id) { mutableStateOf(existing?.dilutionText.orEmpty()) }
    var npk by remember(existing?.id) { mutableStateOf(existing?.npk.orEmpty()) }
    var note by remember(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var confirmingDelete by remember(existing?.id) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    confirmingDelete -> "Remove this fertiliser?"
                    existing == null -> "Add a fertiliser"
                    else -> existing.name
                },
            )
        },
        text = {
            if (confirmingDelete) {
                Text(
                    "It leaves the cupboard. Entries that already record using it are not " +
                        "touched.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = dilution,
                        onValueChange = { dilution = it },
                        label = { Text("Dilution, as the label writes it") },
                        placeholder = { Text("1:200, or 5 ml/L") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    )
                    OutlinedTextField(
                        value = npk,
                        onValueChange = { npk = it },
                        label = { Text("NPK (optional)") },
                        placeholder = { Text("3-1-2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (confirmingDelete) {
                TextButton(onClick = { onDelete?.invoke() }) { Text("Remove") }
            } else {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = { onSave(name, dilution, npk, note) },
                ) { Text("Save") }
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null && !confirmingDelete) {
                    TextButton(onClick = { confirmingDelete = true }) { Text("Remove") }
                }
                TextButton(onClick = { if (confirmingDelete) confirmingDelete = false else onDismiss() }) {
                    Text(if (confirmingDelete) "Keep it" else "Cancel")
                }
            }
        },
    )
}
