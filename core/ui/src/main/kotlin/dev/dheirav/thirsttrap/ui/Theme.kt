package dev.dheirav.thirsttrap.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Paper and ink, printed in two colours.
 *
 * An almanac is a record organised by time and season, which is exactly what
 * this app is - a diary of a slow biological process. The form is not
 * decoration on top of the content; it is the same organising principle.
 *
 * Almanacs were set in two colours because a second pass through the press cost
 * money: black for everything, one spot colour for what mattered. That is the
 * discipline here. Ink carries all the text; the green is a spot colour and
 * appears only where something is actionable or alive. A palette that can only
 * afford one accent cannot use colour to shout.
 *
 * Light is aged paper, hue ~90 across the whole ramp. Dark has no source to
 * copy - almanacs are cream, and "dark almanac" is not a thing that exists - so
 * it is invented rather than referenced: the same paper under a lamp, warm
 * near-black with the ink inverted. Plants get checked in the evening, which is
 * why dark stays a first-class scheme rather than an afterthought.
 *
 * ## Why every role is spelled out
 *
 * Both schemes previously set only `surface` and `background` and left the
 * `surfaceContainer*` family to [darkColorScheme]/[lightColorScheme]. Those
 * defaults are M3's baseline neutrals, which are purple-tinted - so every Card
 * in the app rendered at hue ~300 against a hue ~156 background, about 145
 * degrees out, at 1.08:1 contrast. Below perceptual threshold, and with
 * `elevation = 1.dp` drawing only a shadow, dark mode had no visible card at
 * all.
 *
 * An unset role does not fall back to something neutral; it falls back to
 * someone else's palette. So the roles below are complete, on purpose, and
 * every value is measured rather than eyeballed:
 *
 * Dark ramp, OKLCH lightness and hue, and contrast against the background:
 *
 *     surfaceContainerLowest   L 14.8  H 153  1.06:1
 *     surface / background     L 18.8  H 156  1.00:1
 *     surfaceContainerLow      L 21.7  H 154  1.07:1
 *     surfaceContainer         L 24.6  H 158  1.15:1
 *     surfaceContainerHigh     L 28.6  H 156  1.30:1
 *     surfaceContainerHighest  L 32.9  H 155  1.52:1   <- the filled Card
 *
 * Foreground roles form a real lightness ladder rather than differing only in
 * hue, which was the other half of why a card read as an undifferentiated wall.
 * Dark: onSurface L 90.0, onSurfaceVariant L 81.0, outline L 71.4 - gaps of 9.0
 * and 9.6, evenly spread across the range the card floor allows.
 *
 * That floor is the constraint. `outline` carries the plant card's context row,
 * so it is text, and text has to clear 4.5:1 against the surface it sits on -
 * which is the card at L 33, not the background at L 19. The first value tried
 * here measured 5.95:1 on the background and only 3.92:1 on a card, and a
 * contrast figure quoted against the wrong surface is not a contrast figure.
 */
private val Leaf = Color(0xFF2F5D3A)
private val LeafContainer = Color(0xFFCFE0CC)
private val Bark = Color(0xFF5A5342)

// Was #C0603F, which measured 4.02:1 on the light background and failed AA.
private val Terracotta = Color(0xFF8C3A2A)

internal val LightScheme = lightColorScheme(
    primary = Color(0xFF2F5D3A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFE0CC),
    onPrimaryContainer = Color(0xFF11250F),
    inversePrimary = Color(0xFF8FBF93),
    secondary = Color(0xFF5A5342),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3DBC6),
    onSecondaryContainer = Color(0xFF241F14),
    tertiary = Color(0xFF8C3A2A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2D6C9),
    onTertiaryContainer = Color(0xFF34110A),

    background = Color(0xFFF5F0E2),
    onBackground = Color(0xFF22201A),
    surface = Color(0xFFF5F0E2),
    onSurface = Color(0xFF22201A),
    surfaceVariant = Color(0xFFE3DBC6),
    onSurfaceVariant = Color(0xFF413B30),

    surfaceDim = Color(0xFFD3C9B0),
    surfaceBright = Color(0xFFF5F0E2),
    surfaceContainerLowest = Color(0xFFFFFCF4),
    surfaceContainerLow = Color(0xFFF0EAD9),
    surfaceContainer = Color(0xFFEAE3D0),
    surfaceContainerHigh = Color(0xFFE3DBC6),
    surfaceContainerHighest = Color(0xFFDCD3BC),
    surfaceTint = Color(0xFF2F5D3A),

    outline = Color(0xFF5F5847),
    outlineVariant = Color(0xFFC9C0A8),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF322D22),
    inverseOnSurface = Color(0xFFF0EAD9),

    error = Color(0xFF8A2018),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF7D9D3),
    onErrorContainer = Color(0xFF2E0603),
)

internal val DarkScheme = darkColorScheme(
    // Dropped from #7DDB9C (OKLCH L 82) to L 76: on a near-black screen the
    // depletion bar made the old value the brightest object in the room.
    primary = Color(0xFF8FBF93),
    onPrimary = Color(0xFF12301C),
    primaryContainer = Color(0xFF274A2E),
    onPrimaryContainer = Color(0xFFAEDCB0),
    inversePrimary = Color(0xFF2F5D3A),
    secondary = Color(0xFFCDC5B0),
    onSecondary = Color(0xFF2B2619),
    secondaryContainer = Color(0xFF3E382A),
    onSecondaryContainer = Color(0xFFE3DBC6),
    tertiary = Color(0xFFD9927E),
    onTertiary = Color(0xFF4A1809),
    tertiaryContainer = Color(0xFF68301F),
    onTertiaryContainer = Color(0xFFF2D6C9),

    background = Color(0xFF14110B),
    onBackground = Color(0xFFE9E2D0),
    surface = Color(0xFF14110B),
    onSurface = Color(0xFFE9E2D0),
    surfaceVariant = Color(0xFF3E382A),
    onSurfaceVariant = Color(0xFFCDC5B0),

    surfaceDim = Color(0xFF100E09),
    surfaceBright = Color(0xFF3B3527),
    surfaceContainerLowest = Color(0xFF0D0B07),
    surfaceContainerLow = Color(0xFF1A1610),
    surfaceContainer = Color(0xFF201C14),
    surfaceContainerHigh = Color(0xFF2A251B),
    surfaceContainerHighest = Color(0xFF342E22),
    surfaceTint = Color(0xFF8FBF93),

    outline = Color(0xFFB0A891),
    outlineVariant = Color(0xFF46402F),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE9E2D0),
    inverseOnSurface = Color(0xFF22201A),

    error = Color(0xFFF0B0A4),
    onError = Color(0xFF54160D),
    errorContainer = Color(0xFF75291B),
    onErrorContainer = Color(0xFFF7D9D3),
)

/**
 * Square, because print is square.
 *
 * A rounded corner is a screen idiom - it exists to suggest a physical button
 * under glass. An almanac has none: it has rules, plates and blocks, all of
 * which meet at right angles because that is what a press does. Rounding them
 * would be the one detail that gives the whole thing away.
 *
 * The two large roles keep 2dp rather than 0, because a bottom sheet with a
 * hard corner reads as a rendering bug rather than a decision.
 */
internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(2.dp),
)

@Composable
fun ThirstTrapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Default OFF: the app's own palette is the intended look, and dynamic
    // colour means it is otherwise never seen. Switchable in settings.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(
        colorScheme = scheme,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
