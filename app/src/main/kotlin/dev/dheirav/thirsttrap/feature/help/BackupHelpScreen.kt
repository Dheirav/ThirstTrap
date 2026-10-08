package dev.dheirav.thirsttrap.feature.help

import androidx.compose.runtime.Composable
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
@Composable
fun BackupHelpScreen(onBack: () -> Unit) {
    HelpPage("Backups", onBack) {
        HelpPara(
            "Nothing is saved anywhere but this phone, on purpose. That also " +
                "means nobody else is keeping a copy for you.",
        )
        HelpEmphasis(
            "Photos live inside the app, not in your gallery. Uninstalling " +
                "ThirstTrap deletes them.",
        )

        // Flat sections, not folds. These four were Topic foldables and every
        // other help page was flat, which is what made the set read as five
        // separate apps. A page reached from a list of questions has already
        // had its question chosen, so a column of closed folds asks the same
        // thing twice.
        SectionHead("What a backup is")
        HelpPara(
            "One zip file with everything in it: your plants, every entry, " +
                "every weight, your reminders and your photos. There is a " +
                "small text file inside listing the counts, so you can check " +
                "a backup is complete without opening the app.",
        )

        SectionHead("Why photos matter most")
        HelpPara(
            "Everything else is words, and you could type it again from " +
                "memory. A photo of a leaf six weeks ago you cannot. So " +
                "before you uninstall, change phone, or install a different " +
                "build, export first and keep the zip off the phone.",
        )
        HelpPara(
            "Saving one photo to your gallery is not a backup. The copy has " +
                "no plant and no date on it, and it will not follow any later " +
                "change. Only the zip keeps a photo and what it was of " +
                "together.",
        )

        SectionHead("What happens when you restore")
        HelpPara(
            "Importing adds a backup to what is already here rather than " +
                "replacing it. Each entry is matched by its own id, so " +
                "importing the same file twice changes nothing the second " +
                "time, and an old backup alongside newer entries keeps both.",
        )
        HelpPara(
            "It cannot undo anything. Delete a plant and then import a backup " +
                "from before, and the plant comes back: a backup has no record " +
                "that something was removed on purpose.",
        )

        SectionHead("How often to do it")
        HelpPara(
            "There is no right answer and the app will not nag you. Settings " +
                "shows how long it has been, as a fact and not a warning. " +
                "After a weighing session is a good habit, because weights " +
                "are the one thing you could never write down again from " +
                "memory.",
        )
        HelpPageEnd()
    }
}
