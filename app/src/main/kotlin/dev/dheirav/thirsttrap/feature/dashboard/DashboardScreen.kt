package dev.dheirav.thirsttrap.feature.dashboard

import dev.dheirav.thirsttrap.ui.Motion
import dev.dheirav.thirsttrap.ui.DoubleRule
import dev.dheirav.thirsttrap.ui.OutlinedButton
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.BranchingMark
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import dev.dheirav.thirsttrap.ui.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import dev.dheirav.thirsttrap.ui.FilledTonalButton
import dev.dheirav.thirsttrap.ui.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
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
    onOpenWeighing: () -> Unit,
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
                title = {
                    Text(
                        "PLANTS",
                        style = MaterialTheme.typography.titleLarge,
                        letterSpacing = 0.22.em,
                    )
                },
                actions = {
                    IconButton(onClick = {
                        dev.dheirav.thirsttrap.feature.qr.ScanPot.scan(
                            context = ctx,
                            onPlantId = onScanned,
                            onProblem = { scope.launch { snackbarHost.showSnackbar(it) } },
                        )
                    }) {
                        Icon(AppIcons.qrCodeScanner, contentDescription = "Scan a pot sticker")
                    }
                    // Weighing is a round, not a per-plant errand, so it belongs
                    // on the list rather than four taps inside one plant.
                    IconButton(onClick = onOpenWeighing) {
                        Icon(AppIcons.weight, contentDescription = "Weigh the plants")
                    }
                    IconButton(onClick = onOpenPropagation) {
                        Icon(AppIcons.spa, contentDescription = "Propagation board")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            // A ruled block, not a floating one. The shadow was the last thing
            // on the page still pretending to hover above the paper.
            FloatingActionButton(
                onClick = onAddPlant,
                shape = MaterialTheme.shapes.small,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                ),
            ) {
                Icon(AppIcons.add, contentDescription = "Add a plant")
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
                    start = 16.dp, end = 16.dp, top = 0.dp,
                    bottom = 88.dp + WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
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
        ModalBottomSheet(
            onDismissRequest = { sheetFor = null },
            sheetState = sheetState,
            shape = MaterialTheme.shapes.large,
            // A drag handle is a screen affordance; a printed page does not have
            // one. The rule under the plant's name does the same job of saying
            // "this panel starts here".
            dragHandle = null,
        ) {
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
        BranchingMark(Modifier.size(140.dp, 160.dp))
        Spacer(Modifier.height(24.dp))
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
    // An entry on a page, not a card. A card says "separate object"; a rule says
    // "same page, further down", which is the truer statement about a list of
    // plants you are keeping. It also removes the last container in the app
    // that could read as full-or-empty.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onOpenSheet,
                onClickLabel = "Quick log for ${plant.name}",
                onLongClick = onLongPress,
                onLongClickLabel = "Open ${plant.name}",
            ),
    ) {
        Row(
            Modifier.padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The most recent photo, falling back to an initial. A broken or
            // missing file must never crash the list - see docs/UI-SPEC.md
            // section 9 - so the placeholder stays behind the image.
            // The depletion ring wraps the photo, so the state of the pot and
            // the picture of the pot are the same object. It costs no vertical
            // space, which is what let the card lose three rows.
            DepletionThumbnail(
                photoPath = item.coverPhotoPath,
                initial = plant.name.take(1).uppercase(),
                depletion = if (plant.isWeightTrackable) item.depletion else null,
                trigger = plant.depletionTrigger,
            )

            Spacer(Modifier.size(12.dp))

            // Four rows, not seven. The card used to stack name, location,
            // watered, checked, a bar, a prediction and a cadence - five of them
            // 11-12sp in the same grey, which is a wall rather than a hierarchy.
            // Row 2 is the one people actually read, so it gets the weight and
            // everything else moves down a tier or to the detail screen.
            Column(Modifier.weight(1f)) {
                // 1. Who.
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
                                .clip(MaterialTheme.shapes.extraSmall)
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

                val watered = item.lastWateredMillis
                val wateredText =
                    if (watered == null) "Never watered"
                    else relativeDays(nowMillis, watered, "Watered")
                val prediction = predictionText(item.prediction)

                // 2. The answer. A prediction when there is one; otherwise the
                //    most recent fact, which is the best answer available.
                // The row people read, so a change in it should register as a
                // change rather than as a different card.
                // Hoisted: transitionSpec is not a composable scope, so the
                // reduce-motion check has to happen out here.
                val swap = Motion.confirm<Float>()
                AnimatedContent(
                    targetState = prediction ?: wateredText,
                    transitionSpec = { fadeIn(swap) togetherWith fadeOut(swap) },
                    label = "the answer",
                ) { answer ->
                    Text(
                        answer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                // 4. Everything else, one line, in the dim tier - the third step
                //    of the lightness ladder rather than a fourth thing in grey.
                //    Cadence is not here: it lives on the detail screen, which is
                //    where someone goes to ask that question.
                val checked = item.lastCheckedMillis
                val context = buildAnnotatedString {
                    val parts = mutableListOf<Pair<String, Boolean>>()
                    plant.location?.takeIf { it.isNotBlank() }?.let { parts += it to false }
                    // Medium is deliberately absent. "soil" under a photograph
                    // of soil is a caption for something already on screen, and
                    // it was on every card in the list. It survives on the
                    // detail screen only when it is NOT soil, where it is
                    // actually information.
                    // Only repeat the watering line if row 2 did not use it.
                    if (prediction != null) parts += wateredText to false
                    // Restraint deserves visible credit, not silence - so this
                    // one fragment keeps its colour even in the dim row.
                    if (checked != null && checked > (watered ?: 0L)) {
                        val days = ((nowMillis - checked) / 86_400_000L).toInt()
                        parts += (
                            if (days == 0) "checked today, not thirsty"
                            else relativeDays(nowMillis, checked, "Checked").lowercase()
                            ) to true
                    }
                    parts.forEachIndexed { i, (text, credit) ->
                        if (i > 0) append(" · ")
                        if (credit) {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                append(text)
                            }
                        } else {
                            append(text)
                        }
                    }
                }
                Text(
                    context,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            // The two log actions. UI-SPEC section 7 requires "Still wet" to get
            // the same confirmation as "Watered" - the app must not celebrate
            // watering and stay silent about restraint. That was previously
            // satisfied only because neither had one.
            //
            // Both were also TouchApp and WaterDrop: filled, primary, the same
            // size, side by side, with nothing saying which was which. They now
            // differ in the only way that survives being 24dp of green - the
            // glyph depicts the action. A hand held back from the pot, and a
            // drop.
            LogAction(
                icon = AppIcons.stillWet,
                label = "Log checked, still wet for ${plant.name}",
                justLogged = item.lastCheckedMillis,
                onClick = onQuickCheck,
            )

            // Tap logs immediately; long-press opens the detailed entry, for
            // the times you want to record something other than the usual.
            LogAction(
                icon = AppIcons.waterDrop,
                loggedIcon = AppIcons.waterDropFilled,
                label = "Log watering for ${plant.name}. Long press for amount and method.",
                justLogged = item.lastWateredMillis,
                onClick = onQuickWater,
                onLongClick = onDetailedWater,
            )
        }
        Rule()
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
private fun predictionText(prediction: Prediction): String? = when (prediction) {
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
}

/**
 * The pot's state, drawn around its photo.
 *
 * Replaces a full-width bar that filled toward 100%. A bar filling to full is
 * the visual grammar of *task completion* - the one grammar this app exists to
 * avoid, since it turns "this plant is drying normally" into "you are 62% of
 * the way to doing your job". A ring reads as a level, not as progress toward a
 * score, and Planta and Oura both use one for the same reason.
 *
 * The trigger is a tick on the ring rather than a number, because the number
 * was never the point: what matters is whether the pot has passed the mark.
 */
@Composable
private fun DepletionThumbnail(
    photoPath: String?,
    initial: String,
    depletion: Double?,
    trigger: Double,
) {
    val ringStroke = 3.dp
    val gap = 3.dp
    val photo = 88.dp
    // Only reserve the ring's margin when there is a ring. A plant with no
    // weight readings was paying 12dp of height for an indicator it never
    // draws, on every card in the list.
    val total = if (depletion != null) photo + (ringStroke + gap) * 2 else photo

    val filled by animateFloatAsState(
        targetValue = depletion?.coerceIn(0.0, 1.0)?.toFloat() ?: 0f,
        animationSpec = Motion.settle(),
        label = "depletion",
    )
    val track = MaterialTheme.colorScheme.outlineVariant
    val fill = MaterialTheme.colorScheme.primary
    val tick = MaterialTheme.colorScheme.onSurfaceVariant

    Box(Modifier.size(total), contentAlignment = Alignment.Center) {
        if (depletion != null) {
            Canvas(Modifier.size(total)) {
                val inset = ringStroke.toPx() / 2f
                // Concentric with the photo: its own 8dp corner plus however
                // far the ring sits outside it, or the two curves fight.
                val radius = CornerRadius((12.dp + ringStroke + gap).toPx())
                val outline = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = inset, top = inset,
                            right = size.width - inset, bottom = size.height - inset,
                            cornerRadius = radius,
                        ),
                    )
                }
                drawPath(outline, track, style = Stroke(ringStroke.toPx()))

                val measure = PathMeasure().apply { setPath(outline, false) }
                val length = measure.length
                if (filled > 0f) {
                    val segment = Path()
                    // Starts at the top-left corner and runs clockwise, so the
                    // ring reads the way the eye already scans the card.
                    measure.getSegment(0f, length * filled, segment, true)
                    drawPath(segment, fill, style = Stroke(ringStroke.toPx(), cap = StrokeCap.Round))
                }
                val at = Path()
                val markAt = (length * trigger.toFloat()).coerceIn(0f, length)
                measure.getSegment((markAt - 4f).coerceAtLeast(0f), markAt, at, true)
                drawPath(at, tick, style = Stroke(ringStroke.toPx() * 1.6f, cap = StrokeCap.Round))
            }
        }

        // A bright photo on a near-black card has a hard cut-out edge; the
        // hairline inset resolves it into the card instead.
        Box(
            modifier = Modifier
                .size(photo)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initial,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                // Decorative: the card already announces the plant's name.
                modifier = Modifier.clearAndSetSemantics { },
            )
            photoPath?.let {
                PlantPhoto(
                    path = it,
                    contentDescription = null,
                    modifier = Modifier.size(photo).clip(MaterialTheme.shapes.medium),
                )
            }
        }
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
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp)) {
        // The sheet gets the same furniture as a page: a head, a rule, then
        // entries. Previously it was a floating panel of pills that shared no
        // vocabulary with the list it came out of.
        Text(
            plantName.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            letterSpacing = 0.18.em,
        )
        Text(
            "How does the pot feel?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        DoubleRule(Modifier.padding(top = 10.dp, bottom = 16.dp))

        // Both answers, same size and weight. Neither is the primary one.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = onWatered, modifier = Modifier.weight(1f).height(SheetBlock)) {
                Text(
                    if (suggestedWaterMl != null) "Watered  ${suggestedWaterMl.toInt()} ml" else "Watered",
                    textAlign = TextAlign.Center,
                )
            }
            FilledTonalButton(onClick = onStillWet, modifier = Modifier.weight(1f).height(SheetBlock)) {
                Text("Still wet")
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 10.dp),
        ) {
            OutlinedButton(onClick = onPhoto, modifier = Modifier.weight(1f).height(SheetBlock)) {
                Text("Photo")
            }
            OutlinedButton(onClick = onMore, modifier = Modifier.weight(1f).height(SheetBlock)) {
                Text("More")
            }
        }

        Rule(Modifier.padding(top = 20.dp))
        Row(Modifier.padding(top = 4.dp)) {
            TextButton(onClick = onHistory) { Text("History") }
            TextButton(onClick = onEdit) { Text("Edit plant") }
        }
    }
}

/**
 * One height for all four sheet buttons.
 *
 * "Watered 50 ml" used to wrap onto a second line while its neighbours did not,
 * which made a 2x2 grid of buttons that were not the same size. The amount now
 * sits on one line beside the word.
 */
private val SheetBlock = 56.dp

/** "every 1 days" is the kind of thing that makes an app feel unfinished. */
private fun cadenceLabel(avgDays: Double): String = when (val d = avgDays.toInt()) {
    0 -> "Waters more than once a day"
    1 -> "Waters roughly every day"
    else -> "Waters roughly every $d days"
}

/**
 * One of the two log buttons on a card.
 *
 * [justLogged] is the timestamp of the most recent event of this kind. When it
 * moves to within [Motion.CONFIRM_MS] of now, the icon does one small scale-and-
 * fade - the same one for both actions, which is the point. Watering the plant
 * and deciding not to are equally valid outcomes, and the UI has to treat them
 * that way or it is quietly scoring one above the other.
 *
 * No overshoot: it settles, it does not bounce.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LogAction(
    icon: Painter,
    label: String,
    justLogged: Long?,
    onClick: () -> Unit,
    loggedIcon: Painter? = null,
    onLongClick: (() -> Unit)? = null,
) {
    // Keyed on the timestamp, not on the tap, so it also fires when the log
    // came from somewhere else - the quick-log sheet, a scanned sticker.
    var confirming by remember { mutableStateOf(false) }
    LaunchedEffect(justLogged) {
        if (justLogged == null) return@LaunchedEffect
        if (System.currentTimeMillis() - justLogged > 1_500L) return@LaunchedEffect
        confirming = true
        delay(Motion.CONFIRM_MS.toLong() * 2)
        confirming = false
    }

    val scale by animateFloatAsState(
        targetValue = if (confirming) 1.18f else 1f,
        animationSpec = Motion.confirm(),
        label = "log confirmation",
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (confirming && loggedIcon != null) loggedIcon else icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.scale(scale),
        )
    }
}
