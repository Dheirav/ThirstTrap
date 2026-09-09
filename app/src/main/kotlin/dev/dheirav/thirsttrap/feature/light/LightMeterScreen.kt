package dev.dheirav.thirsttrap.feature.light

import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import dev.dheirav.thirsttrap.ui.Button
import dev.dheirav.thirsttrap.ui.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.domain.LightFit
import dev.dheirav.thirsttrap.domain.LightLevel
import dev.dheirav.thirsttrap.ui.Rule

/** Feature F22. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightMeterScreen(onBack: () -> Unit, viewModel: LightMeterViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plant by viewModel.plant.collectAsStateWithLifecycle()
    val target by viewModel.target.collectAsStateWithLifecycle()
    val placeMode = viewModel.isPlaceMode
    val lifecycleOwner = LocalLifecycleOwner.current

    // The sensor runs only while this screen is actually in front.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.start()
                Lifecycle.Event.ON_PAUSE -> viewModel.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stop()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle("Light here") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!state.hasSensor) {
                Text(
                    "This phone has no light sensor",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 48.dp),
                )
                Text(
                    "Nothing to measure with. Your own eyes are a reasonable substitute: " +
                        "if you can read comfortably without a lamp, it is moderate light.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                return@Column
            }

            target?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Text(
                if (placeMode) {
                    "Hold the phone where a pot would stand, screen facing the way the " +
                        "leaves would."
                } else {
                    "Hold the phone where the plant sits, screen facing the same way the " +
                        "leaves do."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Text(
                state.lux?.let { "${it.toInt()}" } ?: "—",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(top = 32.dp),
            )
            Text("lux", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            state.level?.let { level ->
                Spacer(Modifier.height(24.dp))
                LevelBar(level)
                Spacer(Modifier.height(16.dp))
                Text(
                    level.label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    level.blurb,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            // Only speaks when the plant's own noted needs make it meaningful.
            state.verdict?.let {
                Card(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                    Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
                }
            }

            if (placeMode && state.here.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                    Rule()
                    state.here.forEach { resident ->
                        androidx.compose.foundation.layout.Row(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                resident.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                when (resident.fit) {
                                    LightFit.SUITS -> "suits it"
                                    LightFit.TOO_DARK -> "too dark"
                                    LightFit.TOO_BRIGHT -> "too bright"
                                    // Nothing noted about what this one wants,
                                    // so the app has no business guessing.
                                    null -> "not noted"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (resident.fit == null) {
                                    MaterialTheme.colorScheme.outline
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        Rule()
                    }
                }
            }

            if (placeMode && state.here.isEmpty() && state.lux != null) {
                Text(
                    "Nothing lives here yet, which is usually why you are measuring it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }

            if (!placeMode && plant?.lightNeeds.isNullOrBlank()) {
                Text(
                    "Note what this plant wants in its details, and this screen will say " +
                        "whether the spot suits it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::save,
                enabled = state.lux != null && !state.saved,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        state.saved && placeMode -> "Saved to this place"
                        state.saved -> "Saved to its history"
                        else -> "Save this reading"
                    },
                )
            }

            Text(
                "Phone light sensors are not calibrated and differ between handsets, so " +
                    "treat the band as the answer and the number as a hint.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

@Composable
private fun LevelBar(level: LightLevel) {
    val all = LightLevel.entries
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        all.forEach { l ->
            Box(
                Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(
                        if (l == level) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
            )
        }
    }
}
