package dev.dheirav.thirsttrap.feature.weight

import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.ColumnHead
import dev.dheirav.thirsttrap.ui.DoubleRule
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.SectionHead
import dev.dheirav.thirsttrap.ui.BlockHeight
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import dev.dheirav.thirsttrap.ui.Button
import dev.dheirav.thirsttrap.ui.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import dev.dheirav.thirsttrap.ui.FilledTonalButton
import androidx.compose.foundation.layout.Row
import dev.dheirav.thirsttrap.ui.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.em
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.domain.Confidence
import dev.dheirav.thirsttrap.domain.AmbientExplanation
import dev.dheirav.thirsttrap.domain.AmbientSource
import dev.dheirav.thirsttrap.domain.AmbientVerdict
import dev.dheirav.thirsttrap.domain.DryingDiagnostic
import dev.dheirav.thirsttrap.domain.Prediction
import dev.dheirav.thirsttrap.domain.ReadingContext
import dev.dheirav.thirsttrap.domain.SuppressionReason
import dev.dheirav.thirsttrap.domain.WeightState
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun WeightScreen(
    onBack: () -> Unit,
    onOpenScaleHelp: () -> Unit,
    viewModel: WeightViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val context by viewModel.context.collectAsStateWithLifecycle()
    val hint by viewModel.hint.collectAsStateWithLifecycle()
    val ambient by viewModel.ambientExplanation.collectAsStateWithLifecycle()
    val dismissed by viewModel.dismissed.collectAsStateWithLifecycle()
    var showCalibration by remember { mutableStateOf(false) }
    var showKeypad by remember { mutableStateOf(false) }
    // Skips the half-height stop: this sheet is a keypad, and a keypad you
    // have to drag open before you can use it is worse than no sheet.
    val keypadState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state?.isCalibrated) { state?.let(viewModel::suggestContext) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state?.plant?.name ?: "Weight") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onOpenScaleHelp) { Text("Help") }
                },
            )
        },
    ) { padding ->
        val s = state
        if (s == null) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        if (!s.plant.isWeightTrackable) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Weight won't help here", style = MaterialTheme.typography.titleMedium)
                Text(
                    "This one lives in water, so its weight says nothing about when it " +
                        "needs attention.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            return@Scaffold
        }

        // Head, plate, table - and nothing else. This screen used to stack a
        // status view, an entry view and a history view into one scroll:
        // headline, bar, two note cards, a chart, a heading, a live number, a
        // hint, three chips, a twelve-key pad, a save button, another heading, a
        // paragraph and twenty rows. Weighing is an act, not a view, so it moved
        // into a sheet and the page became a page.
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            if (!s.isCalibrated) {
                NotCalibratedCard(
                    needsRecalibration = s.plant.needsRecalibration,
                    onStart = { showKeypad = true },
                )
                // The curve, even with nothing to measure it against yet.
                if (s.readings.count { !it.excluded } >= 2) {
                    Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        WeightChart(
                            s,
                            Modifier.fillMaxWidth().height(200.dp).padding(10.dp)
                                .semantics { contentDescription = chartSummary(s) },
                        )
                    }
                    Text(
                        "Grams over time. Once you weigh it just after watering, this becomes " +
                            "a percentage and a prediction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            } else {
                PredictionHeadline(s)
                DepletionLine(s)
                DoubleRule(Modifier.padding(top = 12.dp, bottom = 16.dp))

                val diagKey = viewModel.diagnosticKey(s)
                if (s.diagnostic != null && diagKey != null && diagKey !in dismissed) {
                    DiagnosticCard(
                        d = s.diagnostic!!,
                        onDismiss = { viewModel.dismissDiagnostic(diagKey) },
                        onHelp = onOpenScaleHelp,
                    )
                }
                ambient?.let { AmbientCard(it) }

                if (s.readings.count { !it.excluded } < 2) {
                    Text(
                        "One reading so far. Weigh it again in a day or two and the curve " +
                            "starts here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    // A plate: the figure, boxed, with its caption underneath.
                    // This is where an almanac puts a diagram, and it is the one
                    // part of the app whose content is genuinely modern
                    // data-visualisation rather than a table.
                    Card(Modifier.fillMaxWidth()) {
                        WeightChart(
                            s,
                            Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(10.dp)
                                .semantics { contentDescription = chartSummary(s) },
                        )
                    }
                    Text(
                        "The drying curve. Each drop is one cycle between waterings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            // Always. Weighing a pot must not require having calibrated first -
            // the whole point is that the readings come before the app can
            // interpret them. The panel above still explains what is missing.
            FilledTonalButton(
                onClick = { showKeypad = true },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            ) { Text("Weigh it") }

            if (s.readings.isNotEmpty()) {
                SectionHead("Readings")
                val shown = s.readings.sortedByDescending { it.timestampMillis }.take(20)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
                    ColumnHead("Weight", Modifier.weight(0.32f))
                    ColumnHead("When", Modifier.weight(0.38f))
                    ColumnHead("Note", Modifier.weight(0.30f))
                }
                Rule()
                shown.forEach { r ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable(
                                onClickLabel = if (r.excluded) {
                                    "Include this reading again"
                                } else {
                                    "Exclude this reading from the curve"
                                },
                            ) { viewModel.setExcluded(r.id, !r.excluded) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${r.grams.toInt()} g",
                            // Strikethrough as well as tone: excluded must not
                            // be signalled by colour alone.
                            textDecoration = if (r.excluded) TextDecoration.LineThrough else null,
                            color = if (r.excluded) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(0.32f),
                        )
                        Text(
                            readingDate(r.timestampMillis, r.tzOffsetMinutes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.38f),
                        )
                        Text(
                            if (r.excluded) "excluded" else r.context.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(0.30f),
                        )
                    }
                    Rule()
                }
                Text(
                    if (s.readings.size > shown.size) {
                        "Most recent ${shown.size} of ${s.readings.size}. Tap a row to exclude " +
                            "a bad weigh-in."
                    } else {
                        "Tap a row to exclude a bad weigh-in - a pot half off the scale, or a " +
                            "wet saucer. It stays in the record but stops skewing the curve."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    if (showKeypad && state != null) {
        val s = state!!
        ModalBottomSheet(
            onDismissRequest = { showKeypad = false },
            sheetState = keypadState,
            shape = MaterialTheme.shapes.large,
            dragHandle = null,
        ) {
            // The save button is pinned outside the scroll. Everything above it
            // scrolls; it does not. Making the one action the sheet exists for
            // reachable only by scrolling past a twelve-key pad is the same
            // mistake as hiding it under the navigation bar, just less obvious.
            Column(
                Modifier
                    // Most of the screen, on purpose. This gets used standing at
                    // a windowsill holding a pot, and the keys grow to fill
                    // whatever is left after the head and the button - so the
                    // targets are as large as the phone allows rather than a
                    // fixed 52dp with dead space under them.
                    .fillMaxHeight(0.88f)
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
            ) {
              Column(Modifier.weight(1f)) {
                Text(
                    "WEIGH IT",
                    style = MaterialTheme.typography.titleMedium,
                    letterSpacing = 0.18.em,
                )
                Text(
                    if (entry.isBlank()) "grams, pot and all" else "$entry g",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                DoubleRule(Modifier.padding(top = 10.dp, bottom = 14.dp))

                hint?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }

                // Wraps rather than scrolling or squeezing. At three-up "after
                // watering" broke onto two lines and made one chip taller than
                // its neighbours; a scrolling row fixed the height but hid an
                // option off the edge, which is worse - all three need to be
                // visible to be chosen between.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                ) {
                    // The enum's own labels, not a second copy of the words -
                    // the chips and the readings table used to be able to drift.
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

                Button(
                    onClick = { viewModel.save {}; showKeypad = false },
                    enabled = entry.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) { Text("Save this weight") }

                if (!s.isCalibrated) {
                    Text(
                        "Not set up yet: pick \"just watered\" on a weigh taken after " +
                            "watering and draining, and that reading becomes this pot's full " +
                            "mark. Anything else is still recorded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
    }

    if (showCalibration) {
        CalibrationDialog(
            enteredGrams = entry.toDoubleOrNull(),
            onDismiss = { showCalibration = false },
            onConfirm = { wet, trigger ->
                viewModel.calibrate(wet, trigger) { }
                showCalibration = false
            },
        )
    }
}

@Composable
private fun PredictionHeadline(s: WeightState) {
    val (text, sub) = when (val p = s.prediction) {
        is Prediction.WaterNow -> "Needs water now" to null
        is Prediction.Eta -> {
            val main = when {
                p.capped -> "More than 2 weeks"
                p.days < 1 -> "Water today"
                p.days < 2 -> "Water tomorrow"
                else -> "Water in about ${p.days.toInt()} days"
            }
            // Confidence is said out loud rather than implied, so a guess from
            // history is never mistaken for a measurement.
            main to when (p.confidence) {
                Confidence.HIGH -> null
                Confidence.MEDIUM -> "still learning this plant"
                Confidence.LOW -> "estimated from past cycles"
            }
        }
        is Prediction.NeedAnotherReading -> when (p.reason) {
            SuppressionReason.NO_READINGS,
            SuppressionReason.ONE_READING_NO_HISTORY -> "Weigh once more to predict" to
                "One reading is a point, not a slope."
            SuppressionReason.NO_MEASURABLE_DRYING -> "Not drying measurably yet" to
                "Give it another day and weigh again."
            SuppressionReason.NEEDS_RECALIBRATION -> "Needs recalibrating" to
                "The pot changed, so earlier readings no longer apply."
            else -> "Not calibrated" to null
        }
    }
    Column {
        Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        sub?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        s.slopeGramsPerDay?.let {
            Text(
                "Losing about ${-it.toInt()} g a day",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * How far down the pot is, as a sentence.
 *
 * A fraction between the anchors, never raw grams - docs/WATERING-MODEL.md
 * section 8.
 *
 * This was a progress bar. A bar filling toward full is the grammar of task
 * completion, which is the one grammar this app exists to avoid - it turns "the
 * pot is drying normally" into "you are 62% of the way to doing your job". The
 * dashboard already replaced its bar with a ring for that reason; here there is
 * no thumbnail to wrap, and an almanac would not draw a bar anyway. It would
 * print the number.
 */
@Composable
private fun DepletionLine(s: WeightState) {
    val depletion = s.depletion ?: return
    val pct = (depletion * 100).toInt()
    val trigger = (s.plant.depletionTrigger * 100).toInt()
    Text(
        buildString {
            append("$pct% down")
            append("  ·  ")
            append(if (depletion >= s.plant.depletionTrigger) "past its $trigger% mark" else "waters at $trigger%")
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )
}

/** Phrased as a question. The app has a slope, not a stethoscope. */
@Composable
private fun DiagnosticCard(
    d: DryingDiagnostic,
    onDismiss: () -> Unit,
    onHelp: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(
                when (d) {
                    DryingDiagnostic.DRYING_FASTER_THAN_USUAL -> "Drying faster than usual"
                    DryingDiagnostic.POT_STAYING_HEAVY -> "This pot is staying heavy"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                when (d) {
                    DryingDiagnostic.DRYING_FASTER_THAN_USUAL ->
                        "Worth checking whether water is running down the sides rather than " +
                            "soaking in - a root ball that has shrunk away from the pot does that."
                    DryingDiagnostic.POT_STAYING_HEAVY ->
                        "The roots may have stopped drinking. Worth a look for rot, or a " +
                            "spell somewhere colder and darker than usual."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row {
                TextButton(onClick = onHelp) { Text("How to weigh") }
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}

@Composable
private fun NotCalibratedCard(needsRecalibration: Boolean, onStart: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (needsRecalibration) "Needs recalibrating" else "Not set up yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (needsRecalibration) {
                    "The pot itself changed weight, so every earlier reading is now " +
                        "meaningless. Water it, let it drain, and weigh it again."
                } else {
                    "Water it thoroughly, let it drain for half an hour, then weigh it. " +
                        "That one number is the whole setup - the dry end works itself out " +
                        "over the first cycle or two."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            FilledTonalButton(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) { Text("Set the watered weight") }
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Big targets: this gets used standing at a windowsill holding a pot. The
    // rows share the height they are given rather than each taking a fixed
    // 52dp, so on a tall phone the keys are genuinely large.
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

@Composable
private fun CalibrationDialog(
    enteredGrams: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit,
) {
    var trigger by remember { androidx.compose.runtime.mutableDoubleStateOf(0.5) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ScreenTitle("Set the watered weight") },
        text = {
            Column {
                Text(
                    if (enteredGrams == null) {
                        "Type the weight on the keypad first - the pot just after watering " +
                            "and half an hour of draining."
                    } else {
                        "${enteredGrams.toInt()} g becomes this plant's full mark. How dry " +
                            "should it get before it wants water again?"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (enteredGrams != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.3 to "ferns", 0.5 to "most", 0.75 to "succulents").forEach { (v, label) ->
                            FilterChip(
                                selected = kotlin.math.abs(trigger - v) < 0.01,
                                onClick = { trigger = v },
                                label = { Text("${(v * 100).toInt()}% · $label") },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = enteredGrams != null,
                onClick = { enteredGrams?.let { onConfirm(it, trigger) } },
            ) { Text("Set it") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Weight over time, with the anchors and the trigger drawn in.
 *
 * Compose Canvas rather than a charting library: it is a line, three
 * horizontals and some markers, and this way the segment boundaries can be
 * drawn exactly where the model puts them.
 */
@Composable
private fun WeightChart(s: WeightState, modifier: Modifier = Modifier) {
    // Null before the first post-water weigh. The curve is still real - it is
    // grams over time - so it is drawn without the anchor bands and the trigger
    // line rather than not drawn at all.
    val anchors = s.plant.anchors
    val points = s.readings.filterNot { it.excluded }.sortedBy { it.timestampMillis }
    if (points.size < 2) return

    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier) {
        // Time as days-since-first in Double before it touches Float. Epoch
        // millis through Float has a ULP of ~131 seconds at present dates, so
        // readings would quantise into roughly two-minute buckets.
        val originT = points.first().timestampMillis
        fun days(t: Long) = (t - originT) / 86_400_000.0
        val spanD = max(1e-6, days(points.last().timestampMillis))

        // Without anchors the scale is just the readings' own range.
        val lo = min(anchors?.dryGrams ?: Double.MAX_VALUE, points.minOf { it.grams })
        val hi = max(anchors?.wetGrams ?: Double.MIN_VALUE, points.maxOf { it.grams })
        val spanG = max(1.0, hi - lo)

        // Inset, or the topmost point and the first and last are half-clipped
        // by the canvas edge.
        val padY = 8.dp.toPx()
        val plotH = size.height - padY * 2
        val padX = 6.dp.toPx()
        val plotW = size.width - padX * 2

        fun x(t: Long) = padX + (days(t) / spanD * plotW).toFloat()
        fun y(g: Double) = padY + (plotH - ((g - lo) / spanG * plotH)).toFloat()

        // The anchor bands and the trigger line only mean something once there
        // is an anchor. Before that the curve is drawn bare rather than against
        // invented reference lines.
        anchors?.let { a ->
            drawLine(outline, Offset(0f, y(a.wetGrams)), Offset(size.width, y(a.wetGrams)),
                1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
            drawLine(outline, Offset(0f, y(a.dryGrams)), Offset(size.width, y(a.dryGrams)),
                1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
            val triggerG = a.triggerWeight(s.plant.depletionTrigger)
            drawLine(tertiary, Offset(0f, y(triggerG)), Offset(size.width, y(triggerG)),
                1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
        }

        // One polyline PER SEGMENT. Joining the last reading of one drying
        // cycle to the first of the next would draw exactly the continuous fit
        // across a watering that the model refuses to compute.
        s.segments.forEach { segment ->
            val seg = segment.readings.filterNot { it.excluded }
            for (i in 0 until seg.size - 1) {
                val a = seg[i]
                val b = seg[i + 1]
                drawLine(
                    primary,
                    Offset(x(a.timestampMillis), y(a.grams)),
                    Offset(x(b.timestampMillis), y(b.grams)),
                    2.dp.toPx(),
                )
            }
        }

        // Where each new cycle began.
        s.segments.drop(1).forEach { seg ->
            seg.first?.let {
                drawLine(outline, Offset(x(it.timestampMillis), 0f),
                    Offset(x(it.timestampMillis), size.height), 1.dp.toPx())
            }
        }

        // The fit over the current segment, dashed on to where it crosses the
        // trigger - the one element that makes the prediction legible.
        val slope = s.slopeGramsPerDay
        val last = s.segments.lastOrNull()?.readings?.lastOrNull { !it.excluded }
        // No anchor means no trigger to project onto, so no projection.
        val triggerG = anchors?.triggerWeight(s.plant.depletionTrigger)
        if (slope != null && slope < 0 && last != null && triggerG != null) {
            val daysToTrigger = (last.grams - triggerG) / -slope
            if (daysToTrigger > 0) {
                val endX = padX + ((days(last.timestampMillis) + daysToTrigger) / spanD * plotW).toFloat()
                drawLine(
                    tertiary,
                    Offset(x(last.timestampMillis), y(last.grams)),
                    Offset(endX.coerceAtMost(size.width), y(triggerG)),
                    1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                )
            }
        }

        points.forEach {
            drawCircle(primary, radius = 3.dp.toPx(), center = Offset(x(it.timestampMillis), y(it.grams)))
        }
    }
}

private fun readingDate(millis: Long, offsetMinutes: Int): String {
    val zone = java.time.ZoneOffset.ofTotalSeconds(offsetMinutes * 60)
    return java.time.Instant.ofEpochMilli(millis).atZone(zone)
        .format(java.time.format.DateTimeFormatter.ofPattern("d MMM, HH:mm"))
}

/** The chart is a void for a screen reader without this. */
private fun chartSummary(s: WeightState): String = buildString {
    val n = s.readings.count { !it.excluded }
    append("$n readings")
    s.slopeGramsPerDay?.let { append(", falling about ${-it.toInt()} grams a day") }
    when (val p = s.prediction) {
        is Prediction.WaterNow -> append(", needs water now")
        is Prediction.Eta -> append(", water in about ${p.days.toInt()} days")
        else -> Unit
    }
}

/**
 * What the room did, next to what the pot did.
 *
 * Sits beside the diagnostic rather than replacing it. The two answer different
 * questions - the diagnostic asks what might be wrong with the plant, this asks
 * whether anything is wrong at all - and [AmbientVerdict.ROOM_UNCHANGED] is the
 * one that makes the diagnostic worth acting on, by ruling out the boring
 * explanation.
 *
 * Nothing here is a warning. A room getting warmer is not a failure, and a pot
 * responding to it is the plant working correctly.
 */
@Composable
private fun AmbientCard(e: AmbientExplanation) {
    val faster = e.dryingRatio > 1.0
    val pace = if (faster) "faster" else "slower"
    val pct = kotlin.math.abs((e.dryingRatio - 1.0) * 100).toInt()

    val room = buildList {
        e.temperatureDeltaC?.let {
            if (kotlin.math.abs(it) >= 1.0) {
                add("%.0f °C %s".format(kotlin.math.abs(it), if (it > 0) "warmer" else "cooler"))
            }
        }
        e.humidityDeltaPercent?.let {
            if (kotlin.math.abs(it) >= 1.0) {
                add("%.0f%% %s".format(kotlin.math.abs(it), if (it > 0) "more humid" else "drier"))
            }
        }
    }.joinToString(" and ")

    Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(
                when (e.verdict) {
                    AmbientVerdict.EXPLAINS_FASTER,
                    AmbientVerdict.EXPLAINS_SLOWER -> "The room explains this"
                    AmbientVerdict.ROOM_UNCHANGED -> "The room has not changed"
                    AmbientVerdict.CONTRADICTS -> "The room does not explain this"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                when (e.verdict) {
                    AmbientVerdict.EXPLAINS_FASTER,
                    AmbientVerdict.EXPLAINS_SLOWER ->
                        "Drying about $pct% $pace than usual, and where it lives is $room " +
                            "than it was. That is the pot behaving normally in a changed room."
                    AmbientVerdict.ROOM_UNCHANGED ->
                        "Drying about $pct% $pace than usual, and the room is much as it was. " +
                            "Whatever changed, it was not the weather."
                    AmbientVerdict.CONTRADICTS ->
                        "Drying about $pct% $pace than usual, but the room is $room - which " +
                            "would push it the other way. Worth a closer look at the pot."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (e.source == AmbientSource.WEATHER) {
                Text(
                    "Based on outdoor weather, which is not the same as the room.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
