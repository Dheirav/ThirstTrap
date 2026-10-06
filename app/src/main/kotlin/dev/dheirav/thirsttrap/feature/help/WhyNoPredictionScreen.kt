package dev.dheirav.thirsttrap.feature.help

import dev.dheirav.thirsttrap.ui.Space

import androidx.compose.foundation.layout.Column
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
import dev.dheirav.thirsttrap.domain.SuppressionHelp
import dev.dheirav.thirsttrap.domain.SuppressionReason
import dev.dheirav.thirsttrap.domain.helpFor
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.SectionHead

/**
 * Why the app is refusing to give you a date.
 *
 * Reached from the refusal itself, with [asked] set to the one you tapped, so
 * the answer arrives in the same breath as the question. Also browsable from
 * Help with [asked] null, for somebody who wants to know what the app can and
 * cannot do before meeting it.
 *
 * The one you asked about is shown first and in full. The rest follow, because
 * the seven refusals together are the most honest description of the model's
 * limits that exists anywhere in the app, and somebody who has hit one is the
 * person most likely to care about the others.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhyNoPredictionScreen(asked: SuppressionReason?, onBack: () -> Unit) {
    // Both "need another reading" cases print one headline, so showing both
    // would repeat the same heading twice on one page.
    val all = SuppressionReason.entries
        .distinctBy { helpFor(it).shown }
    val others = all.filter { it != asked && helpFor(it).shown != asked?.let { a -> helpFor(a).shown } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle("Why no date") },
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
            Text(
                "The app would rather say nothing than name a day it cannot stand " +
                    "behind. A confident wrong date teaches watering by the calendar, " +
                    "which is the habit weighing exists to replace. There are seven " +
                    "things it can say instead, and each one has something you can do " +
                    "about it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )

            asked?.let {
                SectionHead("What you tapped")
                Explanation(helpFor(it), emphasised = true)
                SectionHead("The others it can say")
            } ?: SectionHead("What it can say")

            others.forEach { Explanation(helpFor(it), emphasised = false) }
            Rule(Modifier.padding(top = Space.Section, bottom = 24.dp))
        }
    }
}

@Composable
private fun Explanation(help: SuppressionHelp, emphasised: Boolean) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(
            "\"${help.shown}\"",
            style = if (emphasised) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            help.why,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Space.Line),
        )
        Text(
            help.whatToDo,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Rule(Modifier.padding(top = 16.dp))
    }
}
