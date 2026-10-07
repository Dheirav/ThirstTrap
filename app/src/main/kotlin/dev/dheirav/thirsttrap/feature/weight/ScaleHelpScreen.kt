package dev.dheirav.thirsttrap.feature.weight

import dev.dheirav.thirsttrap.ui.Space

import dev.dheirav.thirsttrap.ui.ScreenTitle
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
import androidx.compose.ui.text.font.FontWeight

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
                "Why weight beats a calendar",
                "A pot loses water almost entirely by evaporation, which is steady over a " +
                    "day. So its weight falls in a near-straight line between waterings, and " +
                    "the slope of the last few weigh-ins says when it will next be thirsty. " +
                    "No schedule can know that; your scale can.",
            )
            Section(
                "Any kitchen scale will do",
                "5 or 10 gram resolution is plenty. A six-inch pot swings 300 to 400 grams " +
                    "between soaked and dry, so the signal dwarfs the noise. A one-gram scale " +
                    "is a luxury, not a requirement.",
            )
            Section(
                "Consistency matters more than precision",
                "Same scale, same spot on the platter, same saucer situation - and not just " +
                    "after misting. A repeatable reading that is ten grams off is far more " +
                    "useful than an exact one taken differently each time.",
            )
            Section(
                "Weigh before and after watering",
                "Those two are worth more than any others. The one after watering keeps the " +
                    "full mark honest as the plant grows and the soil settles; the one before " +
                    "teaches the app where you actually judge it to be dry.",
            )
            Section(
                "How dry before watering",
                "The slider in Edit plant sets how much of the pot's wet-to-dry range is " +
                    "used up before it is worth watering. It only appears for a pot you " +
                    "weigh, because it is the one number the weighing cannot work out for " +
                    "itself. Around 30% for moisture-lovers like ferns and fittonia, 50% " +
                    "for most foliage plants, and 70% or more for succulents and other " +
                    "drought-lovers.",
            )
            Section(
                "Very small pots",
                "If a pot swings less than about 100 grams between wet and dry, weighing " +
                    "will not beat simply lifting it. Trust your hand.",
            )
            Section(
                "Nothing to buy beyond the scale",
                "No sensor, no subscription, no server. Cheap moisture probes rot in weeks " +
                    "and the clever ones stop working when their company does. A dumb scale " +
                    "and a notebook is the method; this is just the notebook.",
            )
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text(
        body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Space.Tight, bottom = Space.Section),
    )
}
