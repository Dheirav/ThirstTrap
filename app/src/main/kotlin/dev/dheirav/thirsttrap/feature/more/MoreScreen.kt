package dev.dheirav.thirsttrap.feature.more

import dev.dheirav.thirsttrap.ui.Flank
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.IconButton
import dev.dheirav.thirsttrap.ui.MenuLabels
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.Space

/**
 * The six jobs that are not about one plant, with a line each saying what they are.
 *
 * This was a dropdown of six bare words. "Figures" and "Places" tell a reader
 * nothing, and a menu is somewhere you look only once you already know what is
 * in it, so the features behind it were effectively undiscoverable to anyone
 * who had not been told.
 *
 * It costs no extra taps: the overflow icon opens this instead of a menu, so it
 * is still two taps to reach any of them, and now the first of those two says
 * what you are choosing between. The sentences live in MenuLabels beside the
 * labels, because the wayfinding page needs the same ones and this file already
 * records what happens when a label exists in two copies.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    advanced: Boolean,
    onBack: () -> Unit,
    onOpenFeeding: () -> Unit,
    onOpenPropagation: () -> Unit,
    onScanPot: () -> Unit,
    onOpenPlaces: () -> Unit,
    onOpenFigures: () -> Unit,
    onOpenExperiments: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle("More") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = Space.Block),
        ) {
            // Jobs first, then the record. The same split the menu had, kept
            // because it is the right one: doing something, then looking at
            // what was done.
            Job(MenuLabels.Dashboard.FEEDING, MenuLabels.Dashboard.FEEDING_WHAT, onOpenFeeding)
            Job(
                MenuLabels.Dashboard.PROPAGATION,
                MenuLabels.Dashboard.PROPAGATION_WHAT,
                onOpenPropagation,
            )
            if (advanced) {
                Job(MenuLabels.Dashboard.SCAN, MenuLabels.Dashboard.SCAN_WHAT, onScanPot)
            }
            Job(MenuLabels.Dashboard.PLACES, MenuLabels.Dashboard.PLACES_WHAT, onOpenPlaces)
            Job(MenuLabels.Dashboard.FIGURES, MenuLabels.Dashboard.FIGURES_WHAT, onOpenFigures)
            if (advanced) {
                Job(
                    MenuLabels.Dashboard.EXPERIMENTS,
                    MenuLabels.Dashboard.EXPERIMENTS_WHAT,
                    onOpenExperiments,
                )
            }
            // Say that the hidden ones exist, rather than hiding the fact too.
            //
            // Scanning a sticker and the experiment board are gated on the
            // specialist-tools switch, which is off on a fresh install. That
            // gate is right: neither is any use on day one. What was wrong is
            // that nothing anywhere said they were there, so two screens and a
            // whole feature had no reachable entry point at all unless you
            // happened to scroll Settings and read a switch.
            //
            // A line is not a menu item. It does not compete with the six jobs
            // above it, it cannot be tapped by mistake, and it disappears the
            // moment the switch is on.
            if (!advanced) {
                Rule(flank = Flank.Section)
                Text(
                    "Two more, scanning a pot sticker and the experiment board, are " +
                        "off until you turn on the specialist tools in Settings. They " +
                        "are not much use until you have a few plants on the go.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

/** A row, like every other list in this app. Not a filled button. */
@Composable
private fun Job(title: String, what: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClickLabel = title) { onClick() }
            .padding(vertical = Space.Entry),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            what,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Space.Tight),
        )
    }
    Rule()
}
