package dev.dheirav.thirsttrap.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CardDefaults
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.layout.Layout
import kotlin.math.roundToInt

/**
 * A guided walk through the app, pointing at the real thing each time.
 *
 * Ported from Luna, which replaced four pages of explanation with this on
 * 5 Oct 2026 for the reason that applies here twice over: a person finds the
 * weighing round by tapping it, not by reading that it exists.
 *
 * ThirstTrap has the four pages. WayfindingScreen is 2483 characters of "what
 * is on each tab, what is behind the two dots menus", written because 31 routes
 * and two overflow menus are not discoverable. A tour does not make that index
 * better, it makes it unnecessary.
 */

/** The screens the tour walks through. */
enum class TourScreen { DASHBOARD, PLANT, DUE, MORE }

/** The real elements it points at. Each is tagged where it is drawn with [tourTarget]. */
enum class TourTarget {
    PLANT_CARD, WATER_BUTTON, STILL_WET_BUTTON, WEIGH_ROUND, MORE_DOTS,
    PLANT_MENU, DUE_TAB, PLANTS_TAB, BACK, DUE_LIST,
}

/**
 * One stop on the tour.
 *
 * [tapToContinue] steps are done by tapping the highlighted element itself,
 * which really navigates, and the tour moves on when the person arrives on
 * [leadsTo]. The rest have a Next button and block every touch, so the tour can
 * never change data: nothing that writes is ever a tap-through step, and
 * `nothingThatWritesIsTapThrough` in the tests holds that.
 */
data class TourStep(
    val screen: TourScreen,
    val target: TourTarget,
    val caption: String,
    val leadsTo: TourScreen? = null,
) {
    val tapToContinue: Boolean get() = leadsTo != null
}

/**
 * The tour, in order, following the daily loop rather than the menu structure.
 *
 * One short line each, said next to the thing it is about. The two quick-log
 * buttons are described and not tapped, because both of them write an entry.
 */
fun tourSteps(): List<TourStep> = listOf(
    TourStep(
        TourScreen.DASHBOARD, TourTarget.PLANT_CARD,
        "Each plant, with what it wants and when you last looked at it.",
    ),
    TourStep(
        TourScreen.DASHBOARD, TourTarget.WATER_BUTTON,
        "This logs a watering without opening anything.",
    ),
    // Said in the same breath as watering on purpose: the app refuses to make
    // watering the only answer, and a tour that only showed the droplet would
    // teach the opposite.
    TourStep(
        TourScreen.DASHBOARD, TourTarget.STILL_WET_BUTTON,
        "And this is for when you checked and it did not need any. Both count as looking.",
    ),
    TourStep(
        TourScreen.DASHBOARD, TourTarget.WEIGH_ROUND,
        "The weighing round, which is the thing this app is for. A pot tells you more than a calendar can.",
    ),
    TourStep(
        TourScreen.DASHBOARD, TourTarget.PLANT_CARD,
        "Tap a plant.", leadsTo = TourScreen.PLANT,
    ),
    TourStep(
        TourScreen.PLANT, TourTarget.PLANT_MENU,
        "Everything about this one plant is behind these dots, including how thirsty it is.",
    ),
    TourStep(
        TourScreen.PLANT, TourTarget.BACK,
        "Tap the arrow at the top left. Nothing was logged on the tour.",
        leadsTo = TourScreen.DASHBOARD,
    ),
    TourStep(
        TourScreen.DASHBOARD, TourTarget.DUE_TAB,
        "Tap Due.", leadsTo = TourScreen.DUE,
    ),
    TourStep(
        TourScreen.DUE, TourTarget.DUE_LIST,
        "Only what is worth a look today. Empty is the normal state and means nothing is wrong.",
    ),
    TourStep(
        TourScreen.DUE, TourTarget.PLANTS_TAB,
        "Tap Plants.", leadsTo = TourScreen.DASHBOARD,
    ),
    TourStep(
        TourScreen.DASHBOARD, TourTarget.MORE_DOTS,
        "The jobs that are about several plants at once live here. That is the end of the tour.",
    ),
)

/** The step after arriving on [screen]: the next one if the current step led there, else unchanged. */
fun advanceOnScreen(steps: List<TourStep>, index: Int, screen: TourScreen): Int =
    if (steps.getOrNull(index)?.leadsTo == screen) index + 1 else index

/**
 * Where the tour is, and where its targets were last drawn.
 *
 * Bounds are keyed by target and carry the token of the element that wrote them, because the same
 * target can exist on two screens (the back arrow) and the new screen's element registers before the
 * old one is disposed. Without the token, the old one's removal would erase the new one's position.
 */
@Stable
class TourController {
    var index by mutableIntStateOf(-1)
        private set
    val steps = tourSteps()
    val active: Boolean get() = index in steps.indices
    val step: TourStep? get() = steps.getOrNull(index)
    val activeTarget: TourTarget? get() = step?.target

    internal val bounds = mutableStateMapOf<TourTarget, Pair<Any, Rect>>()

    fun start() { index = 0 }
    fun next() { index = if (index + 1 < steps.size) index + 1 else -1 }
    fun back() { if (index > 0) index-- }
    fun stop() { index = -1 }
    fun onScreen(screen: TourScreen) { if (active) index = advanceOnScreen(steps, index, screen) }
}

/** Null when no tour is running anywhere, which is nearly always. */
val LocalTour = compositionLocalOf<TourController?> { null }

/**
 * Tags an element as a place the tour can point at, and brings it into view when it is pointed at.
 * Costs nothing when no tour is running.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tourTarget(target: TourTarget): Modifier = composed {
    val tour = LocalTour.current ?: return@composed this
    val token = remember { Any() }
    val requester = remember { BringIntoViewRequester() }
    DisposableEffect(target) {
        onDispose { if (tour.bounds[target]?.first === token) tour.bounds.remove(target) }
    }
    LaunchedEffect(tour.activeTarget) {
        if (tour.activeTarget == target) {
            delay(120)
            requester.bringIntoView()
        }
    }
    bringIntoViewRequester(requester)
        .onGloballyPositioned { tour.bounds[target] = token to it.boundsInRoot() }
}

/**
 * The spotlight: the app dimmed, the target cut out with a pulsing ring, and one caption beside it.
 *
 * Touches are blocked everywhere except, on a tap-through step, the target itself, which is left
 * uncovered so the real button takes the tap. [onMissing] is called if a tap-through step's target
 * never appears (an empty state that lacks it), so the person is never stuck.
 */
@Composable
fun TourOverlay(
    tour: TourController,
    onMissing: (TourStep) -> Unit,
    action: (@Composable (TourStep) -> Unit)? = null,
) {
    val step = tour.step ?: return
    val density = LocalDensity.current
    val hole = tour.bounds[step.target]?.second?.let { with(density) { it.inflate(8.dp.toPx()) } }
    val ring = MaterialTheme.colorScheme.primary

    // A target that never appears: skip a looking step, or carry out a tap step's navigation.
    LaunchedEffect(tour.index, hole == null) {
        if (hole == null) {
            delay(900)
            if (tour.bounds[step.target] == null) {
                if (step.tapToContinue) onMissing(step) else tour.next()
            }
        }
    }

    val pulse by rememberInfiniteTransition(label = "tour").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "ring",
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // The dimmed screen, with the target cut out. Draws only; touches are handled below.
        Canvas(Modifier.fillMaxSize()) {
            val path = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                hole?.let { addRoundRect(RoundRect(it, CornerRadius(18.dp.toPx()))) }
            }
            drawPath(path, Color.Black.copy(alpha = 0.66f))
            hole?.let {
                drawRoundRect(
                    color = ring.copy(alpha = pulse),
                    topLeft = it.topLeft,
                    size = it.size,
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }

        // Touch blockers. On a tap-through step the target's own rectangle is left open.
        if (hole != null && step.tapToContinue) {
            val blocks = listOf(
                Rect(0f, 0f, width, hole.top),
                Rect(0f, hole.bottom, width, height),
                Rect(0f, hole.top, hole.left, hole.bottom),
                Rect(hole.right, hole.top, width, hole.bottom),
            )
            blocks.filter { it.width > 0 && it.height > 0 }.forEach { Blocker(it) }
        } else {
            Blocker(Rect(0f, 0f, width, height))
        }

        // The caption, below the target if it is in the top half, above it otherwise, and always
        // kept between the status bar and the navigation bar. Placed by measuring the card first:
        // anchored blind from the bottom, a tall caption above a high target ran under the status
        // bar (device review m4).
        val margin = with(density) { 16.dp.toPx() }
        val below = hole == null || hole.center.y < height / 2
        val topInset = WindowInsets.statusBars.getTop(density).toFloat() + margin / 2
        val bottomInset = WindowInsets.navigationBars.getBottom(density).toFloat() + margin / 2
        Layout(
            content = {
                TourCaption(
                    step = step,
                    caption = step.caption,
                    position = "${tour.index + 1} of ${tour.steps.size}",
                    isLast = tour.index == tour.steps.lastIndex,
                    // Not on a tap-through step, whose caption asks for the screen's own back arrow:
                    // a second "Back" in the bubble meant something else (device review M7).
                    canGoBack = tour.index > 0 && !tour.steps[tour.index - 1].tapToContinue && !step.tapToContinue,
                    onNext = tour::next,
                    onBack = tour::back,
                    onSkip = tour::stop,
                    action = action,
                )
            },
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        ) { measurables, constraints ->
            val card = measurables.first().measure(constraints.copy(minWidth = constraints.maxWidth, minHeight = 0))
            layout(constraints.maxWidth, constraints.maxHeight) {
                val wanted = when {
                    hole == null -> height / 3
                    below -> hole.bottom + margin
                    else -> hole.top - margin - card.height
                }
                val lowest = (height - bottomInset - card.height).coerceAtLeast(topInset)
                card.place(0, wanted.coerceIn(topInset, lowest).roundToInt())
            }
        }
    }
}

/** Swallows every touch in [area], so nothing behind the spotlight can be pressed. */
@Composable
private fun Blocker(area: Rect) {
    val density = LocalDensity.current
    Box(
        Modifier
            .offset { IntOffset(area.left.roundToInt(), area.top.roundToInt()) }
            .size(with(density) { area.width.toDp() }, with(density) { area.height.toDp() })
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
    )
}

@Composable
private fun TourCaption(
    step: TourStep,
    caption: String,
    position: String,
    isLast: Boolean,
    canGoBack: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    action: (@Composable (TourStep) -> Unit)?,
) {
    // The project's own Card: a hairline box on the page's surface, not a raised
    // Material slab. A tour bubble is still part of this app.
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.Line)) {
            // Announced as it changes, so a screen reader follows the tour too.
            Text(
                caption,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            action?.invoke(step)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(position, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onSkip) { Text("Skip tour") }
                    if (canGoBack) OutlinedButton(onClick = onBack) { Text("Back") }
                    // A tap-through step is done by tapping the real thing, so it has no Next.
                    if (!step.tapToContinue) Button(onClick = onNext) { Text(if (isLast) "Done" else "Next") }
                }
            }
        }
    }
}
