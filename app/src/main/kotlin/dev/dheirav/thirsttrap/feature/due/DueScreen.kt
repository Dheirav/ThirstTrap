package dev.dheirav.thirsttrap.feature.due

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The in-app due list. It exists because HyperOS may kill the notification
 * entirely - if that happens the information must still be somewhere.
 *
 * Nothing here counts failures. No red, no streaks, no "you missed 4 days".
 * Overdue is not an error state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueScreen(
    onOpenHelp: () -> Unit,
    viewModel: DueViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Due") }) }) { padding ->
        if (state.loaded && state.items.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Nothing to check", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "You're up to date. Reminders appear here when a plant is worth a look.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onOpenHelp) { Text("Reminders not arriving?") }
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.items, key = { it.reminder.id }) { item ->
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Time to check the ${item.plant.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (item.overdueDays > 0) {
                                "It's been ${item.overdueDays} days. Lift the pot - does it feel light?"
                            } else {
                                "Lift the pot - does it feel light?"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                        )
                        // Equal weight, side by side. Neither is the primary.
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalButton(
                                onClick = { viewModel.watered(item) },
                                modifier = Modifier.weight(1f).height(56.dp),
                            ) { Text("Watered") }
                            FilledTonalButton(
                                onClick = { viewModel.stillWet(item) },
                                modifier = Modifier.weight(1f).height(56.dp),
                            ) { Text("Still wet") }
                        }
                        TextButton(onClick = { viewModel.snooze(item) }) { Text("Snooze a day") }
                    }
                }
            }
            if (state.items.size > 1) {
                item {
                    TextButton(onClick = { viewModel.clearAllOverdue() }) {
                        Text("Clear all overdue")
                    }
                }
            }
            item { TextButton(onClick = onOpenHelp) { Text("Reminders not arriving?") } }
        }
    }
}
