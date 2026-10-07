package dev.dheirav.thirsttrap.feature.weight

import dev.dheirav.thirsttrap.ui.Space

import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.Topic
import dev.dheirav.thirsttrap.ui.AlmanacSheet
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Feature F17.20. The method fails quietly if the weighing is inconsistent.
 *
 * A sheet rather than a page, which is the rule WeightScreen already wrote for
 * itself: "Weighing is an act, not a view, so it moved into a sheet and the
 * page became a page." This is the other half of that. Something you read and
 * return from does not need an address, a back arrow and a place on the
 * navigation stack; it needs to appear over what you were looking at and then
 * get out of the way.
 *
 * It also takes a help topic out of the destination graph, where every topic
 * being a destination is why "read this" once needed a navigation control and
 * got the heaviest one available.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleHelpSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AlmanacSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                // Both insets, because this sheet is long enough to expand to
                // the full height of the window: without the top one the title
                // renders under the status bar clock, which is what it did.
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(Space.Block),
        ) {
            ScreenTitle("Weighing plants")
            Section(
                initiallyOpen = true,
                title = "Why weighing works",
                body = "A watered pot is heavy and a dry one is light, and it gets " +
                    "lighter a little at a time as the water evaporates. So if you " +
                    "weigh it every few days, the app can see how fast this pot " +
                    "loses water and tell you when it will run out. A calendar " +
                    "cannot do that, because it does not know your room, your " +
                    "soil, or how big the plant has got. All you need is a " +
                    "kitchen scale: no probe to push into the soil, nothing to " +
                    "subscribe to.",
            )
            Section(
                "How to weigh it",
                "Any kitchen scale will do. One that reads to 5 or 10 grams is " +
                    "plenty, because a six-inch pot changes by 300 to 400 grams " +
                    "between soaked and dry, which is far more than the scale " +
                    "could get wrong. Doing it the same way each time matters more " +
                    "than being exact: same scale, same saucer on or off. A " +
                    "reading that is always ten grams out is more useful than an " +
                    "exact one taken differently each time. The two worth most are " +
                    "just after you water it, and just before you water it next.",
            )
            Section(
                "How dry to let it get",
                "This is the one thing the app cannot work out on its own, because " +
                    "it depends on the plant rather than the pot. The slider in " +
                    "Edit plant sets it, and it only appears for a pot you weigh. " +
                    "Ferns and fittonia want watering at about 30% dry, most leafy " +
                    "houseplants around 50%, and succulents 70% or more.",
            )
            Section(
                "When weighing will not help",
                "If a pot changes by less than about 100 grams between wet and " +
                    "dry, you can judge it by lifting it and the scale adds " +
                    "nothing. That is most very small pots. The same goes for a " +
                    "sealed jar, which recycles its own water and so barely " +
                    "changes weight at all. Turn weighing off for those in Edit " +
                    "plant and the app stops asking.",
            )
        }
    }
}

/**
 * One topic, folded unless it is the one you most likely came for.
 *
 * Seven open paragraphs is about 1500 characters arriving at once, and the
 * reader wanting "what scale do I need" had to read past three answers to
 * other questions to reach it. Folded, the sheet opens as seven questions and
 * you pick yours, which is the same content and a shorter path to any of it.
 */
@Composable
private fun Section(title: String, body: String, initiallyOpen: Boolean = false) {
    Topic(title, initiallyOpen = initiallyOpen) {
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Space.Tight, bottom = Space.Entry),
        )
    }
}
