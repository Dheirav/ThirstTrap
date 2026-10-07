package dev.dheirav.thirsttrap.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import dev.dheirav.thirsttrap.ui.IconButton
import androidx.compose.material3.MaterialTheme
import dev.dheirav.thirsttrap.ui.OutlinedTextField
import androidx.compose.material3.Text
import dev.dheirav.thirsttrap.ui.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.dheirav.thirsttrap.ui.AlmanacMenu
import dev.dheirav.thirsttrap.domain.Photo
import dev.dheirav.thirsttrap.photo.PhotoExport
import dev.dheirav.thirsttrap.domain.TimelapseFrame
import dev.dheirav.thirsttrap.domain.buildTimelapse
import dev.dheirav.thirsttrap.domain.isPlayable
import dev.dheirav.thirsttrap.domain.spanDays
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * A photo, full screen, because that is what tapping a photo means.
 *
 * Every thumbnail in the app was either inert or wired straight to a menu that
 * could delete it, so the one thing a tap could not do was show you the
 * picture. A tap now does only this, everywhere, and everything the old long
 * press offered lives on this page instead: caption, cover, delete.
 *
 * One gesture rather than two is the point. A hidden long press that opens a
 * destructive menu, drawn over the thumbnail it belongs to, is how three real
 * diary entries were deleted by accident during development. Here the same
 * actions are visible controls on a page you meant to open, and Delete asks
 * first.
 *
 * It pages across the whole set rather than showing one photo in isolation,
 * because the reason to look at a leaf is usually the one from three weeks ago.
 * Zoom is per photo and resets when the page changes: carrying a zoom through a
 * swipe means arriving at a photo already framed on the wrong part of it.
 *
 * This is also the timelapse, which used to be a screen of its own. Two
 * full-screen photo viewers is one too many, and the separate one had the three
 * things swiping alone does not give: a scrubber, because dragging once beats
 * swiping twenty-two times, Play, and the day count, because "day 41" is the
 * fact a plant diary is for and "photo 3 of 9" is not. Those moved here.
 *
 * Frames come from the domain's [buildTimelapse], so the order is the order the
 * photos were *taken* rather than the order they were written. A restored
 * backup would otherwise play a plant's life back in whatever order the files
 * landed, and it is also what makes a swipe forward mean forward in time.
 */
@Composable
fun PhotoViewer(
    photos: List<Photo>,
    startId: String,
    /** Travels with a shared photo, since a leaf is not self-explanatory. */
    plantName: String,
    pathOf: (Photo) -> String,
    onDismiss: () -> Unit,
    onSaveCaption: (photoId: String, caption: String) -> Unit,
    onSetCover: (photoId: String) -> Unit,
    onDelete: (photoId: String) -> Unit,
) {
    if (photos.isEmpty()) return
    val frames = remember(photos) { buildTimelapse(photos) }
    val start = frames.indexOfFirst { it.photo.id == startId }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = start, pageCount = { frames.size })

    val scope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var menu by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var editingCaption by remember { mutableStateOf<Photo?>(null) }
    var confirmingDelete by remember { mutableStateOf<Photo?>(null) }
    // The fixed half of a comparison. Null means the ordinary single view.
    // Set only from this screen's own menu. It was also settable on open, for
    // the plant page's "Compare photos" entry, and that entry is gone: it was
    // one of three doors to this one screen, and the photo thumbnails on the
    // plant page are the door that does not need explaining.
    var pinned by remember { mutableStateOf<TimelapseFrame?>(null) }
    val context = LocalContext.current

    LaunchedEffect(pager.settledPage) {
        scale = 1f
        offset = Offset.Zero
    }

    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        while (true) {
            delay(PLAY_FRAME_MILLIS)
            val next = pager.currentPage + 1
            // Stops at the end rather than looping. A plant's history has a
            // most recent photo, and looping back to the cutting implies a
            // cycle the plant is not in.
            if (next >= frames.size) {
                playing = false
                break
            }
            pager.animateScrollToPage(next)
        }
    }

    // An overlay in the activity's own window rather than a Dialog.
    //
    // As a Dialog this was clipped to [0,152][1280,2619] on the target phone
    // whatever DialogProperties said, because the window manager fits a dialog
    // window inside the system bars and a requested fill size is then shrunk.
    // The caption band, bottom-aligned inside that shrunken window, landed
    // where nothing could draw it. The activity window is already edge to edge,
    // so drawing here needs no second window and no negotiation.
    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            // Not surface. A photo is judged against its surroundings, and a
            // paper-coloured background tints everything warm.
            .background(Color.Black),
    ) {
        // The moving half of the view: the whole screen normally, one column of
        // two while something is pinned.
        val pages: @Composable (Modifier) -> Unit = { mod ->
            HorizontalPager(
                state = pager,
                // A swipe and a pan are the same gesture, so the pager only
                // owns it while the photo is unzoomed. Zoomed in, the drag has
                // to move the picture or there is no way to look at a corner.
                userScrollEnabled = scale <= 1f,
                modifier = mod,
            ) { page ->
                // getOrNull, not an index: the list is live state, so a delete
                // can shrink it under a page that is still composing.
                frames.getOrNull(page)?.photo?.let { photo ->
                    // Only the settled page holds the shared transform; a page
                    // still sliding past must not be drawn at someone else's
                    // zoom.
                    val live = page == pager.settledPage
                    ZoomableImage(
                        path = pathOf(photo),
                        caption = photo.caption,
                        scale = if (live) scale else 1f,
                        offset = if (live) offset else Offset.Zero,
                        onTransform = { s, o -> if (live) { scale = s; offset = o } },
                    )
                }
            }
        }

        val held = pinned
        if (held == null) {
            pages(Modifier.fillMaxSize())
        } else {
            // Side by side, never stacked. Plants are taller than they are
            // wide, so stacking wastes the axis that carries the growth.
            //
            // This is what the Compare screen was for, and it is now here
            // instead: pin one photo, swipe the other. That loses nothing,
            // because picking two photos from two filmstrips was only ever a
            // way to reach this state, and it drops a whole screen, its view
            // model, its route and the picker.
            //
            // Both panes share one transform, which is the move the comparison
            // exists for: pinching into the same leaf on both at once. The old
            // screen made that a lock you had to find and turn on.
            Row(Modifier.fillMaxSize()) {
                ZoomableImage(
                    path = pathOf(held.photo),
                    caption = held.photo.caption,
                    scale = scale,
                    offset = offset,
                    onTransform = { s, o -> scale = s; offset = o },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                VerticalRule(Modifier.fillMaxHeight())
                pages(Modifier.weight(1f).fillMaxHeight())
            }
        }

        val current = frames.getOrNull(pager.currentPage)

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(8.dp),
        ) {
            Icon(AppIcons.close, contentDescription = "Close", tint = Color.White)
        }

        Box(
            Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(8.dp),
        ) {
            IconButton(onClick = { menu = true }) {
                Icon(AppIcons.moreVert, contentDescription = "Photo actions", tint = Color.White)
            }
            AlmanacMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Edit caption") },
                    onClick = { menu = false; editingCaption = current?.photo },
                )
                DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = {
                        menu = false
                        current?.let { frame ->
                            val intent = PhotoExport.shareIntent(
                                context,
                                File(pathOf(frame.photo)),
                                shareCaption(plantName, frame.photo),
                            )
                            if (intent == null) {
                                Toast.makeText(context, "That photo is missing", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                context.startActivity(
                                    Intent.createChooser(intent, "Share this photo"),
                                )
                            }
                        }
                    },
                )
                // Only where it needs no permission to offer, which is API 29
                // and up. See PhotoExport.canSaveToGallery.
                if (PhotoExport.canSaveToGallery) {
                    DropdownMenuItem(
                        text = { Text("Save to gallery") },
                        onClick = {
                            menu = false
                            current?.let { frame ->
                                val name = galleryName(plantName, frame.photo)
                                val ok = PhotoExport.saveToGallery(
                                    context, File(pathOf(frame.photo)), name,
                                )
                                Toast.makeText(
                                    context,
                                    if (ok) {
                                        "Saved to Pictures/ThirstTrap"
                                    } else {
                                        "Could not save that one"
                                    },
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(if (pinned == null) "Compare with this" else "Stop comparing") },
                    onClick = {
                        menu = false
                        // Pinning the photo you are looking at, then swiping the
                        // other half, is the whole interaction. Unpinning also
                        // clears the zoom, or you land back on one photo framed
                        // on a corner of it.
                        pinned = if (pinned == null) current else null
                        scale = 1f
                        offset = Offset.Zero
                    },
                )
                DropdownMenuItem(
                    text = { Text("Make cover photo") },
                    onClick = { menu = false; current?.let { onSetCover(it.photo.id) } },
                )
                DropdownMenuItem(
                    text = { Text("Delete photo", color = MaterialTheme.colorScheme.error) },
                    onClick = { menu = false; confirmingDelete = current?.photo },
                )
            }
        }

        if (current != null) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    // A gradient rather than a flat tint. At 0.55 black the
                    // text was still fighting whatever the photo happened to
                    // be: legible over soil, marginal over a bright wall. A
                    // scrim that deepens downward reads at any exposure and
                    // does not look like a slab laid on the picture. Same
                    // treatment as the hero on the plant page.
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.35f to Color.Black.copy(alpha = 0.55f),
                            1f to Color.Black.copy(alpha = 0.85f),
                        ),
                    )
                    // Inset on the inside, so the scrim runs behind the
                    // navigation bar rather than leaving a stripe of bare
                    // photo under it.
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 16.dp, end = 16.dp, top = Space.Section, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                current.photo.caption?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
                held?.let {
                    val apart = kotlin.math.abs(current.daysSinceFirst - it.daysSinceFirst)
                    Text(
                        // The number the comparison is for, said once, above the
                        // two dates it is the gap between.
                        if (apart == 1) "1 day apart" else "$apart days apart",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "${dateOf(it.photo)}  against  ${dateOf(current.photo)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        // The day count first, the date second: one is the
                        // answer, the other is the evidence for it.
                        "${current.dayLabel}  \u00b7  ${dateOf(current.photo)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.weight(1f),
                    )
                    if (frames.size > 1) {
                        Text(
                            "${pager.currentPage + 1} of ${frames.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.End,
                        )
                    }
                }

                // No slider: a scrubber is a second way to do what the swipe
                // already does, and over a photo it is the heaviest thing on
                // screen, a thick white bar across the picture the page exists
                // to show.
                //
                // Two jumps instead, because a swipe only moves one frame and
                // "what did it look like when I got it" is twenty-one swipes
                // away in a set this size. They are also the two positions
                // anybody actually names: the first photo and the latest.
                //
                // One rule for "is this a sequence at all", and it lives in the
                // domain next to the constant it reads.
                if (frames.isPlayable()) {
                    val span = frames.spanDays()
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${frames.size} photos over " +
                                if (span == 1) "1 day" else "$span days",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f),
                        )
                        // scrollToPage, not animateScrollToPage: animating
                        // twenty-one frames is a blur that takes longer than
                        // the swipes it replaces.
                        JumpButton(
                            label = "Oldest",
                            enabled = pager.currentPage > 0,
                            onClick = { playing = false; scope.launch { pager.scrollToPage(0) } },
                        )
                        JumpButton(
                            label = "Latest",
                            enabled = pager.currentPage < frames.lastIndex,
                            onClick = {
                                playing = false
                                scope.launch { pager.scrollToPage(frames.lastIndex) }
                            },
                        )
                        TextButton(onClick = { playing = !playing }) {
                            Text(if (playing) "Stop" else "Play", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    editingCaption?.let { photo ->
        CaptionEditor(
            initial = photo.caption.orEmpty(),
            onDismiss = { editingCaption = null },
            onSave = { onSaveCaption(photo.id, it); editingCaption = null },
        )
    }

    confirmingDelete?.let { photo ->
        AlmanacDialog(
            title = "Delete this photo?",
            onDismissRequest = { confirmingDelete = null },
            body = { DialogText("The photo goes. The diary entry it belongs to stays.") },
            dismiss = {
                TextButton(onClick = { confirmingDelete = null }) { Text("Keep it") }
            },
            confirm = {
                TextButton(
                    onClick = {
                        confirmingDelete = null
                        onDelete(photo.id)
                        // Back to the page you came from rather than to a gap
                        // where the photo was, which also keeps the pager off
                        // an index that no longer exists.
                        onDismiss()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}

/** Dimmed rather than hidden at the end it points at, so the row does not move. */
@Composable
private fun JumpButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(label, color = Color.White.copy(alpha = if (enabled) 1f else 0.35f))
    }
}

@Composable
private fun CaptionEditor(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlmanacDialog(
        title = "Caption",
        onDismissRequest = onDismiss,
        body = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("brown spot on the lower leaf") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        dismiss = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirm = { TextButton(onClick = { onSave(text) }) { Text("Save") } },
    )
}

@Composable
private fun ZoomableImage(
    path: String,
    caption: String?,
    scale: Float,
    offset: Offset,
    onTransform: (Float, Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    // rememberUpdatedState, because pointerInput(Unit) starts its suspend block
    // once and would otherwise keep multiplying against the first scale it saw.
    // The same trap as the compare screen's panes.
    val currentScale by rememberUpdatedState(scale)
    val currentOffset by rememberUpdatedState(offset)
    val onTransformNow by rememberUpdatedState(onTransform)

    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            // Hand-rolled rather than detectTransformGestures, which consumes a
            // one-finger drag as a pan whether or not there is anything to pan.
            // That swallowed the swipe: the pager never saw a horizontal drag
            // and the viewer would not move to the next photo. So a drag is
            // consumed only when it has work to do, which is two fingers
            // (a pinch) or one finger while zoomed in.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pointers = event.changes.count { it.pressed }
                        val mine = pointers >= 2 || currentScale > 1f
                        if (mine) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val next = (currentScale * zoom).coerceIn(1f, 6f)
                            if (next <= 1f) {
                                onTransformNow(1f, Offset.Zero)
                            } else {
                                // Clamp the pan, or the photo can be flung
                                // off-screen with no way back short of
                                // zooming out.
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
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    // Double tap toggles rather than only resetting: at 1x it
                    // is the fastest way into a leaf, and at any zoom it is the
                    // only escape that does not need a two-finger pinch.
                    onDoubleTap = {
                        if (currentScale > 1f) onTransformNow(1f, Offset.Zero)
                        else onTransformNow(2.5f, Offset.Zero)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        PlantPhoto(
            path = path,
            contentDescription = caption ?: "Plant photo",
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

/** Slow enough to see a leaf move, fast enough that 22 plates is 15 seconds. */
private const val PLAY_FRAME_MILLIS = 700L

/**
 * What travels with a shared photo.
 *
 * The photo's own caption was all that went before, and it is usually null, so
 * a shared picture of a leaf arrived with no message at all. Whoever receives
 * it has no idea which plant or when, which is most of the information.
 *
 * Plant then date then the caption, on two lines, because a messaging app will
 * show the first line and a date is the thing that makes a photo of a leaf mean
 * anything to somebody who does not live with it.
 */
private fun shareCaption(plantName: String, photo: Photo): String = buildString {
    append(plantName.ifBlank { "A plant" })
    append(", ")
    append(shareDate(photo))
    photo.caption?.takeIf { it.isNotBlank() }?.let {
        append("\n")
        append(it)
    }
}

/**
 * The filename a saved copy gets.
 *
 * It was `thirsttrap-<epoch millis>.jpg`, which is unreadable in a gallery and
 * sorts by nothing useful. The plant and the day make it findable six months
 * later, which is the whole reason somebody saves one out. MediaStore handles a
 * collision itself by appending a counter, so two photos of one plant on one
 * day do not need distinguishing here.
 */
private fun galleryName(plantName: String, photo: Photo): String {
    val slug = plantName.trim().lowercase()
        .map { if (it.isLetterOrDigit()) it else '-' }
        .joinToString("")
        .replace(Regex("-+"), "-")
        // Truncate first, then trim: a long name cut at forty characters can
        // land on a hyphen, which trimming beforehand would leave behind as
        // "...goes-on-and--2026-10-01.jpg".
        .take(40)
        .trim('-')
        .ifBlank { "plant" }
    val zone = ZoneOffset.ofTotalSeconds(photo.tzOffsetMinutes * 60)
    val day = Instant.ofEpochMilli(photo.takenAtMillis).atZone(zone)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    return "$slug-$day.jpg"
}

/** Day only. A time of day says nothing to somebody who was not there. */
private fun shareDate(photo: Photo): String {
    val zone = ZoneOffset.ofTotalSeconds(photo.tzOffsetMinutes * 60)
    return Instant.ofEpochMilli(photo.takenAtMillis).atZone(zone)
        .format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
}

private fun dateOf(photo: Photo): String {
    val zone = ZoneOffset.ofTotalSeconds(photo.tzOffsetMinutes * 60)
    return Instant.ofEpochMilli(photo.takenAtMillis).atZone(zone)
        .format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))
}
