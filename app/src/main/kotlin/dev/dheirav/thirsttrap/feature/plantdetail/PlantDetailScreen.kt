package dev.dheirav.thirsttrap.feature.plantdetail

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * The per-plant timeline. Requirements item 3.
 *
 * Without this you can log things but never look at what you logged, which
 * makes "was this better than a paper note?" unanswerable.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlantDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: PlantDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plant = state.plant

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plant?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    plant?.let { p ->
                        IconButton(onClick = { onEdit(p.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit plant")
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                Column(Modifier.padding(bottom = 16.dp)) {
                    plant?.species?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        listOfNotNull(
                            plant?.location?.takeIf { it.isNotBlank() },
                            plant?.medium?.name?.lowercase()?.replace('_', ' '),
                            plant?.containerDesc?.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Requirements item 8. Only shown once there are two
                    // waterings to measure between - one is not a cadence.
                    state.averageIntervalDays?.let { avg ->
                        Text(
                            "Waters roughly every ${avg.toInt()} days",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Text(
                        "${state.totalEvents} ${if (state.totalEvents == 1) "entry" else "entries"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            if (state.loaded && state.days.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("No history yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Log a watering or a check and it will show up here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            state.days.forEach { day ->
                stickyHeader(key = day.label) {
                    Box(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                            .padding(vertical = 8.dp),
                    ) {
                        Text(
                            day.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                items(day.events, key = { it.id }) { event ->
                    EventRow(event = event, onDelete = { viewModel.deleteEvent(event) })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventRow(event: CareEvent, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }

    // Events that changed the plant's nature get a heavier treatment, so they
    // are findable while scrolling fast. docs/UI-SPEC.md section 4.
    val isLifeEvent = event.type in setOf(
        CareEventType.REPOTTED, CareEventType.MEDIUM_CHANGED, CareEventType.DIED,
    )

    Column {
        if (isLifeEvent) HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.tertiary)

        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = { menu = true })
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                // Order matters: size() then padding() shrinks the box to 8x2 and
                // the marker renders as a dash instead of a dot.
                Modifier.padding(top = 6.dp).size(8.dp)
                    .background(
                        if (isLifeEvent) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(4.dp),
                    ),
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    label(event),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isLifeEvent) FontWeight.SemiBold else FontWeight.Normal,
                )
                event.note?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                timeOf(event),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Delete this entry") },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}

private fun label(event: CareEvent): String = when (event.type) {
    CareEventType.WATERED -> buildString {
        append("Watered")
        event.amountMl?.let { append(" · ${it.toInt()} ml") }
    }
    CareEventType.CHECKED -> "Checked - still wet"
    CareEventType.FERTILIZED -> "Fertilised"
    CareEventType.WATER_CHANGED -> "Water changed"
    CareEventType.REPOTTED -> "Repotted"
    CareEventType.MEDIUM_CHANGED -> "Medium changed"
    CareEventType.PRUNED -> "Pruned"
    CareEventType.TREATED -> "Treated"
    CareEventType.PEST_OR_DISEASE -> "Pest or disease"
    CareEventType.WEEDED -> "Weeded"
    CareEventType.OBSERVATION -> "Observation"
    CareEventType.MILESTONE -> "Milestone"
    CareEventType.MOVED -> "Moved"
    CareEventType.DIED -> "Died"
    CareEventType.UNKNOWN -> "Logged"
}

/** Rendered in the offset the event was recorded in, not the device's current one. */
private fun timeOf(event: CareEvent): String {
    val zone = ZoneOffset.ofTotalSeconds(event.tzOffsetMinutes * 60)
    return Instant.ofEpochMilli(event.timestampMillis).atZone(zone)
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}
