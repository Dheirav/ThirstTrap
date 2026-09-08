package dev.dheirav.thirsttrap.feature.plantdetail

import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.EventColors
import dev.dheirav.thirsttrap.ui.fullBleed
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.layout.ContentScale
import dev.dheirav.thirsttrap.ui.PlantPhoto
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.CheckResult
import dev.dheirav.thirsttrap.domain.Photo
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * The per-plant timeline. Requirements item 3.
 *
 * Without this you can log things but never look at what you logged, which
 * makes "was this better than a paper note?" unanswerable.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlantDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onCompare: (String) -> Unit,
    onWeigh: (String) -> Unit,
    onMeasureLight: (String) -> Unit,
    onSticker: (String) -> Unit,
    onCare: (String) -> Unit,
    viewModel: PlantDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plant = state.plant
    val capture = dev.dheirav.thirsttrap.photo.rememberPhotoCapture { viewModel.addPhoto(it) }
    var captionFor by remember { mutableStateOf<Photo?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    val hero = state.photos.firstOrNull()?.let(viewModel::pathOf)

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
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            plant?.let { p ->
                                DropdownMenuItem(
                                    text = { Text("Add from gallery") },
                                    onClick = { menuOpen = false; capture.pickFromGallery() },
                                )
                                // Shown always, disabled with a reason. Hiding it
                                // meant the feature vanished exactly when someone
                                // would go looking for it.
                                DropdownMenuItem(
                                    text = { Text("Compare photos") },
                                    enabled = state.photos.size >= 2,
                                    trailingIcon = {
                                        if (state.photos.size < 2) {
                                            Text(
                                                "needs 2",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    onClick = { menuOpen = false; onCompare(p.id) },
                                )
                                if (p.isWeightTrackable) {
                                    DropdownMenuItem(
                                        text = { Text("Weight and prediction") },
                                        onClick = { menuOpen = false; onWeigh(p.id) },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Measure the light here") },
                                    onClick = { menuOpen = false; onMeasureLight(p.id) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Care notes") },
                                    // Deliberately NOT disabled when the
                                    // catalogue has nothing. The care screen's
                                    // empty state is the only route to the
                                    // online name lookup, and disabling this
                                    // made it unreachable for exactly the
                                    // plants it exists for. The hint below
                                    // still sets the expectation.
                                    trailingIcon = {
                                        if (!hasSpeciesCare(p.species) && !hasSpeciesCare(p.name)) {
                                            Text(
                                                "not on file",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    onClick = { menuOpen = false; onCare(p.id) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Pot sticker") },
                                    onClick = { menuOpen = false; onSticker(p.id) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit plant") },
                                    onClick = { menuOpen = false; onEdit(p.id) },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            // With a hero the list must start at the very top of the window so
            // the photo runs under the status bar and the app bar floats over
            // it; Scaffold's padding would otherwise push it below both.
            Modifier
                .fillMaxSize()
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
                                onLongPress = { captionFor = photo },
                            )
                        }
                    }
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
                        onDelete = { viewModel.deleteEvent(event) },
                    )
                }
            }
        }
    }

    captionFor?.let { photo ->
        CaptionDialog(
            photo = photo,
            initial = photo.caption.orEmpty(),
            onDismiss = { captionFor = null },
            onSave = { viewModel.setCaption(photo.id, it) },
            onCover = { viewModel.setCover(photo.id) },
            onDelete = { viewModel.deletePhoto(photo.id) },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoThumb(path: String, caption: String?, onLongPress: () -> Unit) {
    Column {
        PlantPhoto(
            path = path,
            contentDescription = caption ?: "Plant photo",
            modifier = Modifier
                .size(120.dp)
                .clip(MaterialTheme.shapes.small)
                .combinedClickable(
                    onClick = onLongPress,
                    onClickLabel = "Caption or delete this photo",
                    onLongClick = onLongPress,
                ),
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
    pathOf: (Photo) -> String,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }

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
                .combinedClickable(
                    onClick = { menu = true },
                    onClickLabel = "Options for this entry",
                    onLongClick = { menu = true },
                )
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
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.small),
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

            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Delete this entry") },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun CaptionDialog(
    photo: Photo,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onCover: () -> Unit,
    onDelete: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Photo") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Caption") },
                placeholder = { Text("brown spot on the lower leaf") },
            )
        },
        confirmButton = {
            Row {
                TextButton(onClick = { onCover(); onDismiss() }) { Text("Set as cover") }
                TextButton(onClick = { onSave(text); onDismiss() }) { Text("Save") }
            }
        },
        dismissButton = {
            TextButton(onClick = { onDelete(); onDismiss() }) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
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
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier
            .fillMaxWidth()
            .height(300.dp),
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
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
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
