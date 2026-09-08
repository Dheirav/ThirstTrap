package dev.dheirav.thirsttrap.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.domain.MonthCount
import dev.dheirav.thirsttrap.domain.Outcomes
import dev.dheirav.thirsttrap.domain.RootingStat
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.ColumnHead
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.SectionHead
import kotlin.math.roundToInt

/**
 * The collection, counted.
 *
 * An almanac is tables of figures, so this screen is the one place where the
 * form and the content were made for each other. Everything here is a table
 * with column heads and rules; the only chart is a bar column, which is a table
 * that has been stood on its end.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit, viewModel: StatsViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle("Figures") },
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
                .padding(16.dp),
        ) {
            Text(
                "${s.plantCount} plants, ${s.totalEvents} entries.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionHead("Waterings by month")
            WateringTable(s.waterings)

            SectionHead("What became of them")
            OutcomeTable(s.outcomes)

            SectionHead("Cuttings")
            RootingTable(s.rooting)

            Text(
                "Counts, not scores. Nothing here goes up because you opened the app, and " +
                    "a quiet month is a quiet month rather than a gap in a run.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 28.dp),
            )
        }
    }
}

@Composable
private fun WateringTable(months: List<MonthCount>) {
    val peak = months.maxOfOrNull { it.count } ?: 0
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        ColumnHead("Month", Modifier.width(110.dp))
        ColumnHead("Waterings", Modifier.weight(1f))
        ColumnHead("", Modifier.width(48.dp), align = TextAlign.End)
    }
    Rule()
    months.forEach { m ->
        Row(
            Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                m.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(110.dp),
            )
            // A rule of proportional length, which is what a table does when it
            // wants to be a chart. Nothing fills toward a total.
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                if (m.count > 0 && peak > 0) {
                    Box(
                        Modifier
                            .fillMaxWidth(m.count.toFloat() / peak)
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
            Text(
                if (m.count == 0) "-" else m.count.toString(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                color = if (m.count == 0) MaterialTheme.colorScheme.outline
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(48.dp),
            )
        }
        Rule()
    }
}

@Composable
private fun OutcomeTable(o: Outcomes) {
    val rows = listOfNotNull(
        "Still with you" to o.stillHere,
        ("Dormant" to o.dormant).takeIf { o.dormant > 0 },
        ("Given away" to o.givenAway).takeIf { o.givenAway > 0 },
        ("Died" to o.died).takeIf { o.died > 0 },
        ("Unrecorded" to o.unknown).takeIf { o.unknown > 0 },
    )
    Column(Modifier.padding(top = 8.dp)) {
        rows.forEach { (label, count) ->
            Row(
                Modifier.fillMaxWidth().height(40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(count.toString(), style = MaterialTheme.typography.bodyMedium)
            }
            Rule()
        }
        Text(
            // Deliberately not a percentage on its own line. The requirement
            // asks for a survival rate; the anti-goals rule out anything that
            // turns a plant's death into a score you watch. So it is a sentence
            // about the ones that have left, and it does not exist until some
            // have.
            when (val rate = o.survivalRate) {
                null -> "Nothing has left your care yet, so there is no rate to give - and " +
                    "an empty record is not a perfect one."
                else -> "Of the ${o.departed} that have left, ${o.givenAway} went to someone " +
                    "else and ${o.died} died: ${(rate * 100).roundToInt()}% passed on rather " +
                    "than lost."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun RootingTable(r: RootingStat) {
    Column(Modifier.padding(top = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Typical days to root", style = MaterialTheme.typography.bodyMedium)
            Text(
                r.medianDays?.toString() ?: "-",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Rule()
        Text(
            buildString {
                when {
                    r.samples == 0 -> append(
                        "No cutting has reached potting with a recorded date yet. Move one to " +
                            "potted on the propagation board and it starts counting.",
                    )
                    else -> {
                        append("Median across ${r.samples} ")
                        append(if (r.samples == 1) "cutting" else "cuttings")
                        append(". The median rather than the average, because one jar left for ")
                        append("months would drag a mean somewhere useless.")
                    }
                }
                if (r.untracked > 0) {
                    append(" ${r.untracked} rooted before the app recorded stages, so ")
                    append(if (r.untracked == 1) "it is" else "they are")
                    append(" left out rather than guessed at.")
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}
