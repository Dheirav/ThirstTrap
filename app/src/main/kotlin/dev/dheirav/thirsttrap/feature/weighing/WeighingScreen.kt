package dev.dheirav.thirsttrap.feature.weighing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.domain.ReadingContext
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.Button
import dev.dheirav.thirsttrap.ui.ColumnHead
import dev.dheirav.thirsttrap.ui.DoubleRule
import dev.dheirav.thirsttrap.ui.FilledTonalButton
import dev.dheirav.thirsttrap.ui.FilterChip
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.ScreenTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val lastSeen = SimpleDateFormat("d MMM", Locale.getDefault())

/**
 * Weigh everything in one sitting.
 *
 * The scale comes out once, so the round is the unit of work, not the plant.
 * The table is the running sheet: every trackable pot, what it weighed last
 * time, and what it weighs now. Saving moves to the next pot that has not been
 * done, so you put one down, pick the next one up, and the app is already
 * asking for the right number.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun WeighingScreen(onBack: () -> Unit, viewModel: WeighingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cursor by viewModel.cursor.collectAsStateWithLifecycle()
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val context by viewModel.context.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle("Weighing") },
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
                    if (state.loaded) "Nothing to weigh" else "",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Weight only says something for a pot of soil. Plants living in water are " +
                        "left out of the round.",
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
                    if (state.remaining == 0) {
                        "All weighed. Tap any row to change one."
                    } else {
                        "${state.remaining} left. Tap a row to weigh it; saving moves to the next."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    ColumnHead("Plant", Modifier.weight(1f))
                    ColumnHead("Last", Modifier.weight(0.34f))
                    ColumnHead("Now", Modifier.weight(0.3f))
                }
                Rule()
            }
            itemsIndexed(state.rows, key = { _, r -> r.plant.id }) { index, row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { viewModel.open(index) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        row.plant.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        row.last?.let { "${it.grams.toInt()} g" } ?: "-",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(0.34f),
                    )
                    Text(
                        row.doneThisRound?.let { "${it.grams.toInt()} g" } ?: "-",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (row.doneThisRound != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier.weight(0.3f),
                    )
                }
                Rule()
            }
            item {
                Text(
                    row2Hint(state.rows.count { it.last != null }, state.rows.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }

    cursor?.let { index ->
        val row = state.rows.getOrNull(index) ?: return@let
        ModalBottomSheet(
            onDismissRequest = viewModel::close,
            sheetState = sheetState,
            shape = MaterialTheme.shapes.large,
            dragHandle = null,
        ) {
            Column(
                Modifier
                    .fillMaxHeight(0.88f)
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
            ) {
                Text(
                    row.plant.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    letterSpacing = 0.18.em,
                )
                Text(
                    buildString {
                        append(if (entry.isBlank()) "grams, pot and all" else "$entry g")
                        row.last?.let { append("     last ${it.grams.toInt()} g, ${lastSeen.format(Date(it.timestampMillis))}") }
                    },
                    style = MaterialTheme.typography.headlineMedium,
                )
                DoubleRule(Modifier.padding(top = 10.dp, bottom = 14.dp))

                Column(Modifier.weight(1f)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    ) {
                        listOf(
                            ReadingContext.ROUTINE,
                            ReadingContext.PRE_WATER,
                            ReadingContext.POST_WATER,
                        ).forEach { c ->
                            FilterChip(
                                selected = context == c,
                                onClick = { viewModel.onContext(c) },
                                label = { Text(c.label) },
                            )
                        }
                    }
                    Keypad(
                        onDigit = viewModel::onDigit,
                        onBackspace = viewModel::onBackspace,
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = viewModel::saveAndAdvance,
                        enabled = entry.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) { Text(if (state.remaining > 1) "Save and next" else "Save") }
                    TextButton(onClick = viewModel::skip) { Text("Skip") }
                }
            }
        }
    }
}

private fun row2Hint(withHistory: Int, total: Int): String = when {
    withHistory == 0 ->
        "Nothing has been weighed yet. Weigh each pot once now, then again in a day or two, " +
            "and the curves start."
    withHistory < total ->
        "$withHistory of $total have a previous weight to compare against."
    else -> "Every pot has a previous weight, so each of these adds a point to its curve."
}

@Composable
private fun Keypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf("123", "456", "789", ".0<")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                row.forEach { ch ->
                    FilledTonalButton(
                        onClick = { if (ch == '<') onBackspace() else onDigit(ch) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        if (ch == '<') {
                            Icon(AppIcons.backspace, contentDescription = "Delete last digit")
                        } else {
                            Text(ch.toString(), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }
}
