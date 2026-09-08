package dev.dheirav.thirsttrap.feature.timelapse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.domain.isPlayable
import dev.dheirav.thirsttrap.domain.spanDays
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.DoubleRule
import dev.dheirav.thirsttrap.ui.OutlinedButton
import dev.dheirav.thirsttrap.ui.PlantPhoto
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.ScreenTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val plateDate = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())

/**
 * A plant's photo history as a series of plates.
 *
 * Requirement 18 wants a "scrubbable timelapse". The scrubber is the feature -
 * this renders no video and writes no file, because dragging a finger already
 * does the thing, and an encoder would be the first part of the app that could
 * fail silently on somebody's device.
 *
 * Laid out the way a plate series is printed: the figure, a rule, then the
 * caption. The caption counts days rather than frames, because "day 41" is the
 * fact somebody wants from a plant diary and "photo 3 of 9" is not.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelapseScreen(onBack: () -> Unit, viewModel: TimelapseViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val index by viewModel.index.collectAsStateWithLifecycle()
    val playing by viewModel.playing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle("Timelapse") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val frames = state.frames

        if (!frames.isPlayable()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (frames.isEmpty()) "No photos yet" else "Only one photo so far",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "A timelapse needs at least two. Photograph the plant from roughly the " +
                        "same spot each time and the difference shows up on its own.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            return@Scaffold
        }

        val current = frames[index.coerceIn(0, frames.lastIndex)]

        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // The plate. Fixed aspect so the frame does not jump as the photos
            // change shape - a timelapse whose border moves is unreadable.
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .semantics {
                        contentDescription =
                            "${current.dayLabel}, photo ${current.index + 1} of ${frames.size}"
                    },
                contentAlignment = Alignment.Center,
            ) {
                PlantPhoto(
                    path = viewModel.pathOf(current.photo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Rule(Modifier.padding(top = 10.dp))
            Text(
                "Fig. ${current.index + 1} - ${current.dayLabel}",
                style = MaterialTheme.typography.titleSmall,
                letterSpacing = 0.08.em,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                plateDate.format(Date(current.photo.takenAtMillis)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            current.photo.caption?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            DoubleRule(Modifier.padding(top = 14.dp, bottom = 10.dp))

            Slider(
                value = current.index.toFloat(),
                onValueChange = { viewModel.scrubTo(it.toInt()) },
                valueRange = 0f..frames.lastIndex.toFloat(),
                steps = (frames.size - 2).coerceAtLeast(0),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = "Scrub through ${frames.size} photos"
                },
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val span = frames.spanDays()
                Text(
                    "${frames.size} photos over " +
                        if (span == 1) "1 day" else "$span days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                OutlinedButton(onClick = viewModel::togglePlay) {
                    Text(if (playing) "Stop" else "Play")
                }
            }
        }
    }
}
