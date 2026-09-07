package dev.dheirav.thirsttrap.feature.propagation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.domain.PropagationCard
import dev.dheirav.thirsttrap.domain.PropagationStage
import dev.dheirav.thirsttrap.domain.stageIsStale

/**
 * Requirements item 20. Columns scroll sideways rather than cramming five
 * stages onto a phone width; each column is readable on its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropagationScreen(
    onBack: () -> Unit,
    onOpenPlant: (String) -> Unit,
    viewModel: PropagationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Propagation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.loaded && state.isEmpty) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No cuttings on the go", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Add a plant and set where it came from to \"cutting\", and it will " +
                        "appear here to be tracked from cut to established.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            return@Scaffold
        }

        LazyRow(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(PropagationStage.entries, key = { it.name }) { stage ->
                val cards = state.columns[stage].orEmpty()
                Column(Modifier.width(260.dp)) {
                    Text(
                        "${stage.label}  ${cards.size}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stage.hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(cards, key = { it.plant.id }) { card ->
                            CuttingCard(
                                card = card,
                                nowMillis = now,
                                onOpen = { onOpenPlant(card.plant.id) },
                                onForward = { stage.next?.let { viewModel.move(card, it) } },
                                onBack = { stage.previous?.let { viewModel.move(card, it) } },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CuttingCard(
    card: PropagationCard,
    nowMillis: Long,
    onOpen: () -> Unit,
    onForward: () -> Unit,
    onBack: () -> Unit,
) {
    val days = card.daysInStage(nowMillis)
    val stale = days != null && stageIsStale(card.stage, days)

    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.padding(12.dp)) {
            Text(card.plant.name, fontWeight = FontWeight.SemiBold)
            Text(
                when {
                    days == null -> "just added"
                    days == 0 -> "moved here today"
                    days == 1 -> "1 day here"
                    else -> "$days days here"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (stale) {
                // A nudge, not a telling-off. A cutting that has sat in water
                // for six weeks may be perfectly fine.
                Text(
                    "Worth a look",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                if (card.stage.previous != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Move back to ${card.stage.previous!!.label}",
                        )
                    }
                }
                if (card.stage.next != null) {
                    IconButton(onClick = onForward) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Move on to ${card.stage.next!!.label}",
                        )
                    }
                }
            }
        }
    }
}
