package dev.dheirav.thirsttrap.feature.help

import dev.dheirav.thirsttrap.ui.Flank

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
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.Topic
import dev.dheirav.thirsttrap.ui.Space

/**
 * What a backup is, and the one thing the app had never said out loud.
 *
 * D33 decided against any cloud sync, which quietly makes the user the backup
 * system. The app owes somebody in that position the facts the system runs on,
 * and it was giving them two out of three: the backup screen explains what goes
 * into the zip and that importing merges by id, and Settings shows how long it
 * has been since the last export.
 *
 * The missing third is that **photos live in the app's private storage and are
 * destroyed when the app is uninstalled**. A grep for "uninstall" across every
 * user-facing string in the app found nothing. That is the fact that turns
 * "uninstall, reinstall, restore" from an inconvenience into losing every photo
 * somebody has taken, and it is exactly the sequence anybody moving from a
 * sideloaded build to a signed one has to perform.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupHelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle("Backups") },
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
                .padding(horizontal = Space.Block),
        ) {
            Para(
                "Nothing is saved anywhere but this phone, on purpose. That also " +
                    "means nobody else is keeping a copy for you.",
            )
            Emphasis(
                "Photos live inside the app, not in your gallery. Uninstalling " +
                    "ThirstTrap deletes them.",
            )

            Topic("What a backup is") {
                Para(
                    "One zip file with everything in it: your plants, every entry, " +
                        "every weight, your reminders and your photos. There is a " +
                        "small text file inside listing the counts, so you can check " +
                        "a backup is complete without opening the app.",
                )
            }
            Topic("Why photos matter most") {
                Para(
                    "Everything else is words, and you could type it again from " +
                        "memory. A photo of a leaf six weeks ago you cannot. So " +
                        "before you uninstall, change phone, or install a different " +
                        "build, export first and keep the zip off the phone.",
                )
                Para(
                    "Saving one photo to your gallery is not a backup. The copy has " +
                        "no plant and no date on it, and it will not follow any later " +
                        "change. Only the zip keeps a photo and what it was of " +
                        "together.",
                )
            }
            Topic("What happens when you restore") {
                Para(
                    "Importing adds a backup to what is already here rather than " +
                        "replacing it. Each entry is matched by its own id, so " +
                        "importing the same file twice changes nothing the second " +
                        "time, and an old backup alongside newer entries keeps both.",
                )
                Para(
                    "It cannot undo anything. Delete a plant and then import a backup " +
                        "from before, and the plant comes back: a backup has no record " +
                        "that something was removed on purpose.",
                )
            }
            Topic("How often to do it") {
                Para(
                    "There is no right answer and the app will not nag you. Settings " +
                        "shows how long it has been, as a fact and not a warning. " +
                        "After a weighing session is a good habit, because weights " +
                        "are the one thing you could never write down again from " +
                        "memory.",
                )
            }
            Rule(flank = Flank.Section)
        }
    }
}

@Composable
private fun Para(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Space.Entry),
    )
}

/** The one sentence on this page somebody has to come away with. */
@Composable
private fun Emphasis(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(top = Space.Entry),
    )
}
