package dev.dheirav.thirsttrap.feature.propagation

import dev.dheirav.thirsttrap.ui.Space

import dev.dheirav.thirsttrap.ui.OutlinedButton
import dev.dheirav.thirsttrap.ui.ColumnHead
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.AppIcons
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import dev.dheirav.thirsttrap.ui.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import dev.dheirav.thirsttrap.ui.IconButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.domain.PropagationCard
import dev.dheirav.thirsttrap.domain.PropagationStage
import dev.dheirav.thirsttrap.domain.stageIsStale

/**
 * Requirements item 20.
 *
 * One page that scrolls down, not five columns that scroll sideways. A board
 * of columns is a desktop shape: on a phone it showed one 260dp column at a
 * time, so the pipeline it exists to display was the one thing you could not
 * see, and a card list inside a sideways-scrolling row fights the gesture the
 * rest of the app uses for everything.
 *
 * Read top to bottom the stages are in order, which is the same information
 * the columns were carrying, in the direction a phone is held.
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
                title = { ScreenTitle("Propagation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.loaded && state.isEmpty) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(Space.Page),
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
                    modifier = Modifier.padding(top = Space.Line),
                )
            }
            return@Scaffold
        }

        // Flattened into one list rather than a LazyColumn per stage: a lazy
        // list inside a lazy list of the same orientation has no height to
        // measure against, so the stages become items and their cards follow.
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = Space.Block, vertical = Space.Entry),
            verticalArrangement = Arrangement.spacedBy(Space.Line),
        ) {
            PropagationStage.entries.forEach { stage ->
                val cards = state.columns[stage].orEmpty()

                item(key = "head-${stage.name}") {
                    StageHead(label = stage.label, count = cards.size, hint = stage.hint)
                }

                if (cards.isEmpty()) {
                    item(key = "empty-${stage.name}") {
                        Text(
                            "Nothing at this stage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }

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

/**
 * A stage divider: what this stage is, how many are in it, what it means.
 *
 * Letterspaced caps over a hairline is the app's section grammar, and it is
 * what replaces the column edge now that the stages are stacked. The count
 * sits on the same line because "4" under "WATER" reads as a card.
 */
@Composable
private fun StageHead(label: String, count: Int, hint: String) {
    Column(Modifier.fillMaxWidth().padding(top = Space.Entry)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColumnHead(label, modifier = Modifier.weight(1f))
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Rule()
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Space.Line),
        )
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

    // A row, not a box with the arrows parked underneath. In a 260dp column
    // the stacked layout was the only thing that fitted; at full width it
    // became a tall card with the plant's name in one corner and an arrow in
    // the other, and a page of those reads as five empty boxes.
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(
            Modifier.padding(start = Space.Entry, end = Space.Tight, top = Space.Line, bottom = Space.Line),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
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
                    // A nudge, not a telling-off. A cutting that has sat in
                    // water for six weeks may be perfectly fine.
                    Text(
                        "Worth a look",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = Space.Hair),
                    )
                }
            }
            // These move a cutting between stages, and they were the only two
            // of 32 IconButtons not in a top bar: a bare arrowBack 20dp from
            // the real back arrow, meaning something else entirely. Naming the
            // stage says what the tap does, which an arrow cannot.
            card.stage.previous?.let { prev ->
                OutlinedButton(onClick = onBack) { Text(prev.label) }
            }
            card.stage.next?.let { next ->
                OutlinedButton(onClick = onForward) { Text(next.label) }
            }
        }
    }
}
