package dev.dheirav.thirsttrap.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Green, and deliberately not the saturated lime every gardening app reaches
 * for - but with enough life in it to read as a plant rather than as sage.
 * Dark is a first-class scheme, not an inversion: plants get checked in the
 * evening.
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
 * hue, which was the other half of why a card read as an undifferentiated wall:
 * onSurface L 90, onSurfaceVariant L 76, outline L 66.
 */
private val Leaf = Color(0xFF276B44)
private val LeafContainer = Color(0xFFB4EFC5)
private val Bark = Color(0xFF4F6354)

// Was #C0603F, which measured 4.02:1 on the light background and failed AA.
private val Terracotta = Color(0xFFA4482A)

internal val LightScheme = lightColorScheme(
    primary = Leaf,
    onPrimary = Color.White,
    primaryContainer = LeafContainer,
    onPrimaryContainer = Color(0xFF00210F),
    inversePrimary = Color(0xFF81C394),
    secondary = Bark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E8D6),
    onSecondaryContainer = Color(0xFF0C1F14),
    tertiary = Terracotta,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBCF),
    onTertiaryContainer = Color(0xFF3A0B00),

    background = Color(0xFFF7FBF3),
    onBackground = Color(0xFF191D18),
    surface = Color(0xFFF7FBF3),
    onSurface = Color(0xFF191D18),
    surfaceVariant = Color(0xFFDCE5DB),
    onSurfaceVariant = Color(0xFF414942),

    surfaceDim = Color(0xFFD7DCD4),
    surfaceBright = Color(0xFFF7FBF3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F6EE),
    surfaceContainer = Color(0xFFEBF0E8),
    surfaceContainerHigh = Color(0xFFE5EAE2),
    surfaceContainerHighest = Color(0xFFDFE4DC),
    surfaceTint = Leaf,

    outline = Color(0xFF717971),
    outlineVariant = Color(0xFFC1CAC0),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2E322D),
    inverseOnSurface = Color(0xFFEFF3EB),

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

internal val DarkScheme = darkColorScheme(
    // Dropped from #7DDB9C (OKLCH L 82) to L 76: on a near-black screen the
    // depletion bar made the old value the brightest object in the room.
    primary = Color(0xFF81C394),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF1F5236),
    onPrimaryContainer = Color(0xFF98F8B6),
    inversePrimary = Leaf,
    secondary = Color(0xFFA9C1B0),
    onSecondary = Color(0xFF223528),
    secondaryContainer = Color(0xFF384B3E),
    onSecondaryContainer = Color(0xFFD2E8D6),
    tertiary = Color(0xFFE8A98D),
    onTertiary = Color(0xFF5B1B06),
    tertiaryContainer = Color(0xFF7A3418),
    onTertiaryContainer = Color(0xFFFFDBCF),

    background = Color(0xFF0F1511),
    onBackground = Color(0xFFDAE0DA),
    surface = Color(0xFF0F1511),
    onSurface = Color(0xFFDAE0DA),
    surfaceVariant = Color(0xFF414942),
    onSurfaceVariant = Color(0xFFACB3AD),

    surfaceDim = Color(0xFF0C110E),
    surfaceBright = Color(0xFF343E37),
    surfaceContainerLowest = Color(0xFF080C09),
    surfaceContainerLow = Color(0xFF151C17),
    surfaceContainer = Color(0xFF1B231E),
    surfaceContainerHigh = Color(0xFF242D27),
    surfaceContainerHighest = Color(0xFF2E3831),
    surfaceTint = Color(0xFF81C394),

    outline = Color(0xFF8D948E),
    outlineVariant = Color(0xFF414A44),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFDAE0DA),
    inverseOnSurface = Color(0xFF191D18),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
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
    MaterialTheme(colorScheme = scheme, content = content)
}
