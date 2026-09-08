package dev.dheirav.thirsttrap.feature.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.layout.ContentScale
import dev.dheirav.thirsttrap.ui.PlantPhoto
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.PlantAttention
import dev.dheirav.thirsttrap.domain.Prediction
import dev.dheirav.thirsttrap.domain.SuppressionReason
import kotlinx.coroutines.delay
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
    onOpenPlant: (String) -> Unit,
    onLogMore: (String) -> Unit,
    onOpenPropagation: () -> Unit,
    onScanned: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val photoError by viewModel.photoError.collectAsStateWithLifecycle()
    val archived by viewModel.archived.collectAsStateWithLifecycle()
    val showArchived by viewModel.showArchived.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val ctx = androidx.compose.ui.platform.LocalContext.current

    var sheetFor by remember { mutableStateOf<PlantAttention?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // The sort order is frozen while an undo is pending. Re-sorting on the log
    // itself yanks the card out from under the finger - found on device, where
    // logging one plant and reaching for UNDO hit a different plant's droplet.
    // rememberSaveable, not remember: the camera app can take this Activity
    // down with it, and a lost plant id means the photo lands nowhere.
    var photoFor by rememberSaveable { mutableStateOf<String?>(null) }
    val capture = dev.dheirav.thirsttrap.photo.rememberPhotoCapture { uri ->
        photoFor?.let { viewModel.addPhoto(it, uri) }
        photoFor = null
    }

    var frozenOrder by remember { mutableStateOf<List<String>?>(null) }
    // Counts snackbars still in flight. Watering several plants in a row is
    // normal, and the first one finishing must not unfreeze the order while a
    // later undo is still offered.
    var pendingUndos by remember { mutableIntStateOf(0) }
    // A ticking clock, not a value frozen at composition. Without it a reminder
    // falling due at 09:00 shows no badge until some unrelated database write
    // happens to re-emit the flow.
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000)
        }
    }

    val items = remember(state.items, frozenOrder) {
        val order = frozenOrder ?: return@remember state.items
        val byId = state.items.associateBy { it.plant.id }
        order.mapNotNull { byId[it] } + state.items.filter { it.plant.id !in order }
    }

    fun announce(event: CareEvent, message: String) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        frozenOrder = items.map { it.plant.id }
        pendingUndos++
        scope.launch {
            val result = snackbarHost.showSnackbar(message, "UNDO", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(event)
            pendingUndos--
            if (pendingUndos == 0) frozenOrder = null
        }
    }

    LaunchedEffect(photoError) {
        photoError?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearPhotoError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plants") },
                actions = {
                    IconButton(onClick = {
                        dev.dheirav.thirsttrap.feature.qr.ScanPot.scan(
                            context = ctx,
                            onPlantId = onScanned,
                            onProblem = { scope.launch { snackbarHost.showSnackbar(it) } },
                        )
                    }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan a pot sticker")
                    }
                    IconButton(onClick = onOpenPropagation) {
                        Icon(Icons.Filled.Spa, contentDescription = "Propagation board")
                    }
                },
            )
        },
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
                if (archived.isNotEmpty()) {
                    item {
                        FilterChip(
                            selected = showArchived,
                            onClick = { viewModel.toggleArchived() },
                            label = { Text("${archived.size} archived") },
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }

                if (showArchived) {
                    items(archived, key = { "archived-" + it.id }) { plant ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(plant.name, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "archived - history kept",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = { viewModel.unarchive(plant.id) }) {
                                    Text("Restore")
                                }
                            }
                        }
                    }
                }

                items(items, key = { it.plant.id }) { item ->
                    PlantCard(
                        item = item,
                        nowMillis = now,
                        onQuickWater = {
                            viewModel.logWatered(item.plant.id, item.suggestedWaterMl) { event ->
                                announce(event, wateredMessage(item.plant.name, item.suggestedWaterMl))
                            }
                        },
                        onQuickCheck = {
                            viewModel.logStillWet(item.plant.id) { event ->
                                announce(event, "Good call - ${item.plant.name} checked, not thirsty yet")
                            }
                        },
                        onDetailedWater = { onLogMore(item.plant.id) },
                        onOpenSheet = { sheetFor = item },
                        onLongPress = { onOpenPlant(item.plant.id) },
                    )
                }
            }
        }
    }

    sheetFor?.let { item ->
        ModalBottomSheet(onDismissRequest = { sheetFor = null }, sheetState = sheetState) {
            QuickLogSheet(
                plantName = item.plant.name,
                suggestedWaterMl = item.suggestedWaterMl,
                onWatered = {
                    sheetFor = null
                    viewModel.logWatered(item.plant.id, item.suggestedWaterMl) { e ->
                        announce(e, wateredMessage(item.plant.name, item.suggestedWaterMl))
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
                onPhoto = {
                    photoFor = item.plant.id
                    sheetFor = null
                    capture.takePhoto()
                },
                onMore = { sheetFor = null; onLogMore(item.plant.id) },
                onHistory = { sheetFor = null; onOpenPlant(item.plant.id) },
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
    onQuickCheck: () -> Unit,
    onDetailedWater: () -> Unit,
    onOpenSheet: () -> Unit,
    onLongPress: () -> Unit,
) {
    val plant = item.plant
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpenSheet,
                onClickLabel = "Quick log for ${plant.name}",
                onLongClick = onLongPress,
                onLongClickLabel = "Open ${plant.name}",
            ),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // The most recent photo, falling back to an initial. A broken or
            // missing file must never crash the list - see docs/UI-SPEC.md
            // section 9 - so the placeholder stays behind the image.
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
                    // Decorative: the card already announces the plant's name.
                    modifier = Modifier.clearAndSetSemantics { },
                )
                item.coverPhotoPath?.let { path ->
                    PlantPhoto(
                        path = path,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                    )
                }
            }

            Spacer(Modifier.size(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        plant.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    // Informational, never a tally of failures, and never red -
                    // overdue is not an error state. docs/UI-SPEC.md section 3.
                    val due = item.reminderDueMillis
                    if (due != null && due <= nowMillis) {
                        Box(
                            Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "check",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
                Text(
                    listOfNotNull(
                        plant.location?.takeIf { it.isNotBlank() },
                        plant.medium.label.lowercase(),
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
                        if (days == 0) "Checked today - not thirsty"
                        else relativeDays(nowMillis, checked, "Checked"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (plant.isWeightTrackable) {
                    item.depletion?.let { DepletionBar(it, plant.depletionTrigger, item.prediction is Prediction.WaterNow) }
                }

                PredictionLine(item.prediction)

                // Requirements item 8. Needs two waterings to measure between.
                item.averageIntervalDays?.let { avg ->
                    Text(
                        cadenceLabel(avg),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(
                        onClickLabel = "Log checked, still wet for ${plant.name}",
                        onClick = onQuickCheck,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.TouchApp,
                    contentDescription = "Log checked, still wet for ${plant.name}",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            // Tap logs immediately; long-press opens the detailed entry, for
            // the times you want to record something other than the usual.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .combinedClickable(
                        onClick = onQuickWater,
                        onLongClick = onDetailedWater,
                    )
                    .semantics {
                        contentDescription = "Log watering for ${plant.name}. " +
                            "Long press for amount and method."
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** Says the amount back, so a one-tap log is never a surprise. */
private fun wateredMessage(name: String, ml: Double?): String =
    if (ml != null) "Logged - $name watered ${ml.toInt()} ml" else "Logged - $name watered"

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
        is Prediction.NeedAnotherReading -> when (prediction.reason) {
            // Telling someone to weigh a cutting in a jar is nonsense.
            SuppressionReason.WEIGHT_MEANINGLESS_FOR_MEDIUM -> null
            SuppressionReason.NOT_CALIBRATED -> null
            SuppressionReason.NEEDS_RECALIBRATION -> "Needs recalibrating"
            SuppressionReason.NO_MEASURABLE_DRYING -> "Not drying measurably yet"
            SuppressionReason.NO_READINGS,
            SuppressionReason.ONE_READING_NO_HISTORY -> "Weigh once more to predict"
        }
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
    suggestedWaterMl: Double?,
    onWatered: () -> Unit,
    onStillWet: () -> Unit,
    onPhoto: () -> Unit,
    onMore: () -> Unit,
    onHistory: () -> Unit,
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
            FilledTonalButton(onClick = onWatered, modifier = Modifier.weight(1f).heightIn(min = 64.dp)) {
                Text(
                    if (suggestedWaterMl != null) "Watered\n${suggestedWaterMl.toInt()} ml" else "Watered",
                    textAlign = TextAlign.Center,
                )
            }
            FilledTonalButton(onClick = onStillWet, modifier = Modifier.weight(1f).heightIn(min = 64.dp)) {
                Text("Still wet")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
            FilledTonalButton(onClick = onPhoto, modifier = Modifier.weight(1f).heightIn(min = 64.dp)) {
                Text("Photo")
            }
            FilledTonalButton(onClick = onMore, modifier = Modifier.weight(1f).heightIn(min = 64.dp)) {
                Text("More…")
            }
        }
        Row {
            TextButton(onClick = onHistory, modifier = Modifier.padding(top = 8.dp)) { Text("History") }
            TextButton(onClick = onEdit, modifier = Modifier.padding(top = 8.dp)) { Text("Edit plant") }
        }
    }
}

/** "every 1 days" is the kind of thing that makes an app feel unfinished. */
private fun cadenceLabel(avgDays: Double): String = when (val d = avgDays.toInt()) {
    0 -> "Waters more than once a day"
    1 -> "Waters roughly every day"
    else -> "Waters roughly every $d days"
}
