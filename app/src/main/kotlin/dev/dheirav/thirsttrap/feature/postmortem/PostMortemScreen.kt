package dev.dheirav.thirsttrap.feature.postmortem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.ui.PlantPhoto
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Requirements item 24.
 *
 * The record usually already knows what happened, and nobody reads a log
 * unprompted. This is the one moment someone is willing to look - and the
 * least likely to remember what they did two months ago.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostMortemScreen(onDone: () -> Unit, viewModel: PostMortemViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recap = state.recap

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("What happened") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                state.plant?.name.orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Nothing gets deleted. It moves to your archive with everything it has, " +
                    "so the next one of these can be read against it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.photos.isNotEmpty()) {
                Text("How it looked", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.photos.sortedBy { it.takenAtMillis }, key = { it.id }) { photo ->
                        PlantPhoto(
                            path = viewModel.pathOf(photo),
                            contentDescription = photo.caption ?: "Photo from its history",
                            modifier = Modifier.size(96.dp).clip(MaterialTheme.shapes.small),
                        )
                    }
                }
            }

            recap?.let { r ->
                HorizontalDivider()
                Text("The record", style = MaterialTheme.typography.titleSmall)
                Text(
                    buildString {
                        r.daysOwned?.let { append("Logged for $it days. ") }
                        append("${r.totalWaterings} waterings")
                        r.averageIntervalDays?.let { append(", roughly every ${it.toInt()} days") }
                        append(".")
                        r.lastWateredDaysAgo?.let { append(" Last watered $it days ago.") }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )

                // Drawn from the log, never a verdict. The record can say a plant
                // was watered every three days; it cannot say that killed it.
                r.observations.forEach {
                    Card(Modifier.fillMaxWidth()) {
                        Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                    }
                }

                if (r.keyEvents.isNotEmpty()) {
                    Text("Things that changed", style = MaterialTheme.typography.titleSmall)
                    r.keyEvents.forEach { e ->
                        Text(
                            "${dateOf(e.timestampMillis, e.tzOffsetMinutes)} · ${e.type.label}" +
                                (e.note?.let { " - $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            HorizontalDivider()

            OutlinedTextField(
                value = state.cause,
                onValueChange = viewModel::onCause,
                label = { Text("What do you think happened?") },
                placeholder = { Text("root rot, dried out while away, never really settled") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.differently,
                onValueChange = viewModel::onDifferently,
                label = { Text("What would you do differently?") },
                placeholder = { Text("smaller pot, check it weekly, keep it out of the afternoon sun") },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(onClick = { viewModel.record(onDone) }, modifier = Modifier.fillMaxWidth()) {
                Text("Save and archive")
            }
        }
    }
}

private fun dateOf(millis: Long, offsetMinutes: Int): String {
    val zone = ZoneOffset.ofTotalSeconds(offsetMinutes * 60)
    return Instant.ofEpochMilli(millis).atZone(zone)
        .format(DateTimeFormatter.ofPattern("d MMM"))
}
