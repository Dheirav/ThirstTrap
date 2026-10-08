package dev.dheirav.thirsttrap.feature.help

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.text.font.FontWeight
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.Flank
import dev.dheirav.thirsttrap.ui.IconButton
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.Space

/**
 * The one shape every help page has.
 *
 * It existed before this file, as a private function in HelpScreen.kt used by
 * the two pages that happen to live there, while Wayfinding, Backups and the
 * reminders page each hand-rolled the same Scaffold, TopAppBar, ScreenTitle,
 * scroll and padding. Four copies agreed on most of it and disagreed on the
 * rest: two padded horizontally and two padded all round, so a page's text
 * started at a different place depending on which file you had opened.
 *
 * A private helper is invisible to the next person writing a page, so they
 * write a fifth copy. That is the whole reason this is its own file.
 *
 * Sections inside it are [dev.dheirav.thirsttrap.ui.SectionHead], always open,
 * never folded. Help pages are reached from a list of questions, so the person
 * arriving has already chosen; asking them to choose again, from a column of
 * closed folds, is the same decision twice. Backups used to fold and nothing
 * else did, which is what made the set feel like five apps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle(title) },
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
                .padding(horizontal = Space.Block),
            content = content,
        )
        // Horizontal padding only, and the closing rule below carries the
        // bottom. A symmetric all-round pad put Space.Block above the first
        // line on some pages and nothing above it on others, because the ones
        // that start with a SectionHead already get Space.Section from it.
    }
}

/**
 * A paragraph of help prose.
 *
 * Declared privately in two files before this, at bodyMedium in both and with
 * a different gap above it in each: Space.Entry on Backups, Space.Block on
 * Wayfinding. Nobody comparing one page against the other could see that,
 * which is the argument for the spacing living in one place rather than at the
 * call site.
 *
 * bodyMedium, 14sp, and it is the only size help prose comes in. There were
 * three across these pages, bodyLarge, bodyMedium and bodySmall, so a sentence
 * changed size depending on whether it was a paragraph, an item's description
 * or a closing note. Anything that needs to stand out does it by weight now,
 * which is what [HelpEmphasis] is.
 */
@Composable
fun HelpPara(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Space.Entry),
    )
}

/**
 * The one sentence on a page somebody has to come away with. At most one.
 *
 * Same size as everything else now; it was the only bodyLarge on a bodyMedium
 * page, which read as a different font rather than as emphasis. SemiBold does
 * the work, because the point was never that the sentence is bigger.
 */
@Composable
fun HelpEmphasis(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(top = Space.Entry),
    )
}

/** Closes a page, so the last paragraph is not flush against the navigation bar. */
@Composable
fun HelpPageEnd() {
    Rule(flank = Flank.Section)
}
