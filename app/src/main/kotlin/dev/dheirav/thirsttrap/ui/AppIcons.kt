package dev.dheirav.thirsttrap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import dev.dheirav.thirsttrap.R

/**
 * The app's icons: Material Symbols Rounded, vendored as sixteen vector
 * drawables by tools/fetch-icons.py.
 *
 * Replaces `material-icons-extended`, which Google's own documentation marks as
 * "no longer maintained or recommended" and warns "can also increase the build
 * time of your apps significantly". The app used sixteen icons out of the
 * several thousand that artifact ships.
 *
 * ## Outlined is the default, and filled means something
 *
 * Everything here is the outlined weight except [waterDropFilled]. That is the
 * only filled icon in the app, and it means exactly one thing: a watering that
 * has just been logged.
 *
 * That rule exists because of a real ambiguity. The two log actions on a plant
 * card - "still wet, left it alone" and "watered it" - were `TouchApp` and
 * `WaterDrop`, side by side, both filled, both tinted `primary`, with nothing
 * distinguishing the restraint action from the watering action. They are the
 * app's central gesture and they were a coin flip.
 */
object AppIcons {
    val arrowBack: Painter @Composable get() = painterResource(R.drawable.ic_arrow_back)
    val arrowForward: Painter @Composable get() = painterResource(R.drawable.ic_arrow_forward)
    val add: Painter @Composable get() = painterResource(R.drawable.ic_add)
    val addAPhoto: Painter @Composable get() = painterResource(R.drawable.ic_add_a_photo)
    val backspace: Painter @Composable get() = painterResource(R.drawable.ic_backspace)
    val brokenImage: Painter @Composable get() = painterResource(R.drawable.ic_broken_image)
    val lock: Painter @Composable get() = painterResource(R.drawable.ic_lock)
    val lockOpen: Painter @Composable get() = painterResource(R.drawable.ic_lock_open)
    val moreVert: Painter @Composable get() = painterResource(R.drawable.ic_more_vert)
    val notifications: Painter @Composable get() = painterResource(R.drawable.ic_notifications)
    val qrCodeScanner: Painter @Composable get() = painterResource(R.drawable.ic_qr_code_scanner)
    val settings: Painter @Composable get() = painterResource(R.drawable.ic_settings)
    val spa: Painter @Composable get() = painterResource(R.drawable.ic_spa)
    val weight: Painter @Composable get() = painterResource(R.drawable.ic_weight)
    val yard: Painter @Composable get() = painterResource(R.drawable.ic_yard)

    /** "I watered it." Outlined at rest. */
    val waterDrop: Painter @Composable get() = painterResource(R.drawable.ic_water_drop)

    /**
     * The one filled icon in the app. Only ever shown for a moment, right after
     * a watering is logged - weight is the confirmation, not decoration.
     */
    val waterDropFilled: Painter @Composable get() = painterResource(R.drawable.ic_water_drop_filled)

    /**
     * "I checked, it is still wet, I left it alone." A hand held back from the
     * pot, which is a picture of the action; `TouchApp` was a finger pressing a
     * button, which is a picture of the UI.
     */
    val stillWet: Painter @Composable get() = painterResource(R.drawable.ic_still_wet)
}
