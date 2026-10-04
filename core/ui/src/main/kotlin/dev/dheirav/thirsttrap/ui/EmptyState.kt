package dev.dheirav.thirsttrap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A page with nothing on it yet, said the same way every time.
 *
 * Five screens built this by hand and landed on three different title weights:
 * titleMedium on three, headlineSmall on two. titleMedium wins because it is
 * the plurality and because headlineSmall is page-title weight, which makes an
 * empty tab shout louder than the tab it is inside.
 *
 * The title is held back until the data has actually answered. Every one of the
 * five already did this, with `if (state.loaded) "No places yet" else ""`,
 * because a list is empty for one frame before it is full and a page that says
 * "nothing here" and then fills in is worse than one that says nothing at all.
 * Passing a blank title is the supported way to say "not yet".
 *
 * There is no illustration by default and no action by default. A mark is for
 * the one page that is the whole app being empty; an action is for the one
 * thing a reader can do about it, if there is one.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    mark: @Composable (() -> Unit)? = null,
    action: @Composable (ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (mark != null) {
            mark()
            Spacer(Modifier.height(24.dp))
        }
        if (title.isNotEmpty()) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}
