package dev.dheirav.thirsttrap.feature.weight

import dev.dheirav.thirsttrap.ui.AppIcons
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalMaterial3Api::class)
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

        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            if (!s.isCalibrated) {
                NotCalibratedCard(
                    needsRecalibration = s.plant.needsRecalibration,
                    onStart = { showCalibration = true },
                )
            } else {
                PredictionHeadline(s)
                Spacer(Modifier.height(12.dp))
                DepletionBar(s)
                Spacer(Modifier.height(16.dp))
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
                    WeightChart(
                        s,
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .semantics { contentDescription = chartSummary(s) },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            Text("Weigh it", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                if (entry.isBlank()) "grams, pot and all" else "$entry g",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            hint?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                listOf(
                    ReadingContext.ROUTINE to "just checking",
                    ReadingContext.PRE_WATER to "before watering",
                    ReadingContext.POST_WATER to "after watering",
                ).forEach { (c, label) ->
                    FilterChip(
                        selected = context == c,
                        onClick = { viewModel.onContext(c) },
                        label = { Text(label) },
                    )
                }
            }

            Keypad(
                onDigit = viewModel::onDigit,
                onBackspace = viewModel::onBackspace,
            )

            Button(
                onClick = {
                    if (!s.isCalibrated) showCalibration = true else viewModel.save {}
                },
                enabled = entry.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(56.dp),
            ) {
                Text(if (s.isCalibrated) "Save this weight" else "Use as the watered weight")
            }

            if (s.readings.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("Readings", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tap one to exclude a bad weigh-in - a pot half off the scale, or a " +
                        "wet saucer. It stays in the record but stops skewing the curve.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                val shown = s.readings.sortedByDescending { it.timestampMillis }.take(20)
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${r.grams.toInt()} g",
                            // Strikethrough as well as tone: excluded must not
                            // be signalled by colour alone.
                            textDecoration = if (r.excluded) TextDecoration.LineThrough else null,
                            color = if (r.excluded) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            buildString {
                                append(readingDate(r.timestampMillis, r.tzOffsetMinutes))
                                append(" · ")
                                append(if (r.excluded) "excluded" else r.context.label)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (s.readings.size > shown.size) {
                    Text(
                        "Showing the most recent ${shown.size} of ${s.readings.size}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

/** A fraction between the anchors, never raw grams - docs/WATERING-MODEL.md section 8. */
@Composable
private fun DepletionBar(s: WeightState) {
    // NaN is reachable when the wet and dry anchors collapse onto each other.
    // It renders as a confident "0%" and goes into fillMaxWidth, so it is
    // caught here rather than shown.
    val d = s.depletion?.takeIf { it.isFinite() } ?: return
    val trigger = s.plant.depletionTrigger
    val past = s.prediction is Prediction.WaterNow
    Column {
        Box(Modifier.fillMaxWidth().height(14.dp).clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxWidth(d.coerceIn(0.0, 1.0).toFloat()).height(14.dp)
                .background(MaterialTheme.colorScheme.primary))
            Box(Modifier.fillMaxWidth(trigger.toFloat()).height(14.dp), contentAlignment = Alignment.CenterEnd) {
                Box(Modifier.size(width = 2.dp, height = 14.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant))
            }
        }
        Text(
            if (past) "${(d * 100).toInt()}% - past its ${(trigger * 100).toInt()}% mark"
            else "${(d * 100).toInt()}% of the way to needing water",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
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
            FilledTonalButton(onClick = onStart) { Text("Set the watered weight") }
        }
    }
}

@Composable
private fun Keypad(onDigit: (Char) -> Unit, onBackspace: () -> Unit) {
    // Big targets: this gets used standing at a windowsill holding a pot.
    val rows = listOf("123", "456", "789", ".0<")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { ch ->
                    FilledTonalButton(
                        onClick = { if (ch == '<') onBackspace() else onDigit(ch) },
                        modifier = Modifier.weight(1f).height(60.dp),
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
        title = { Text("Set the watered weight") },
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
    val anchors = s.plant.anchors ?: return
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

        val lo = min(anchors.dryGrams, points.minOf { it.grams })
        val hi = max(anchors.wetGrams, points.maxOf { it.grams })
        val spanG = max(1.0, hi - lo)

        // Inset, or the topmost point and the first and last are half-clipped
        // by the canvas edge.
        val padY = 8.dp.toPx()
        val plotH = size.height - padY * 2
        val padX = 6.dp.toPx()
        val plotW = size.width - padX * 2

        fun x(t: Long) = padX + (days(t) / spanD * plotW).toFloat()
        fun y(g: Double) = padY + (plotH - ((g - lo) / spanG * plotH)).toFloat()

        drawLine(outline, Offset(0f, y(anchors.wetGrams)), Offset(size.width, y(anchors.wetGrams)),
            1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
        drawLine(outline, Offset(0f, y(anchors.dryGrams)), Offset(size.width, y(anchors.dryGrams)),
            1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
        val triggerG = anchors.triggerWeight(s.plant.depletionTrigger)
        drawLine(tertiary, Offset(0f, y(triggerG)), Offset(size.width, y(triggerG)),
            1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))

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
        if (slope != null && slope < 0 && last != null) {
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
