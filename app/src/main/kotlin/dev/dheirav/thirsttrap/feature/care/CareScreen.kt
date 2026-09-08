package dev.dheirav.thirsttrap.feature.care

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareScreen(onBack: () -> Unit, viewModel: CareViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val applied by viewModel.applied.collectAsStateWithLifecycle()
    val care = state.care

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(care?.name ?: "Care notes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (care == null) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Nothing on file for this one", style = MaterialTheme.typography.titleMedium)
                Text(
                    "The species field is blank, or it is not something the built-in notes " +
                        "cover. Nothing here invents advice it does not have.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    "Your own log will outgrow generic advice anyway - a few weighings say " +
                        "more about this pot than any care sheet can.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            care.botanical?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Section("Light", care.light)
            Section("Water", care.water)
            care.humidity?.let { Section("Humidity", it) }
            care.toxicity?.let { Section("Pets", it) }

            if (care.commonProblems.isNotEmpty()) {
                Text(
                    "What usually goes wrong",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp),
                )
                care.commonProblems.forEach {
                    Text("· $it", style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }

            care.note?.let {
                Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Text(
                "Use these as this plant's settings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Sets how dry it should get before watering to ${(care.depletionTrigger * 100).toInt()}%" +
                    " - the one number there is no way to guess - along with the light and " +
                    "watering notes above.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            FilledTonalButton(
                onClick = viewModel::applySuggestions,
                enabled = !applied && !state.alreadyApplied,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        applied -> "Applied"
                        state.alreadyApplied -> "Already using these"
                        else -> "Apply to ${state.plant?.name.orEmpty()}"
                    },
                )
            }

            Text(
                "General guidance for the species, not for your pot. Once you have weighed " +
                    "this one a few times, its own drying curve is the better answer - that " +
                    "is the whole point of the weight screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp),
    )
    Text(body, style = MaterialTheme.typography.bodyMedium)
}
