package dev.dheirav.thirsttrap.ui

import dev.dheirav.thirsttrap.ui.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import java.io.File

/**
 * A photo that cannot crash or silently vanish.
 *
 * A row whose file has gone - a restore that went wrong, storage cleared,
 * an interrupted write - must render as a placeholder, never as a blank hole
 * and never as an exception. docs/UI-SPEC.md section 9 and the orphan-row case
 * in docs/DATA-MODEL.md.
 */
@Composable
fun PlantPhoto(
    path: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    // Checked once per path rather than on every recomposition.
    val exists = remember(path) { File(path).exists() }

    Box(modifier, contentAlignment = Alignment.Center) {
        if (!exists) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.brokenImage,
                    // Silent when the caller wanted a decorative image, or every
                    // dashboard card would announce a lost file.
                    contentDescription = contentDescription?.let { "$it - file is missing" },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            // Coil's crossfade is off by default. A photo appearing instantly
            // on a dark card is a flash; 220ms is the difference between a page
            // loading and a page blinking.
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(path)
                    .crossfade(if (Motion.reduced()) 0 else Motion.CONFIRM_MS)
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
