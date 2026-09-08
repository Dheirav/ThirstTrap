package dev.dheirav.thirsttrap.feature.care

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalUriHandler
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
import dev.dheirav.thirsttrap.domain.CareDetail
import dev.dheirav.thirsttrap.domain.LookupFailure

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareScreen(onBack: () -> Unit, viewModel: CareViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val applied by viewModel.applied.collectAsStateWithLifecycle()
    val lookup by viewModel.lookup.collectAsStateWithLifecycle()
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

                LookupSection(
                    enabled = state.onlineLookupEnabled,
                    lookup = lookup,
                    onLookUp = viewModel::lookUpOnline,
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

            // A thin entry must not wear the authority of a thick one. The
            // bundled tier knows how bright, how wet and how humid, and nothing
            // about what actually kills the plant - so it says so.
            if (care.detail == CareDetail.BUNDLED) {
                Text(
                    "Basic notes, from the bundled plant dataset. Enough to set a starting " +
                        "point, not enough to tell you what usually goes wrong with this one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
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

            care.source?.let {
                Text(
                    "Source: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
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

/**
 * The online fallback, and the reason it is a fallback rather than a feature.
 *
 * This resolves a NAME. It never returns care advice, so the worst it can do is
 * link the wrong article - and when there is no network, or the user never
 * opted in, the screen says which, rather than spinning.
 */
@Composable
private fun LookupSection(
    enabled: Boolean,
    lookup: LookupState,
    onLookUp: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    if (!enabled) {
        Text(
            "Looking the name up online is switched off. Settings has a toggle for it - " +
                "it sends the species name and nothing else, and the rest of the app never " +
                "touches the network either way.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )
        return
    }

    when (lookup) {
        LookupState.Idle -> OutlinedButton(
            onClick = onLookUp,
            modifier = Modifier.padding(top = 24.dp),
        ) { Text("Look the name up online") }

        LookupState.Running -> CircularProgressIndicator(Modifier.padding(top = 24.dp))

        is LookupState.Found -> {
            val l = lookup.lookup
            Column(Modifier.padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(l.acceptedName, style = MaterialTheme.typography.titleMedium)
                l.family?.let {
                    Text("Family $it", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Usually the moment a user learns their plant has another name,
                // and the reason the catalogue looked empty.
                l.synonymOf?.let {
                    Text(
                        "You typed $it, which is an older name for it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                l.wikipediaExtract?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp))
                }
                l.wikipediaUrl?.let { url ->
                    AssistChip(
                        onClick = { uriHandler.openUri(url) },
                        label = { Text("Read on Wikipedia") },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                Text(
                    "Still no care notes for this one - this says what the plant is, not how " +
                        "to water it. Weigh the pot a few times and the app will know more " +
                        "about it than any care sheet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }

        is LookupState.Failed -> Text(
            when (lookup.reason) {
                LookupFailure.NOT_ENABLED -> "Online lookup is switched off in Settings."
                LookupFailure.OFFLINE -> "No network. Nothing else in the app needs one."
                LookupFailure.NO_MATCH -> "No plant by that name in the GBIF backbone. " +
                    "Check the spelling, or it may only have a common name."
                LookupFailure.SERVICE_ERROR -> "The lookup service did not answer. " +
                    "Nothing lost - try again whenever."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}
