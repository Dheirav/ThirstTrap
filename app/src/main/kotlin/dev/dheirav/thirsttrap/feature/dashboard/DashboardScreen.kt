package dev.dheirav.thirsttrap.feature.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.PlantAttention
import dev.dheirav.thirsttrap.domain.Prediction
import dev.dheirav.thirsttrap.domain.SuppressionReason
import kotlinx.coroutines.launch

/**
 * The dashboard, now on real data.
 *
 * The primary path is ONE tap - the droplet on the card, no navigation and no
 * confirmation dialog, with a 5-second undo. A confirmation dialog on every
 * watering would be the worst decision in this app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddPlant: () -> Unit,
    onEditPlant: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var sheetFor by remember { mutableStateOf<PlantAttention?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // The sort order is frozen while an undo is pending. Re-sorting on the log
    // itself yanks the card out from under the finger - found on device, where
    // logging one plant and reaching for UNDO hit a different plant's droplet.
    var frozenOrder by remember { mutableStateOf<List<String>?>(null) }
    val items = remember(state.items, frozenOrder) {
        val order = frozenOrder ?: return@remember state.items
        val byId = state.items.associateBy { it.plant.id }
        order.mapNotNull { byId[it] } + state.items.filter { it.plant.id !in order }
    }

    fun announce(event: CareEvent, message: String) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        frozenOrder = items.map { it.plant.id }
        scope.launch {
            val result = snackbarHost.showSnackbar(message, "UNDO", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(event)
            frozenOrder = null
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Plants") }) },
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPlant) {
                Icon(Icons.Filled.Add, contentDescription = "Add a plant")
            }
        },
    ) { padding ->
        when {
            // Never a spinner: an empty first frame costs half the ten-second
            // logging budget before the user has done anything.
            !state.loaded -> Box(Modifier.fillMaxSize().padding(padding))

            state.items.isEmpty() -> EmptyState(Modifier.fillMaxSize().padding(padding), onAddPlant)

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 16.dp,
                    bottom = 88.dp + WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.plant.id }) { item ->
                    PlantCard(
                        item = item,
                        nowMillis = System.currentTimeMillis(),
                        onQuickWater = {
                            viewModel.logWatered(item.plant.id) { event ->
                                announce(event, "Logged - ${item.plant.name} watered")
                            }
                        },
                        onOpenSheet = { sheetFor = item },
                        onLongPress = { onEditPlant(item.plant.id) },
                    )
                }
            }
        }
    }

    sheetFor?.let { item ->
        ModalBottomSheet(onDismissRequest = { sheetFor = null }, sheetState = sheetState) {
            QuickLogSheet(
                plantName = item.plant.name,
                onWatered = {
                    sheetFor = null
                    viewModel.logWatered(item.plant.id) { e ->
                        announce(e, "Logged - ${item.plant.name} watered")
                    }
                },
                onStillWet = {
                    sheetFor = null
                    viewModel.logStillWet(item.plant.id) { e ->
                        // Identical haptic, identical snackbar treatment. If
                        // watering felt rewarding and restraint felt like a
                        // dismissal, the app would be teaching the wrong answer.
                        announce(e, "Good call - checked, not thirsty yet")
                    }
                },
                onEdit = { sheetFor = null; onEditPlant(item.plant.id) },
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier, onAddPlant: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No plants yet", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Add the first one and start logging. Everything stays on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        FilledTonalButton(onClick = onAddPlant) { Text("Add your first plant") }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlantCard(
    item: PlantAttention,
    nowMillis: Long,
    onQuickWater: () -> Unit,
    onOpenSheet: () -> Unit,
    onLongPress: () -> Unit,
) {
    val plant = item.plant
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpenSheet, onLongClick = onLongPress),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    plant.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(Modifier.size(12.dp))

            Column(Modifier.weight(1f)) {
                Text(plant.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(
                        plant.location?.takeIf { it.isNotBlank() },
                        plant.medium.name.lowercase().replace('_', ' '),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val watered = item.lastWateredMillis
                Text(
                    if (watered == null) "Never watered" else relativeDays(nowMillis, watered, "Watered"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Only when a check is more recent than the last watering:
                // restraint deserves visible credit, not silence.
                val checked = item.lastCheckedMillis
                if (checked != null && checked > (watered ?: 0L)) {
                    val days = ((nowMillis - checked) / 86_400_000L).toInt()
                    Text(
                        if (days == 0) "Checked today - not thirsty" else "Checked $days days ago",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (plant.isWeightTrackable) {
                    item.depletion?.let { DepletionBar(it, plant.depletionTrigger, item.prediction is Prediction.WaterNow) }
                }

                PredictionLine(item.prediction)
            }

            IconButton(
                onClick = onQuickWater,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Log watering for ${plant.name}" },
            ) {
                Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun relativeDays(now: Long, then: Long, verb: String): String =
    when (val d = ((now - then) / 86_400_000L).toInt()) {
        0 -> "$verb today"
        1 -> "$verb yesterday"
        else -> "$verb $d days ago"
    }

@Composable
private fun PredictionLine(prediction: Prediction) {
    val text = when (prediction) {
        is Prediction.WaterNow -> "Needs water now"
        is Prediction.Eta -> when {
            prediction.capped -> "More than 2 weeks"
            prediction.days < 1.0 -> "Water today"
            prediction.days < 2.0 -> "Water tomorrow"
            else -> "Water in about ${prediction.days.toInt()} days"
        }
        // Telling someone to weigh a cutting in a jar is nonsense; say nothing.
        is Prediction.NeedAnotherReading ->
            if (prediction.reason == SuppressionReason.WEIGHT_MEANINGLESS_FOR_MEDIUM) null else null
    } ?: return

    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Composable
private fun DepletionBar(depletion: Double, trigger: Double, pastTrigger: Boolean) {
    val pct = (depletion * 100).toInt()
    Column(Modifier.padding(top = 6.dp)) {
        Box(
            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier.fillMaxWidth(depletion.coerceIn(0.0, 1.0).toFloat()).height(8.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Box(Modifier.fillMaxWidth(trigger.toFloat()).height(8.dp), contentAlignment = Alignment.CenterEnd) {
                Box(Modifier.size(width = 2.dp, height = 8.dp).background(MaterialTheme.colorScheme.onSurfaceVariant))
            }
        }
        Text(
            if (pastTrigger) "$pct% - past its ${(trigger * 100).toInt()}% trigger" else "$pct% toward watering",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun QuickLogSheet(
    plantName: String,
    onWatered: () -> Unit,
    onStillWet: () -> Unit,
    onEdit: () -> Unit,
) {
    Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 40.dp)) {
        Text(plantName, style = MaterialTheme.typography.titleLarge)
        Text(
            "How does the pot feel?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )
        // Both answers, same size and weight. Neither is the primary one.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = onWatered, modifier = Modifier.weight(1f).height(64.dp)) {
                Text("Watered")
            }
            FilledTonalButton(onClick = onStillWet, modifier = Modifier.weight(1f).height(64.dp)) {
                Text("Still wet")
            }
        }
        TextButton(onClick = onEdit, modifier = Modifier.padding(top = 8.dp)) { Text("Edit plant") }
    }
}
