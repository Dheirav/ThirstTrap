package dev.dheirav.thirsttrap.feature.care

import dev.dheirav.thirsttrap.ui.Flank

import dev.dheirav.thirsttrap.ui.Space

import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.SectionHead
import dev.dheirav.thirsttrap.ui.AppIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import dev.dheirav.thirsttrap.ui.OutlinedButton
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.verticalScroll
import dev.dheirav.thirsttrap.ui.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import dev.dheirav.thirsttrap.ui.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.domain.CareDetail
import dev.dheirav.thirsttrap.domain.LookupFailure
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareScreen(onBack: () -> Unit, viewModel: CareViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lookup by viewModel.lookup.collectAsStateWithLifecycle()
    val care = state.care

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(care?.name ?: "Care notes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (care == null) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(Space.Page),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Nothing on file for this one", style = MaterialTheme.typography.titleMedium)
                Text(
                    "The species field is blank, or it is not something the built-in " +
                        "notes cover.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Space.Line),
                )
                // "Your own log will outgrow generic advice anyway" used to sit
                // here, and "its own drying curve is the better answer" still
                // sits further down the same screen. One screen said it twice.

                LookupSection(
                    enabled = state.onlineLookupEnabled,
                    lookup = lookup,
                    onLookUp = viewModel::lookUpOnline,
                )
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Space.Block),
            verticalArrangement = Arrangement.spacedBy(Space.Tight),
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
                    "Basic notes, from the bundled plant dataset.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.Line),
                )
            }

            Section("Light", care.light)
            Section("Water", care.water)
            care.humidity?.let { Section("Humidity", it) }
            care.toxicity?.let { Section("Pets", it) }

            if (care.commonProblems.isNotEmpty()) {
                SectionHead("What usually goes wrong")
                care.commonProblems.forEach {
                    Text("· $it", style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = Space.Tight))
                }
            }

            care.note?.let {
                Card(Modifier.fillMaxWidth().padding(top = Space.Entry)) {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Rule(flank = Flank.Section)

            SectionHead("Use these as this plant's settings")
            Text(
                "Sets watering at ${(care.depletionTrigger * 100).roundToInt()}% dry, " +
                    "along with the light and watering notes above.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Space.Line),
            )
            Button(
                onClick = { viewModel.applySuggestions(onBack) },
                enabled = !state.alreadyApplied,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (state.alreadyApplied) {
                        "Already using these"
                    } else {
                        "Apply to ${state.plant?.name.orEmpty()}"
                    },
                )
            }

            Text(
                "General guidance for the species, not for your pot. Once you have " +
                    "weighed this one a few times, its own drying curve is the better answer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.Section),
            )

            care.source?.let {
                Text(
                    "Source: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.Line),
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
        modifier = Modifier.padding(top = Space.Entry),
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
            "Looking the name up online is switched off. Settings has a toggle for it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Space.Section),
        )
        return
    }

    when (lookup) {
        LookupState.Idle -> OutlinedButton(
            onClick = onLookUp,
            modifier = Modifier.padding(top = Space.Section),
        ) { Text("Look the name up online") }

        LookupState.Running -> CircularProgressIndicator(Modifier.padding(top = Space.Section))

        is LookupState.Found -> {
            val l = lookup.lookup
            Column(Modifier.padding(top = Space.Section), horizontalAlignment = Alignment.CenterHorizontally) {
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
                        modifier = Modifier.padding(top = Space.Tight),
                    )
                }
                l.wikipediaExtract?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = Space.Entry))
                }
                l.wikipediaUrl?.let { url ->
                    AssistChip(
                        onClick = { uriHandler.openUri(url) },
                        label = { Text("Read on Wikipedia") },
                        modifier = Modifier.padding(top = Space.Entry),
                    )
                }
                Text(
                    "Still no care notes for this one. This says what the plant is, not " +
                        "how to water it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Space.Block),
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
            modifier = Modifier.padding(top = Space.Section),
        )
    }
}
