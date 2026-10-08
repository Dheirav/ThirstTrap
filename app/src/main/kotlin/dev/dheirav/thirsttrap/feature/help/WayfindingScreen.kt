package dev.dheirav.thirsttrap.feature.help

import dev.dheirav.thirsttrap.ui.Space

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.dheirav.thirsttrap.ui.MenuLabels
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.SectionHead

/**
 * What is inside the two menus, what the photo viewer can do, and the two
 * gestures nothing on screen can tell you about.
 *
 * It used to open with the three tabs and a map of the app. The guided tour
 * does that now, and does it better, because it points at the real control
 * instead of naming it. So the orientation half is gone and what is left is the
 * half a tour cannot carry: a spotlight can say "everything about this plant is
 * behind these dots" but it cannot list the eight things that are there without
 * becoming eight steps nobody would sit through.
 *
 * The gesture table at the bottom is the part that earns its place, and the
 * tour structurally cannot replace it: a tap-through step asks for a tap. Every
 * other route in the app is a plain tap and therefore learnable by trying,
 * which was the point of D48. Two long presses survive as shortcuts, and a
 * shortcut nobody knows about is not a shortcut. docs/NAVIGATION.md section 7.
 */
@Composable
fun WayfindingScreen(onBack: () -> Unit) {
    HelpPage("What is in the menus", onBack) {
        HelpPara(
            "Almost everything lives on the plant it belongs to. The rest is behind " +
                "one of these two menus.",
        )

        SectionHead("Behind the dots on the Plants tab")
        Item(MenuLabels.Dashboard.FEEDING, MenuLabels.Dashboard.FEEDING_WHAT)
        Item(MenuLabels.Dashboard.PROPAGATION, MenuLabels.Dashboard.PROPAGATION_WHAT)
        Item(MenuLabels.Dashboard.SCAN, MenuLabels.Dashboard.SCAN_WHAT)
        Item(MenuLabels.Dashboard.PLACES, MenuLabels.Dashboard.PLACES_WHAT)
        Item(MenuLabels.Dashboard.FIGURES, MenuLabels.Dashboard.FIGURES_WHAT)
        Item(MenuLabels.Dashboard.EXPERIMENTS, MenuLabels.Dashboard.EXPERIMENTS_WHAT)

        SectionHead("Behind the dots on one plant")
        Item(MenuLabels.Plant.LOG, "A watering, check, repot or feed, with a date.")
        Item(MenuLabels.Plant.GALLERY, "A photo you already took, rather than a new one.")
        Item(MenuLabels.Plant.DIAGNOSE, "What the log and the weight can and cannot tell you, and what to rule out first.")
        Item(MenuLabels.Plant.WEIGHT, "Weigh the pot and get a date when there is one worth giving. The reason the app exists.")
        Item(MenuLabels.Plant.LIGHT, "A light-sensor reading, filed against the place.")
        Item(MenuLabels.Plant.LOOKUP, "Only when there are no care notes on file.")
        Item(MenuLabels.Plant.STICKER, "Advanced features only. A QR label for the pot, so scanning it opens this plant.")
        Item(MenuLabels.Plant.EDIT, "Name, species, pot, how dry it is allowed to get.")

        SectionHead("Photos")
        HelpPara(
            "Tap a photo to open it full screen. Everything photos can do is in " +
                "there: swipe between them, Oldest and Latest for either end, Play " +
                "to run through them in order. Its dots menu pins one against " +
                "another with \"Compare with this\", and shares or saves a copy.",
        )

        SectionHead("Two gestures worth knowing")
        HelpPara(
            "Everything opens on a plain tap, so you can learn the app by trying " +
                "it. Two exceptions, both shortcuts to something a tap also reaches:",
        )
        Gesture(
            "Hold a plant on the Plants tab",
            "The quick sheet: water, still wet, photo, weigh, edit. All of it is " +
                "also on the plant's page.",
        )
        Gesture(
            "Hold the water drop on a plant",
            "Asks for the amount and how you watered, instead of logging your usual.",
        )

        HelpPara(
            "Nothing else hides behind a hold, and nothing deletes without asking.",
        )
        HelpPageEnd()
    }
}


/** A name and what it is for, which is the only thing a map has to say. */
@Composable
private fun Item(name: String, what: String) {
    Row(Modifier.fillMaxWidth().padding(top = Space.Entry), horizontalArrangement = Arrangement.Start) {
        Column {
            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                what,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Set apart from the list items, because this is the half nobody can guess. */
@Composable
private fun Gesture(action: String, result: String) {
    Column(Modifier.fillMaxWidth().padding(top = Space.Entry)) {
        Rule()
        Text(
            action,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = Space.Line),
        )
        Text(
            result,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Space.Hair),
        )
    }
}
