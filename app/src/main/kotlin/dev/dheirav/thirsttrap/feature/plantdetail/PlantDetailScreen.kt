package dev.dheirav.thirsttrap.feature.plantdetail

import dev.dheirav.thirsttrap.ui.MenuLabels
import dev.dheirav.thirsttrap.ui.ScreenTitle
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.EventColors
import dev.dheirav.thirsttrap.ui.fullBleed
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Row
import dev.dheirav.thirsttrap.ui.FilledTonalButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import dev.dheirav.thirsttrap.domain.hasSpeciesCare
import dev.dheirav.thirsttrap.domain.CareEvent
import androidx.compose.material3.TextButton
import androidx.compose.ui.layout.ContentScale
import dev.dheirav.thirsttrap.ui.PlantPhoto
import dev.dheirav.thirsttrap.ui.AlmanacMenu
import dev.dheirav.thirsttrap.ui.AlmanacDialog
import dev.dheirav.thirsttrap.ui.OutlinedButton
import dev.dheirav.thirsttrap.ui.DialogText
import dev.dheirav.thirsttrap.ui.PhotoViewer
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.CheckResult
import dev.dheirav.thirsttrap.domain.Photo
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.withLink

/**
 * The per-plant timeline. Requirements item 3.
 *
 * Without this you can log things but never look at what you logged, which
 * makes "was this better than a paper note?" unanswerable.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlantDetailScreen(
    advanced: Boolean,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onLogMore: (String) -> Unit,
    onDiagnose: (String) -> Unit,
    onWeigh: (String) -> Unit,
    onMeasureLight: (String) -> Unit,
    onSticker: (String) -> Unit,
    onCare: (String) -> Unit,
    /** Replaces this page with a neighbour's, so Back still means the dashboard. */
    onOpenPlant: (String) -> Unit,
    viewModel: PlantDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val neighbors by viewModel.neighbors.collectAsStateWithLifecycle()
    val plant = state.plant
    val capture = dev.dheirav.thirsttrap.photo.rememberPhotoCapture { viewModel.addPhoto(it) }
    var viewing by remember { mutableStateOf<Photo?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    val hero = state.photos.firstOrNull()?.let(viewModel::pathOf)

    // One Box so the viewer overlays the whole page. Two siblings in a
    // composable body only stack because the caller happens to place them
    // in a Box, which is not a contract worth depending on for a full-screen
    // overlay.
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            // The hero runs under the status bar, so this screen draws its own
            // insets rather than being pushed below them.
            contentWindowInsets = if (hero != null) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
            topBar = {
                TopAppBar(
                    // The name lives on the photo when there is one; repeating it in
                    // the bar would put the same four words on screen twice.
                    title = { if (hero == null) Text(plant?.name ?: "") },
                    colors = if (hero != null) {
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                            navigationIconContentColor = Color.White,
                            actionIconContentColor = Color.White,
                        )
                    } else {
                        TopAppBarDefaults.topAppBarColors()
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(AppIcons.arrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Two inline, the rest behind an overflow. Six icons plus a
                        // back arrow is 336dp of chrome on a 360dp phone, which left
                        // the plant's name - the screen's only identifier - with a
                        // few dp, and nothing at all at large font sizes.
                        if (plant != null) {
                            IconButton(onClick = capture.takePhoto) {
                                Icon(AppIcons.addAPhoto, contentDescription = "Take a photo")
                            }
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(AppIcons.moreVert, contentDescription = "More actions")
                            }
                            AlmanacMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                plant?.let { p ->
                                    // First, because it is the app's central
                                    // action and this page did not have it at
                                    // all: logging a watering with an amount, a
                                    // check, a repot or a feed was reachable
                                    // only through the dashboard's quick sheet.
                                    // That mattered once the dashboard's tap
                                    // started opening this page instead of the
                                    // sheet, or the full log screen would have
                                    // been left behind a long press.
                                    DropdownMenuItem(
                                        text = { Text(MenuLabels.Plant.LOG) },
                                        onClick = { menuOpen = false; onLogMore(p.id) },
                                    )
                                    // Second, under logging. "Something is
                                    // wrong with this plant" is something you
                                    // do at the moment you are worried, not a
                                    // help topic, and it was four taps deep
                                    // behind Settings and a word meaning "I am
                                    // confused". It is about this plant, so it
                                    // belongs on this plant.
                                    DropdownMenuItem(
                                        text = { Text(MenuLabels.Plant.DIAGNOSE) },
                                        onClick = { menuOpen = false; onDiagnose(p.id) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(MenuLabels.Plant.GALLERY) },
                                        onClick = { menuOpen = false; capture.pickFromGallery() },
                                    )
                                    // "Compare photos" and "Timelapse" used to sit
                                    // here, and both opened the photo viewer in a
                                    // particular state: one with the oldest photo
                                    // pinned, one starting at the oldest with Play
                                    // ready. D44 folded the timelapse screen into
                                    // the viewer and D48 folded Compare in after
                                    // it, which merged the screens and left their
                                    // two menu entries standing as separate doors.
                                    //
                                    // Three routes to one page, which the user
                                    // pointed out: tapping a thumbnail, and these
                                    // two. The thumbnails are right there on this
                                    // page whenever there are photos at all, so the
                                    // menu entries were the redundant pair. Pin and
                                    // Play live in the viewer, with the photos.
                                    //
                                    // What is lost is the "needs 2" hint, which was
                                    // the only thing telling somebody comparing
                                    // exists before they had two photos to compare.
                                    // That now lives in the viewer's own menu, which
                                    // is where you are when the question arises.
                                    if (p.isWeightTrackable) {
                                        DropdownMenuItem(
                                            // Third name for this screen. It was
                                            // "Weight and prediction", which named
                                            // its implementation, then "When it
                                            // needs water", which promises a date
                                            // the screen frequently refuses to give:
                                            // there are seven ways it can decline,
                                            // and a label that oversells sets up the
                                            // disappointment the "Why not?" link now
                                            // has to absorb.
                                            //
                                            // "How thirsty it is" is true in every
                                            // state, including before anything has
                                            // been weighed, and it matches the voice
                                            // the app already uses for "How dry
                                            // before watering" and "How does the pot
                                            // feel?".
                                            text = { Text(MenuLabels.Plant.WEIGHT) },
                                            onClick = { menuOpen = false; onWeigh(p.id) },
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(MenuLabels.Plant.LIGHT) },
                                        onClick = { menuOpen = false; onMeasureLight(p.id) },
                                    )
                                    // "Care notes" used to sit here always,
                                    // alongside the in-page "Care notes for
                                    // this species" button, which read as the
                                    // same thing offered twice. It was not a
                                    // duplicate, it was mislabelled: when the
                                    // catalogue has nothing this is the only
                                    // route to the online name lookup, because
                                    // LookupSection lives in the care screen's
                                    // empty state, and the in-page button only
                                    // appears when there are notes to read.
                                    //
                                    // So the two are now exclusive and each
                                    // says what it actually does. Notes on
                                    // file: the button on the page, and nothing
                                    // in the menu. Nothing on file: this, named
                                    // for the only thing the screen can offer.
                                    if (!hasSpeciesCare(p.species) && !hasSpeciesCare(p.name)) {
                                        DropdownMenuItem(
                                            text = { Text(MenuLabels.Plant.LOOKUP) },
                                            onClick = { menuOpen = false; onCare(p.id) },
                                        )
                                    }
                                    if (advanced) {
                                        DropdownMenuItem(
                                            text = { Text(MenuLabels.Plant.STICKER) },
                                            onClick = { menuOpen = false; onSticker(p.id) },
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(MenuLabels.Plant.EDIT) },
                                        onClick = { menuOpen = false; onEdit(p.id) },
                                    )
                                }
                            }
                        }
                    },
                )
            },
        ) { padding ->
            // Swiping sideways walks the dashboard's line of plants. The gesture
            // detector only claims drags that are horizontal past touch slop, so
            // the list's own scrolling is untouched - and a horizontal strip
            // inside the page (the photo row) consumes its drags first, which is
            // the right precedence: content wins over navigation.
            val swipeThresholdPx = with(LocalDensity.current) { 96.dp.toPx() }
            LazyColumn(
                // With a hero the list must start at the very top of the window so
                // the photo runs under the status bar and the app bar floats over
                // it; Scaffold's padding would otherwise push it below both.
                Modifier
                    .fillMaxSize()
                    .pointerInput(neighbors) {
                        var dragged = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragged = 0f },
                            onHorizontalDrag = { _, amount -> dragged += amount },
                            onDragEnd = {
                                when {
                                    // Finger moved left: the page slides away
                                    // leftwards, the next plant comes in.
                                    dragged < -swipeThresholdPx ->
                                        neighbors.nextId?.let(onOpenPlant)
                                    dragged > swipeThresholdPx ->
                                        neighbors.previousId?.let(onOpenPlant)
                                }
                            },
                        )
                    }
                    .padding(
                        if (hero != null) {
                            PaddingValues(bottom = padding.calculateBottomPadding())
                        } else {
                            padding
                        },
                    ),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (hero != null) 0.dp else 16.dp,
                    bottom = 16.dp,
                ),
            ) {
                if (hero != null) {
                    item {
                        // Full-bleed, under the status bar, per UI-SPEC section 4.
                        // fullBleed measures past the list's 16dp gutter rather than
                        // negating it - Compose rejects negative padding at runtime.
                        PlantHero(
                            path = hero,
                            name = plant?.name.orEmpty(),
                            chips = plantChips(plant),
                            // The cover photo is the largest image on the screen
                            // and tapping it did nothing at all.
                            onOpen = { state.photos.firstOrNull()?.let { viewing = it } },
                            modifier = Modifier.fullBleed(16.dp),
                        )
                    }
                }

                item {
                    Column(Modifier.padding(bottom = 16.dp)) {
                        if (hero == null) {
                            plant?.species?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                plantChips(plant).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        plant?.let { p ->
                            if (hasSpeciesCare(p.species) || hasSpeciesCare(p.name)) {
                                // A real 48dp target. contentPadding = 0 collapsed
                                // it to the text's own height, which is both hard
                                // to hit and under the accessibility floor.
                                FilledTonalButton(
                                    onClick = { onCare(p.id) },
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .heightIn(min = 48.dp),
                                ) { Text("Care notes for this species") }
                            }
                        }

                        // Requirements item 8. Only shown once there are two
                        // waterings to measure between - one is not a cadence.
                        state.averageIntervalDays?.let { avg ->
                            Text(
                                cadenceLabel(avg),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        Text(
                            "${state.totalEvents} ${if (state.totalEvents == 1) "entry" else "entries"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }

                if (state.loaded && state.photos.isEmpty()) {
                    item {
                        Text(
                            "No photos yet. The camera button above starts a record you can " +
                                "compare against later.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }
                }

                if (state.photos.isNotEmpty()) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 16.dp),
                        ) {
                            items(state.photos, key = { it.id }) { photo ->
                                PhotoThumb(
                                    path = viewModel.pathOf(photo),
                                    caption = photo.caption,
                                    onOpen = { viewing = photo },
                                )
                            }
                        }
                    }
                }

                // The one thing lost when "Compare photos" left this menu was
                // the greyed "needs 2" label, which was the only hint that
                // comparing exists before you have anything to compare. Said
                // here instead, where the photos are and where you would act
                // on it, and only at exactly one photo: at two it is no longer
                // news, and at none the line above already covers it.
                if (state.photos.size == 1) {
                    item {
                        Text(
                            "One more photo and you can put them side by side, or play " +
                                "them in order.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }
                }

                if (state.loaded && state.days.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("No history yet", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Log a watering or a check and it will show up here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                state.days.forEach { day ->
                    stickyHeader(key = day.label) {
                        Box(
                            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                                .padding(vertical = 8.dp),
                        ) {
                            Text(
                                day.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    items(day.events, key = { it.id }) { event ->
                        EventRow(
                            event = event,
                            photos = state.photosByEvent[event.id].orEmpty(),
                            pathOf = viewModel::pathOf,
                            onOpenPhoto = { viewing = it },
                            onDelete = { viewModel.deleteEvent(event) },
                            onSave = viewModel::updateEvent,
                            allPlants = state.allPlants,
                            onOpenPlant = onOpenPlant,
                        )
                    }
                }
            }
        }

        // Outside the Scaffold, not inside its content: an overlay that stopped at
        // the app bar would be a photo with a toolbar on top of it.
        viewing?.let { photo ->
            PhotoViewer(
                // The whole set rather than the one tapped, because the reason
                // to open a photo is usually to put it next to an older one,
                // and from here that is a swipe.
                photos = state.photos,
                startId = photo.id,
                plantName = plant?.name.orEmpty(),
                pathOf = viewModel::pathOf,
                onDismiss = { viewing = null },
                onSaveCaption = viewModel::setCaption,
                onSetCover = viewModel::setCover,
                onDelete = viewModel::deletePhoto,
            )
        }
    }

}

@Composable
private fun PhotoThumb(path: String, caption: String?, onOpen: () -> Unit) {
    Column {
        PlantPhoto(
            path = path,
            contentDescription = caption ?: "Plant photo",
            modifier = Modifier
                .size(120.dp)
                .clip(MaterialTheme.shapes.small)
                // One gesture. A tap used to open the caption-and-delete menu,
                // so the one thing you could not do with a photo was look at
                // it; those actions now live on the photo's own page, where
                // Delete is a visible control that asks rather than a hidden
                // long press drawn over the thumbnail it belongs to.
                .clickable(onClickLabel = "View this photo", onClick = onOpen),
        )
        caption?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp).width(120.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventRow(
    event: CareEvent,
    photos: List<Photo>,
    onOpenPhoto: (Photo) -> Unit,
    pathOf: (Photo) -> String,
    onDelete: () -> Unit,
    onSave: (CareEvent) -> Unit,
    allPlants: List<dev.dheirav.thirsttrap.domain.Plant> = emptyList(),
    onOpenPlant: (String) -> Unit = {},
) {
    var editing by remember(event.id) { mutableStateOf(false) }

    // Events that changed the plant's nature get a heavier treatment, so they
    // are findable while scrolling fast. docs/UI-SPEC.md section 4.
    val isLifeEvent = event.type in setOf(
        CareEventType.REPOTTED, CareEventType.MEDIUM_CHANGED, CareEventType.DIED,
    )

    Column {
        if (isLifeEvent) HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.tertiary)

        Row(
            Modifier
                .fillMaxWidth()
                // One gesture, and it opens the entry rather than a menu.
                //
                // A tap and a long press both used to open a dropdown whose
                // only item was Delete, drawn over the row it belonged to.
                // That is the same shape D44 took off the photos, and it is
                // the mechanism that destroyed three real diary entries during
                // development: a mis-aimed tap landing on a destructive item in
                // a menu positioned above the thing it was about.
                .clickable(onClickLabel = "Open this entry") { editing = true }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                // Order matters: size() then padding() shrinks the box to 8x2 and
                // the marker renders as a dash instead of a dot.
                // Colour names the activity, not the urgency - so the timeline
                // is scannable by eye without any colour meaning "bad".
                Modifier.padding(top = 6.dp).size(8.dp)
                    .background(EventColors.of(event.type), MaterialTheme.shapes.extraSmall),
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    label(event),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isLifeEvent) FontWeight.SemiBold else FontWeight.Normal,
                )
                event.note?.takeIf { it.isNotBlank() }?.let {
                    LinkedNote(it, allPlants, onOpenPlant)
                }
                if (photos.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        items(photos, key = { it.id }) { photo ->
                            PlantPhoto(
                                path = pathOf(photo),
                                contentDescription = photo.caption ?: "Photo",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable(onClickLabel = "View this photo") {
                                        onOpenPhoto(photo)
                                    },
                            )
                        }
                    }
                }
            }
            Text(
                timeOf(event),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )


        }
    }

    if (editing) {
        EntryEditor(
            event = event,
            photos = photos,
            pathOf = pathOf,
            onOpenPhoto = onOpenPhoto,
            onDismiss = { editing = false },
            onSave = { onSave(it); editing = false },
            onDelete = { editing = false; onDelete() },
        )
    }
}

/**
 * A diary entry, opened.
 *
 * Nothing could edit one before. A wrong note or a timestamp an hour out had no
 * home at all, and `PlantRepository.updateEvent` had existed with zero callers
 * since it was written: this is the only thing that has ever called it.
 *
 * Laid out and worded like the weigh-in editor, including the way Delete flips
 * the same dialog into a confirmation rather than opening a second one. Two
 * editors for two rows in two lists should not be two different interactions.
 *
 * Only the note and the day are editable. The type is not, because "this was a
 * watering, not a check" is a different entry rather than an edit of this one,
 * and the clock time is not, because an entry backdated to a day carries no
 * claim about the minute.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryEditor(
    event: CareEvent,
    photos: List<Photo>,
    pathOf: (Photo) -> String,
    onOpenPhoto: (Photo) -> Unit,
    onDismiss: () -> Unit,
    onSave: (CareEvent) -> Unit,
    onDelete: () -> Unit,
) {
    var note by remember(event.id) { mutableStateOf(event.note.orEmpty()) }
    var day by remember(event.id) { mutableStateOf(event.timestampMillis) }
    var confirmingDelete by remember(event.id) { mutableStateOf(false) }
    var picking by remember(event.id) { mutableStateOf(false) }

    AlmanacDialog(
        title = if (confirmingDelete) "Delete this entry?" else label(event),
        onDismissRequest = onDismiss,
        body = {
            if (confirmingDelete) {
                DialogText(
                    buildString {
                        append(label(event))
                        append(", ")
                        append(entryDate(day, event.tzOffsetMinutes))
                        append(". This cannot be undone, and unlike a weight it cannot be ")
                        append("measured again either.")
                        if (photos.isNotEmpty()) {
                            append(
                                " The ${if (photos.size == 1) "photo" else "photos"} " +
                                    "will stay with the plant.",
                            )
                        }
                    },
                )
            } else {
                OutlinedButton(onClick = { picking = true }) {
                    Text(entryDate(day, event.tzOffsetMinutes))
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    placeholder = { Text("what you noticed") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                if (photos.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        items(photos, key = { it.id }) { photo ->
                            PlantPhoto(
                                path = pathOf(photo),
                                contentDescription = photo.caption ?: "Photo",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable(onClickLabel = "View this photo") {
                                        onOpenPhoto(photo)
                                    },
                            )
                        }
                    }
                }
            }
        },
        dismiss = {
            if (confirmingDelete) {
                TextButton(onClick = { confirmingDelete = false }) { Text("Keep it") }
            } else {
                TextButton(onClick = { confirmingDelete = true }) { Text("Delete") }
            }
        },
        confirm = {
            if (confirmingDelete) {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(
                    onClick = {
                        onSave(
                            event.copy(
                                note = note.trim().takeIf { it.isNotEmpty() },
                                timestampMillis = day,
                            ),
                        )
                    },
                ) { Text("Save") }
            }
        },
    )

    if (picking) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = day,
            // Nothing in a diary happened tomorrow.
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis <= System.currentTimeMillis() + 86_400_000L
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    // Keeps the clock time and moves the day, so an entry
                    // corrected from Tuesday to Monday stays at 18:40 rather
                    // than jumping to midnight.
                    picker.selectedDateMillis?.let { day = keepTimeOfDay(day, it, event.tzOffsetMinutes) }
                    picking = false
                }) { Text("Use this day") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        ) { DatePicker(state = picker) }
    }
}

/** The day and the clock time, in the offset the entry was recorded in. */
private fun entryDate(millis: Long, tzOffsetMinutes: Int): String {
    val zone = ZoneOffset.ofTotalSeconds(tzOffsetMinutes * 60)
    return Instant.ofEpochMilli(millis).atZone(zone)
        .format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))
}

/**
 * Moves an instant onto a different day without moving it within the day.
 *
 * The picker hands back UTC midnight for the chosen date, which would drop the
 * time of day. Both halves are read in the entry's own offset, because that is
 * the wall clock the entry was written against.
 */
private fun keepTimeOfDay(original: Long, pickedUtcMidnight: Long, tzOffsetMinutes: Int): Long {
    val zone = ZoneOffset.ofTotalSeconds(tzOffsetMinutes * 60)
    val time = Instant.ofEpochMilli(original).atZone(zone).toLocalTime()
    val date = Instant.ofEpochMilli(pickedUtcMidnight).atZone(ZoneOffset.UTC).toLocalDate()
    return date.atTime(time).toInstant(zone).toEpochMilli()
}


/**
 * A note with [[plant]] links resolved (F12). Wiki semantics: a link that
 * resolves is a tap-through; one that does not reads as the text it is.
 */
@Composable
private fun LinkedNote(
    note: String,
    allPlants: List<dev.dheirav.thirsttrap.domain.Plant>,
    onOpenPlant: (String) -> Unit,
) {
    val segments = dev.dheirav.thirsttrap.domain.parseNoteSegments(note, allPlants)
    val linkColor = MaterialTheme.colorScheme.primary
    val text = androidx.compose.ui.text.buildAnnotatedString {
        for (s in segments) {
            when (s) {
                is dev.dheirav.thirsttrap.domain.NoteSegment.Text -> append(s.text)
                is dev.dheirav.thirsttrap.domain.NoteSegment.Link -> {
                    val link = androidx.compose.ui.text.LinkAnnotation.Clickable(
                        tag = s.plantId,
                        linkInteractionListener = { onOpenPlant(s.plantId) },
                    )
                    withLink(link) {
                        withStyle(
                            androidx.compose.ui.text.SpanStyle(
                                color = linkColor,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                            ),
                        ) { append(s.display) }
                    }
                }
            }
        }
    }
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun label(event: CareEvent): String = buildString {
    append(event.type.label)
    // The one place a check earns a suffix, and only when it says something.
    if (event.type == CareEventType.CHECKED && event.checkResult == CheckResult.STILL_HEAVY) {
        append(" - still wet")
    }
    event.amountMl?.let { append(" · ${it.toInt()} ml") }
}

/** Rendered in the offset the event was recorded in, not the device's current one. */
private fun timeOf(event: CareEvent): String {
    val zone = ZoneOffset.ofTotalSeconds(event.tzOffsetMinutes * 60)
    return Instant.ofEpochMilli(event.timestampMillis).atZone(zone)
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}

/** "every 1 days" is the kind of thing that makes an app feel unfinished. */
private fun cadenceLabel(avgDays: Double): String = when (val d = avgDays.toInt()) {
    0 -> "Waters more than once a day"
    1 -> "Waters roughly every day"
    else -> "Waters roughly every $d days"
}

/**
 * The facts worth putting next to a plant's name.
 *
 * Medium appears only when it is *not* soil. Soil is the default for almost
 * every pot in the app, so printing it says nothing the photograph has not
 * already said - it was on every dashboard card, captioning a picture of soil.
 * Semi-hydro or water is different: that is a fact about the pot you cannot
 * always see, and it changes how the weight model reads.
 */
private fun plantChips(plant: dev.dheirav.thirsttrap.domain.Plant?): List<String> = listOfNotNull(
    // People name a plant after what it is, and then the hero read
    // "Fittonia" over "Fittonia".
    plant?.species?.takeIf { it.isNotBlank() && !it.equals(plant.name, ignoreCase = true) },
    plant?.location?.takeIf { it.isNotBlank() },
    plant?.medium?.takeIf { it != dev.dheirav.thirsttrap.domain.Medium.SOIL }?.label?.lowercase(),
    plant?.containerDesc?.takeIf { it.isNotBlank() },
)

/**
 * The cover photo, full-bleed, with the name over it.
 *
 * The plant's own photograph is the best identifier the screen has and it was
 * previously a 120dp square below the fold. Two scrims make the overlay legible
 * without dimming the whole picture: one at the top for the bar's icons, one at
 * the bottom that resolves into the page background so the photo ends rather
 * than stops.
 */
@Composable
private fun PlantHero(
    path: String,
    name: String,
    chips: List<String>,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier
            .fillMaxWidth()
            .height(300.dp)
            .clickable(onClickLabel = "View this photo", onClick = onOpen),
    ) {
        PlantPhoto(
            path = path,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.55f),
                    0.22f to Color.Transparent,
                    0.62f to Color.Transparent,
                    1f to surface,
                ),
            ),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                // 32 = the 16dp the hero was shifted left by fullBleed, plus the
                // 16dp page gutter. Its own 16dp landed the name at x=0, hard
                // against the screen edge while every other line on the page
                // started at 16.
                .padding(start = 32.dp, end = 32.dp, bottom = 12.dp),
        ) {
            Text(
                name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (chips.isNotEmpty()) {
                Text(
                    chips.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
