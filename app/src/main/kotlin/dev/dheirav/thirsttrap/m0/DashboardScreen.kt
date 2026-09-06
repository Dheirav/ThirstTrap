package dev.dheirav.thirsttrap.m0

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.dheirav.thirsttrap.domain.PlantAttention
import dev.dheirav.thirsttrap.domain.Prediction
import kotlinx.coroutines.launch

/**
 * M0 prototype of the dashboard. Answers hard UX problem #1: is logging a
 * watering faster here than writing it on paper?
 *
 * The primary path is ONE tap - the droplet on the card, no navigation, no
 * confirmation dialog, with a 5-second undo. A confirmation dialog on every
 * watering would be the single worst decision in this app.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    onSendTestReminder: (String) -> Unit,
) {
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // M0 recomposition trigger; Room Flows replace this in M1.
    var tick by remember { mutableStateOf(0) }
    var sheetFor by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val now = System.currentTimeMillis()
    val items = remember(tick) { FakeData.attention(now) }

    fun logWatered(plantId: String) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val entry = FakeData.logWatered(plantId)
        tick++
        scope.launch {
            val result = snackbarHost.showSnackbar(
                message = "Logged - ${FakeData.nameOf(plantId)} watered",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                FakeData.undo(entry)
                tick++
            }
        }
    }

    fun logStillWet(plantId: String) {
        // Identical haptic and identical snackbar treatment to logWatered.
        // Restraint is the skill being rewarded here.
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val entry = FakeData.logChecked(plantId, stillWet = true)
        tick++
        scope.launch {
            val result = snackbarHost.showSnackbar(
                message = "Good call - checked, not thirsty yet",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                FakeData.undo(entry)
                tick++
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Plants") }) },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        // No loading spinner, deliberately: the list must be on screen the
        // instant the activity draws, or the ten-second budget is already gone.
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.plant.id }) { item ->
                PlantCard(
                    item = item,
                    nowMillis = now,
                    onQuickWater = { logWatered(item.plant.id) },
                    onOpenSheet = { sheetFor = item.plant.id },
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { onSendTestReminder("fern") }) {
                    Text("Send a test reminder in 10s")
                }
            }
        }
    }

    sheetFor?.let { plantId ->
        ModalBottomSheet(onDismissRequest = { sheetFor = null }, sheetState = sheetState) {
            QuickLogSheet(
                plantName = FakeData.nameOf(plantId),
                onWatered = { sheetFor = null; logWatered(plantId) },
                onStillWet = { sheetFor = null; logStillWet(plantId) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlantCard(
    item: PlantAttention,
    nowMillis: Long,
    onQuickWater: () -> Unit,
    onOpenSheet: () -> Unit,
) {
    val plant = item.plant
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpenSheet, onLongClick = onOpenSheet),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Stand-in for the photo thumbnail; real capture arrives in M1.
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
                    listOfNotNull(plant.location, plant.medium.name.lowercase().replace('_', ' '))
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.lastWateredMillis?.let {
                    val days = ((nowMillis - it) / 86_400_000L).toInt()
                    Text(
                        if (days == 0) "Watered today" else "Watered $days days ago",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // The bar appears only when the plant is calibrated. An empty or
                // zeroed bar would be a lie; absence is honest.
                item.depletion?.let { DepletionBar(it, plant.depletionTrigger) }

                val label = FakeData.predictionLabel(item.prediction)
                if (label != null) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                } else if (item.prediction is Prediction.NeedAnotherReading) {
                    Text(
                        "Weigh once more to predict",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            IconButton(
                onClick = onQuickWater,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Log watering for ${plant.name}" },
            ) {
                Icon(
                    Icons.Filled.WaterDrop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Fraction of the way to needing water, with the trigger marked. Carries a
 * percentage label so meaning is never encoded in colour alone.
 */
@Composable
private fun DepletionBar(depletion: Double, trigger: Double) {
    val pct = (depletion * 100).toInt()
    Column(Modifier.padding(top = 6.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(depletion.coerceIn(0.0, 1.0).toFloat())
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Box(
                Modifier
                    .fillMaxWidth(trigger.toFloat())
                    .height(8.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    Modifier
                        .size(width = 2.dp, height = 8.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
        Text(
            "$pct% toward watering",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * The secondary path: three taps total for anything that is not a plain
 * watering. Both answers are the same size and weight - neither is styled as
 * the primary one.
 */
@Composable
private fun QuickLogSheet(
    plantName: String,
    onWatered: () -> Unit,
    onStillWet: () -> Unit,
) {
    Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 40.dp)) {
        Text(plantName, style = MaterialTheme.typography.titleLarge)
        Text(
            "How does the pot feel?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(
                onClick = onWatered,
                modifier = Modifier.weight(1f).height(64.dp),
            ) { Text("Watered") }
            FilledTonalButton(
                onClick = onStillWet,
                modifier = Modifier.weight(1f).height(64.dp),
            ) { Text("Still wet") }
        }
    }
}
