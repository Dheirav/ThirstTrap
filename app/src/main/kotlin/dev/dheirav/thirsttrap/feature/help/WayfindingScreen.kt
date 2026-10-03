package dev.dheirav.thirsttrap.feature.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import dev.dheirav.thirsttrap.ui.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.MenuLabels
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.SectionHead

/**
 * Where things are, and the two gestures nothing on screen can tell you about.
 *
 * Not a tour and not coach marks. Overlay tooltips pointing at buttons on first
 * run would fight everything else here: no chrome, no nagging, no tone of
 * voice, and they get dismissed unread anyway. The first-run page explains what
 * the app is *for*; this one answers "where is the thing I want", which is a
 * different question and the one somebody asks in week two.
 *
 * The gesture table at the bottom is the part that earns its place. Every other
 * route in the app is a plain tap and therefore learnable by trying, which was
 * the point of D48. Two long presses survive as shortcuts, and a shortcut
 * nobody knows about is not a shortcut. docs/NAVIGATION.md section 7.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WayfindingScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle("Finding your way") },
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
                .padding(horizontal = 16.dp),
        ) {
            Para(
                "Three tabs along the bottom, and almost everything else lives on the " +
                    "plant it belongs to. If you are looking for something and cannot " +
                    "find it, it is behind one of the two overflow menus below.",
            )

            SectionHead("The three tabs")
            Item(MenuLabels.Tab.PLANTS, "Everything you are keeping, each with its state and two quick log buttons. The plus button adds one.")
            Item(MenuLabels.Tab.DUE, "Only what is asking for attention today. Empty is the normal state and means nothing is wrong.")
            Item(MenuLabels.Tab.SETTINGS, "Backup, reminders, appearance, and two help buttons: how the app works, and something is wrong.")

            SectionHead("Behind the dots on the Plants tab")
            Para(
                "Six jobs that are about several plants at once rather than about one, " +
                    "which is why they are not on any single plant:",
            )
            Item(MenuLabels.Dashboard.FEEDING, "What is due a feed, and the dilution maths for a given can.")
            Item(MenuLabels.Dashboard.PROPAGATION, "Cuttings from cut to established, by stage.")
            Item(MenuLabels.Dashboard.SCAN, "Opens whichever plant's sticker you point the camera at.")
            Item(MenuLabels.Dashboard.PLACES, "A note and a light reading per spot in the house.")
            Item(MenuLabels.Dashboard.FIGURES, "How often you water, what became of things, and whether the predictions have been any good.")
            Item(MenuLabels.Dashboard.EXPERIMENTS, "Two arms, one variable, for when you want an answer rather than an impression.")

            SectionHead("Behind the dots on one plant")
            Para(
                "Everything that is about that plant. The two you will use most are at " +
                    "the top and the camera has its own button beside the dots.",
            )
            Item(MenuLabels.Plant.LOG, "A watering with an amount, a check, a repot, a feed, anything with a date.")
            Item(MenuLabels.Plant.GALLERY, "Adds a photo you already took, instead of taking a new one.")
            Item(MenuLabels.Plant.DIAGNOSE, "Walks you through what the log and the weight can and cannot tell you, and what to rule out first. Ends with a button to log what you found.")
            Item(MenuLabels.Plant.WEIGHT, "Weigh the pot, see how far down its range it has come, and get a date when there is one worth giving. This is the one the app exists for.")
            Item(MenuLabels.Plant.LIGHT, "Uses the phone's light sensor and files the reading against the place.")
            Item(MenuLabels.Plant.LOOKUP, "Only appears when there are no care notes on file for it.")
            Item(MenuLabels.Plant.STICKER, "Only with advanced features on. Prints a QR label for the pot, so scanning it opens this plant.")
            Item(MenuLabels.Plant.EDIT, "Name, species, pot, how dry it is allowed to get.")

            SectionHead("Photos")
            Para(
                "Tap any photo to open it full screen. Everything photos can do is in " +
                    "there rather than in a menu: swipe between them, Oldest and Latest " +
                    "to jump either end, Play to run through them in order, and in the " +
                    "dots menu, \"Compare with this\" to pin one and swipe the other " +
                    "half against it. That menu also shares a photo, or saves a copy " +
                    "into your gallery.",
            )

            SectionHead("Two gestures worth knowing")
            Para(
                "Everything in the app opens on a plain tap, so you can find your way " +
                    "around by trying things. There are exactly two exceptions, both " +
                    "shortcuts past something you can also reach by tapping:",
            )
            Gesture(
                "Hold a plant on the Plants tab",
                "The quick sheet: water, still wet, photo, weigh, edit. Everything in it " +
                    "is also on the plant's own page, which a tap opens.",
            )
            Gesture(
                "Hold the water drop on a plant",
                "Asks for the amount and how you watered, instead of logging your usual " +
                    "amount straight away.",
            )

            Para(
                "Nothing else in the app hides behind a hold. Tapping a photo opens the " +
                    "photo, tapping a diary entry opens the entry, and neither one can " +
                    "delete anything without asking first.",
                top = 20,
            )
            Rule(Modifier.padding(top = 20.dp, bottom = 24.dp))
        }
    }
}

@Composable
private fun Para(text: String, top: Int = 16) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = top.dp),
    )
}

/** A name and what it is for, which is the only thing a map has to say. */
@Composable
private fun Item(name: String, what: String) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.Start) {
        Column {
            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                what,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Set apart from the list items, because this is the half nobody can guess. */
@Composable
private fun Gesture(action: String, result: String) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Rule()
        Text(
            action,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            result,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
