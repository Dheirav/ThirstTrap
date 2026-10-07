package dev.dheirav.thirsttrap.feature.dashboard

import dev.dheirav.thirsttrap.ui.Flank

import dev.dheirav.thirsttrap.ui.Space

import dev.dheirav.thirsttrap.ui.EmptyState
import dev.dheirav.thirsttrap.ui.Button
import dev.dheirav.thirsttrap.ui.WateringAnswer
import dev.dheirav.thirsttrap.ui.BlockHeight
import dev.dheirav.thirsttrap.ui.MenuLabels
import dev.dheirav.thirsttrap.ui.AlmanacSheet
import dev.dheirav.thirsttrap.ui.AlmanacMenu
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.dheirav.thirsttrap.ui.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import dev.dheirav.thirsttrap.ui.FilledTonalButton
import dev.dheirav.thirsttrap.ui.FilterChip
import dev.dheirav.thirsttrap.ui.FloatingActionButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import dev.dheirav.thirsttrap.ui.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import dev.dheirav.thirsttrap.ui.TextButton
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
import androidx.compose.ui.geometry.Size
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
import dev.dheirav.thirsttrap.domain.calendarDaysAgo
import dev.dheirav.thirsttrap.domain.PlantAttention
import dev.dheirav.thirsttrap.domain.Prediction
import dev.dheirav.thirsttrap.domain.SuppressionReason
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dev.dheirav.thirsttrap.domain.Confidence
import dev.dheirav.thirsttrap.ui.AlmanacTitle

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
    onOpenWeighing: () -> Unit,
    onOpenMore: () -> Unit,
    onWeighPlant: (String) -> Unit,
    /** Settings' "show the specialist tools". Off hides the rarer entries. */
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
                    AlmanacTitle("Plants")
                },
                actions = {
                    // One icon, not four. Weighing is the recurring job and the
                    // reason the app exists, so it gets the bar; everything else
                    // gets a word, because a glyph nobody can read is worse than
                    // a menu. docs/NAVIGATION.md.
                    IconButton(onClick = onOpenWeighing) {
                        Icon(AppIcons.weight, contentDescription = "Weigh the plants")
                    }
                    // Opens a page, not a menu. Six bare words in a dropdown
                    // told a reader nothing about what Figures or Places were,
                    // and a menu is somewhere you look only once you know what
                    // is in it. Same two taps, and now the first one explains
                    // the choice. docs/NAVIGATION.md section 9.
                    IconButton(onClick = onOpenMore) {
                        Icon(AppIcons.moreVert, contentDescription = "More")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            // A ruled block, not a floating one. The shadow was the last thing
            // on the page still pretending to hover above the paper.
            //
            // Hidden while the list is empty. The empty state already offers
            // "Add your first plant", and two primary actions calling the same
            // function is the page asking one question twice.
            if (state.items.isNotEmpty()) {
                FloatingActionButton(onClick = onAddPlant) {
                    Icon(AppIcons.add, contentDescription = "Add a plant")
                }
            }
        },
    ) { padding ->
        when {
            // Never a spinner: an empty first frame costs half the ten-second
            // logging budget before the user has done anything.
            !state.loaded -> Box(Modifier.fillMaxSize().padding(padding))

            state.items.isEmpty() -> EmptyState(
                    title = if (state.loaded) "No plants yet" else "",
                    body = "Add the first one and start logging. Everything stays on this device.",
                    modifier = Modifier.fillMaxSize().padding(padding),
                    mark = { BranchingMark(Modifier.size(140.dp, 160.dp)) },
                    action = { Button(onClick = onAddPlant) { Text("Add your first plant") } },
                )

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
                            modifier = Modifier.padding(bottom = Space.Line),
                        )
                    }
                }

                if (showArchived) {
                    items(archived, key = { "archived-" + it.id }) { plant ->
                        // A ruled entry, like every other plant on this list.
                        // It was a bordered Card eighty lines above PlantCard,
                        // which closes with Rule(): the same screen said "these
                        // are separate objects" and "this is the same page,
                        // further down" about two halves of one list.
                        Column {
                            Row(
                                Modifier.padding(vertical = Space.Entry),
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
                            Rule()
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
                        onOpen = { onOpenPlant(item.plant.id) },
                        onOpenSheet = { sheetFor = item },
                    )
                }
            }
        }
    }

    sheetFor?.let { item ->
        AlmanacSheet(
            onDismissRequest = { sheetFor = null },
            sheetState = sheetState,
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
                onWeigh = { sheetFor = null; onWeighPlant(item.plant.id) },
                weighable = item.plant.isWeightTrackable,
                onMore = { sheetFor = null; onLogMore(item.plant.id) },
                onHistory = { sheetFor = null; onOpenPlant(item.plant.id) },
                onEdit = { sheetFor = null; onEditPlant(item.plant.id) },
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
    onQuickCheck: () -> Unit,
    onOpen: () -> Unit,
    onOpenSheet: () -> Unit,
) {
    val plant = item.plant
    // An entry on a page, not a card. A card says "separate object"; a rule says
    // "same page, further down", which is the truer statement about a list of
    // plants you are keeping. It also removes the last container in the app
    // that could read as full-or-empty.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Tapping a plant opens the plant. It used to open the quick-log
            // sheet, with the plant's own page behind a long press, which is
            // backwards from what every list on the phone does and made the
            // page the undiscoverable half.
            //
            // The sheet keeps the long press. That is defensible where the
            // hidden menu on a diary row was not, because nothing in the sheet
            // is only in the sheet: the two commonest actions are visible icons
            // on this row, and everything else is on the page the tap now
            // opens - which is why "Log something" had to be added there first.
            .combinedClickable(
                onClick = onOpen,
                onClickLabel = "Open ${plant.name}",
                onLongClick = onOpenSheet,
                onLongClickLabel = "Quick log for ${plant.name}",
            ),
    ) {
        Row(
            Modifier.padding(vertical = Space.Entry),
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

            Spacer(Modifier.size(Space.Entry))

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
                                .padding(start = Space.Line)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = Space.Line, vertical = Space.Hair),
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
                        modifier = Modifier.padding(top = Space.Hair),
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
                        // Calendar days, not elapsed hours. D38 fixed this on
                        // the answer row and missed the line directly under it,
                        // so a check at 22:00 still read "checked today" at
                        // 09:00 the next morning.
                        val tz = java.util.TimeZone.getDefault().getOffset(nowMillis) / 60_000
                        val days = calendarDaysAgo(nowMillis, checked, tz)
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
                    modifier = Modifier.padding(top = Space.Hair),
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
            // Nothing to log against a plant that has gone. The row stays on
            // the list, because un-archiving one is how you read its history,
            // but offering to water it is the same mistake as predicting a
            // date for it.
            if (!plant.status.isGone) {
                LogAction(
                    icon = AppIcons.stillWet,
                    label = "Log checked, still wet for ${plant.name}",
                    justLogged = item.lastCheckedMillis,
                    onClick = onQuickCheck,
                )

                // Dead space between two targets that mean opposite things.
                // Fitts's Law is usually quoted as "big and close"; its other
                // half is that two targets sharing a boundary have a mis-tap
                // rate set by the boundary, not by their size. Both are 48dp
                // and they were flush, so the miss between "still wet" and
                // "watered" was a wrong entry in the diary rather than a near
                // miss. I made that exact mistake on this row.
                Spacer(Modifier.width(Space.Line))

                // One long press per card. The row already opens the quick
                // sheet on long press, and this held a second long press with
                // an invisible boundary between them, so holding the row and
                // holding the droplet did different things. The sheet's "More"
                // calls onLogMore with the same plant id, so nothing is lost.
                LogAction(
                    icon = AppIcons.waterDrop,
                    loggedIcon = AppIcons.waterDropFilled,
                    label = "Log watering for ${plant.name}",
                    justLogged = item.lastWateredMillis,
                    onClick = onQuickWater,
                )
            }
        }
        Rule()
    }
}

/** Says the amount back, so a one-tap log is never a surprise. */
private fun wateredMessage(name: String, ml: Double?): String =
    if (ml != null) "Logged - $name watered ${ml.toInt()} ml" else "Logged - $name watered"

/**
 * Read in the reader's own frame: the card is a statement about now, so "today"
 * means the day it is where the phone is, not where the entry was logged.
 */
private fun relativeDays(now: Long, then: Long, verb: String): String {
    val offset = java.util.TimeZone.getDefault().getOffset(now) / 60_000
    return when (val d = calendarDaysAgo(now, then, offset)) {
        0 -> "$verb today"
        1 -> "$verb yesterday"
        else -> "$verb $d days ago"
    }
}

@Composable
private fun predictionText(prediction: Prediction): String? = when (prediction) {
    is Prediction.WaterNow -> "Needs water now"
    is Prediction.Eta -> {
        // The model's confidence tiers, said out loud (WATERING-MODEL §6). A
        // measured fit and a guess from past cycles used to read identically
        // here, which is precisely the false confidence the tiers exist to
        // avoid: honest uncertainty is the product, not a caveat.
        val whenText = when {
            prediction.days < 1.0 -> "today"
            prediction.days < 2.0 -> "tomorrow"
            else -> "in about ${prediction.days.toInt()} days"
        }
        // Capped decides the phrase, confidence decides the qualifier, and both
        // always apply. Capped used to be the first arm of the whole when, so
        // it swallowed the confidence entirely: a capped ETA from a
        // low-confidence EWMA prior read exactly like one from a
        // high-confidence fit, while the weight screen went on saying
        // "estimated from past cycles" about the same Prediction. Two screens
        // disagreeing about one value is what the tiers exist to prevent.
        val head = when {
            prediction.capped -> "More than 2 weeks"
            prediction.confidence == Confidence.LOW -> "Maybe $whenText"
            else -> "Water $whenText"
        }
        head + when (prediction.confidence) {
            Confidence.HIGH -> ""
            Confidence.MEDIUM -> " - still learning"
            Confidence.LOW -> ", from past cycles"
        }
    }
    is Prediction.NeedAnotherReading -> when (prediction.reason) {
        // Telling someone to weigh a cutting in a jar is nonsense.
        SuppressionReason.WEIGHT_MEANINGLESS_FOR_MEDIUM -> null
        // Was null, so the one state a brand new weighable plant is actually in
        // said nothing on its card. SuppressionHelp already carries the full
        // explanation; this is the short form of it, and the help page is still
        // a tap away for the rest.
        SuppressionReason.NOT_CALIBRATED -> "Weigh it once after watering to start"
        // Said out loud rather than left blank: the row is on the list because
        // somebody un-archived it, so the useful thing is to say what it is.
        SuppressionReason.PLANT_IS_GONE -> "This one has gone"
        SuppressionReason.NEEDS_RECALIBRATION -> "Needs recalibrating"
        SuppressionReason.NO_MEASURABLE_DRYING -> "Not drying measurably yet"
        // The fact line under this already says "watered today", so the answer
        // row only has to stop claiming the pot is thirsty.
        SuppressionReason.WATERED_SINCE_LAST_READING -> "Weigh it to start the new cycle"
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

    // Read off the photo rather than restated, because restating it is how the
    // ring and the photo came to disagree.
    val photoShape = MaterialTheme.shapes.medium

    Box(Modifier.size(total), contentAlignment = Alignment.Center) {
        if (depletion != null) {
            Canvas(Modifier.size(total)) {
                val inset = ringStroke.toPx() / 2f
                // Concentric with the photo: ITS corner plus however far the
                // ring sits outside it, or the two curves fight.
                //
                // This was a hardcoded 12dp against a comment claiming the photo
                // had an 8dp corner, while AppShapes.medium is RoundedCornerShape
                // (0.dp). So a round-cornered ring was being drawn around a hard
                // square: at the corners the ring bowed away from the photo by
                // its full radius, which is the frame not wrapping the image.
                // Three numbers that each had to be kept in step by hand, and
                // were not. Taking the corner from the shape itself means the
                // ring follows the photo wherever the theme puts it.
                val photoCorner = (photoShape as? RoundedCornerShape)
                    ?.topStart?.toPx(Size(photo.toPx(), photo.toPx()), this) ?: 0f
                val radius = CornerRadius(photoCorner + (ringStroke + gap).toPx())
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
    onWeigh: () -> Unit,
    onMore: () -> Unit,
    onHistory: () -> Unit,
    onEdit: () -> Unit,
    weighable: Boolean,
) {
    Column(Modifier.padding(start = Space.Section, end = Space.Section, top = Space.Section, bottom = Space.Page)) {
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
            modifier = Modifier.padding(top = Space.Hair),
        )
        DoubleRule()

        WateringAnswer(
            onWatered = onWatered,
            onStillWet = onStillWet,
            suggestedWaterMl = suggestedWaterMl,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.Line),
            modifier = Modifier.padding(top = Space.Line),
        ) {
            OutlinedButton(onClick = onPhoto, modifier = Modifier.weight(1f).height(BlockHeight)) {
                Text("Photo")
            }
            // The feature the app exists for was three taps inside a plant's
            // overflow menu, named after its implementation. One tap from the
            // list, and hidden only where weight means nothing.
            if (weighable) {
                OutlinedButton(onClick = onWeigh, modifier = Modifier.weight(1f).height(BlockHeight)) {
                    Text("Weigh it")
                }
            }
            OutlinedButton(onClick = onMore, modifier = Modifier.weight(1f).height(BlockHeight)) {
                Text("More")
            }
        }

        Rule(flank = Flank.Section)
        Row(Modifier.padding(top = Space.Tight)) {
            TextButton(onClick = onHistory) { Text("History") }
            TextButton(onClick = onEdit) { Text("Edit plant") }
        }
    }
}


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
            // Square, like every other affordance. This was the one remaining
            // circle: the ripple drew a disc on a card made entirely of right
            // angles, which is the same two-vocabularies problem the pill
            // toggles had, just smaller.
            .clip(MaterialTheme.shapes.small)
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
