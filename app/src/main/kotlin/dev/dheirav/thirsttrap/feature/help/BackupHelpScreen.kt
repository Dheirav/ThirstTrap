package dev.dheirav.thirsttrap.feature.help

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
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.Rule
import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.SectionHead

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
                .padding(horizontal = 16.dp),
        ) {
            Para(
                "There is no cloud account and nothing syncs anywhere, which is " +
                    "deliberate. It also means the backups are yours to take: if you " +
                    "never export, there is no copy of your diary but the one on this " +
                    "phone.",
            )

            SectionHead("What is in a backup")
            Para(
                "One zip file holding every plant, every entry, every weight reading, " +
                    "your reminders, the fertilisers, the room readings, your place " +
                    "notes, and the photo files themselves. A small text file inside " +
                    "it lists the counts, so you can check a backup is complete " +
                    "without an app to open it.",
            )

            SectionHead("Photos are the part that cannot be replaced")
            Emphasis(
                "Photos are stored inside the app, not in your gallery, so uninstalling " +
                    "ThirstTrap deletes them.",
            )
            Para(
                "Everything else is text and could in principle be typed again from " +
                    "memory. A photo of what a leaf looked like six weeks ago cannot " +
                    "be. If you are about to uninstall, reinstall, change phone or " +
                    "move to a different build of the app, export first and keep the " +
                    "zip somewhere outside the phone.",
            )
            Para(
                "You can also save a single photo into your gallery, from the dots menu " +
                    "when it is open full screen. That is an escape hatch rather than a " +
                    "backup: the copy has no plant and no date attached to it, and it " +
                    "will not know if you later change the caption or delete the entry. " +
                    "The zip is the thing that keeps a photo and its context together.",
            )

            SectionHead("What restoring does")
            Para(
                "Importing merges a backup into whatever is already here rather than " +
                    "replacing it. Entries are matched by their own identity, so " +
                    "importing the same file twice changes nothing the second time, " +
                    "and importing an old backup alongside newer entries keeps both.",
            )
            Para(
                "What it will not do is undo anything. If you deleted a plant last " +
                    "week and import a backup from before that, the plant comes back. " +
                    "A delete is not recorded in the file, so there is nothing in a " +
                    "backup that can say \"this was removed on purpose\".",
            )

            SectionHead("How often")
            Para(
                "There is no right answer and the app will not nag you about it. " +
                    "Settings shows how long it has been since your last export, as a " +
                    "fact rather than a warning. A sensible habit is after a weighing " +
                    "session, since weight readings are the one thing in here that " +
                    "genuinely cannot be reconstructed from memory.",
            )
            Rule(Modifier.padding(top = 24.dp, bottom = 24.dp))
        }
    }
}

@Composable
private fun Para(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
}

/** The one sentence on this page somebody has to come away with. */
@Composable
private fun Emphasis(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )
}
