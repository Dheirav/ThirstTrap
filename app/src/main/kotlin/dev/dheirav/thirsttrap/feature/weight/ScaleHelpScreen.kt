package dev.dheirav.thirsttrap.feature.weight

import dev.dheirav.thirsttrap.ui.AppIcons
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Feature F17.20. The method fails quietly if the weighing is inconsistent. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleHelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weighing plants") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
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
        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
    )
}
