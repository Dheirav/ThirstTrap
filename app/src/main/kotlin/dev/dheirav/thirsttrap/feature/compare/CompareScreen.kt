package dev.dheirav.thirsttrap.feature.compare

import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.ui.PlantPhoto
import dev.dheirav.thirsttrap.domain.Photo
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * The diagnosis workflow: is the brown spot bigger than it was?
 *
 * Side by side in portrait, never stacked - plants are taller than they are
 * wide, so stacking would waste the axis that matters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(onBack: () -> Unit, viewModel: CompareViewModel = hiltViewModel()) {
    val photos by viewModel.allPhotos.collectAsStateWithLifecycle()
    val leftId by viewModel.left.collectAsStateWithLifecycle()
    val rightId by viewModel.right.collectAsStateWithLifecycle()
    val sync by viewModel.syncZoom.collectAsStateWithLifecycle()

    // Shared transform when the panes are locked together. Zooming into the
    // same leaf on both is the actual move this screen exists for.
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var leftScale by remember { mutableFloatStateOf(1f) }
    var leftOffset by remember { mutableStateOf(Offset.Zero) }
    var rightScale by remember { mutableFloatStateOf(1f) }
    var rightOffset by remember { mutableStateOf(Offset.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ScreenTitle("Compare") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleSync) {
                        Icon(
                            if (sync) AppIcons.lock else AppIcons.lockOpen,
                            contentDescription = "Zoom both panes together",
                            modifier = Modifier.semantics {
                                stateDescription = if (sync) "On" else "Off"
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (photos.size < 2) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Need two photos", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Take another photo of this plant a few days from now, and you can " +
                        "put them side by side.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            return@Scaffold
        }

        val leftPhoto = viewModel.photoById(leftId)
        val rightPhoto = viewModel.photoById(rightId)

        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ZoomPane(
                    path = leftPhoto?.let(viewModel::pathOf),
                    scale = if (sync) scale else leftScale,
                    offset = if (sync) offset else leftOffset,
                    onTransform = { s, o ->
                        if (sync) { scale = s; offset = o } else { leftScale = s; leftOffset = o }
                    },
                    modifier = Modifier.weight(1f).fillMaxSize(),
                )
                ZoomPane(
                    path = rightPhoto?.let(viewModel::pathOf),
                    scale = if (sync) scale else rightScale,
                    offset = if (sync) offset else rightOffset,
                    onTransform = { s, o ->
                        if (sync) { scale = s; offset = o } else { rightScale = s; rightOffset = o }
                    },
                    modifier = Modifier.weight(1f).fillMaxSize(),
                )
            }

            // The number the whole screen is for.
            viewModel.elapsedLabel()?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                )
            }

            Row(Modifier.fillMaxWidth()) {
                Filmstrip(
                    photos = photos,
                    selectedId = leftId,
                    pathOf = viewModel::pathOf,
                    onSelect = viewModel::selectLeft,
                    side = "left",
                    modifier = Modifier.weight(1f),
                )
                Filmstrip(
                    photos = photos,
                    selectedId = rightId,
                    pathOf = viewModel::pathOf,
                    onSelect = viewModel::selectRight,
                    side = "right",
                    modifier = Modifier.weight(1f),
                )
            }

            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dateOf(leftPhoto), style = MaterialTheme.typography.labelMedium)
                Text(dateOf(rightPhoto), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ZoomPane(
    path: String?,
    scale: Float,
    offset: Offset,
    onTransform: (Float, Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    // rememberUpdatedState, because pointerInput(Unit) starts its suspend block
    // once and would otherwise keep multiplying against the scale from the very
    // first composition.
    val currentScale by rememberUpdatedState(scale)
    val currentOffset by rememberUpdatedState(offset)
    val onTransformNow by rememberUpdatedState(onTransform)

    Box(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val next = (currentScale * zoom).coerceIn(1f, 6f)
                    if (next <= 1f) {
                        onTransformNow(1f, Offset.Zero)
                    } else {
                        // Clamp the pan to the scaled image, or the photo can be
                        // flung off-screen with no way back short of zooming out.
                        val maxX = size.width * (next - 1f) / 2f
                        val maxY = size.height * (next - 1f) / 2f
                        val moved = currentOffset + pan
                        onTransformNow(
                            next,
                            Offset(
                                moved.x.coerceIn(-maxX, maxX),
                                moved.y.coerceIn(-maxY, maxY),
                            ),
                        )
                    }
                }
            }
            .pointerInput(Unit) {
                // The only escape from a zoom was pinching back to exactly 1x.
                detectTapGestures(onDoubleTap = { onTransformNow(1f, Offset.Zero) })
            },
        contentAlignment = Alignment.Center,
    ) {
        if (path == null) {
            Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            PlantPhoto(
                path = path,
                contentDescription = "Plant photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
            )
        }
    }
}

/** Each pane picks independently - that is the point of two strips. */
@Composable
private fun Filmstrip(
    photos: List<Photo>,
    selectedId: String?,
    side: String,
    pathOf: (Photo) -> String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.height(72.dp),
        contentPadding = PaddingValues(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(photos, key = { it.id }) { photo ->
            val selected = photo.id == selectedId
            PlantPhoto(
                path = pathOf(photo),
                contentDescription = "${dateOf(photo)}, show in the $side pane",
                modifier = Modifier
                    .semantics { this.selected = selected }
                    .size(60.dp)
                    .clip(MaterialTheme.shapes.small)
                    .border(
                        width = if (selected) 3.dp else 0.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                    )
                    .clickable { onSelect(photo.id) },
            )
        }
    }
}

private fun dateOf(photo: Photo?): String {
    if (photo == null) return ""
    val zone = ZoneOffset.ofTotalSeconds(photo.tzOffsetMinutes * 60)
    return Instant.ofEpochMilli(photo.takenAtMillis).atZone(zone)
        .format(DateTimeFormatter.ofPattern("d MMM, HH:mm"))
}
