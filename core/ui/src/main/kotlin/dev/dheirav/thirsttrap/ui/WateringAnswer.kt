package dev.dheirav.thirsttrap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text

/**
 * The two answers to "how does the pot feel?", as one component.
 *
 * This pair is the whole product. It existed in three places with the same
 * comment copied beside each one, saying the two answers must carry equal
 * weight, which made the most load-bearing rule in the app a convention that
 * three screens had to remember. MenuLabels already records four help pages
 * going stale in an hour because a label lived in two copies.
 *
 * Equal weight is now a property of the component rather than a note. There is
 * deliberately no parameter to emphasise one side: both are tonal, both take
 * `weight(1f)`, and the only asymmetry permitted is the millilitres on the
 * watered label, which is information rather than emphasis. Promoting either
 * would score watering above restraint, and a pot that did not need water is
 * the better outcome of the two.
 */
@Composable
fun WateringAnswer(
    onWatered: () -> Unit,
    onStillWet: () -> Unit,
    modifier: Modifier = Modifier,
    suggestedWaterMl: Double? = null,
    stillWetLabel: String = "Still wet",
    gap: androidx.compose.ui.unit.Dp = 10.dp,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        Answer(onWatered) {
            Text(
                suggestedWaterMl?.let { "Watered  ${it.toInt()} ml" } ?: "Watered",
                textAlign = TextAlign.Center,
            )
        }
        Answer(onStillWet) { Text(stillWetLabel, textAlign = TextAlign.Center) }
    }
}

/** One side. Private, so neither side can be given a different shape by a caller. */
@Composable
private fun RowScope.Answer(onClick: () -> Unit, content: @Composable () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(BlockHeight),
    ) { content() }
}
