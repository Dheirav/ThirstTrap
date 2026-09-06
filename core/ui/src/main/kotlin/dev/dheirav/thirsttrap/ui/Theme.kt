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
 * Muted green, deliberately. The saturated palette every gardening app reaches
 * for reads as a toy, and this is a diary that gets opened in the evening —
 * which is also why the dark scheme is a first-class thing rather than an
 * inversion of the light one.
 */
/**
 * Green, and deliberately not the saturated lime every gardening app reaches
 * for - but with enough life in it to read as a plant rather than as sage.
 * Dark is a first-class scheme, not an inversion: plants get checked in the
 * evening.
 */
private val Leaf = Color(0xFF2E7D4F)
private val LeafContainer = Color(0xFFB4EFC5)
private val Bark = Color(0xFF4F6354)
private val Terracotta = Color(0xFFC0603F)

private val LightScheme = lightColorScheme(
    primary = Leaf,
    onPrimary = Color.White,
    primaryContainer = LeafContainer,
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Bark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2E8D6),
    onSecondaryContainer = Color(0xFF0C1F14),
    tertiary = Terracotta,
    onTertiary = Color.White,
    background = Color(0xFFF7FBF3),
    onBackground = Color(0xFF191D18),
    surface = Color(0xFFF7FBF3),
    onSurface = Color(0xFF191D18),
    surfaceVariant = Color(0xFFDCE5DB),
    onSurfaceVariant = Color(0xFF414942),
    outline = Color(0xFF717971),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF7DDB9C),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF1F5236),
    onPrimaryContainer = Color(0xFF98F8B6),
    secondary = Color(0xFFB6CCBB),
    onSecondary = Color(0xFF223528),
    secondaryContainer = Color(0xFF384B3E),
    onSecondaryContainer = Color(0xFFD2E8D6),
    tertiary = Color(0xFFFFB59B),
    onTertiary = Color(0xFF5B1B06),
    background = Color(0xFF0F1511),
    onBackground = Color(0xFFE0E4DC),
    surface = Color(0xFF0F1511),
    onSurface = Color(0xFFE0E4DC),
    surfaceVariant = Color(0xFF414942),
    onSurfaceVariant = Color(0xFFC0C9C0),
    outline = Color(0xFF8B938A),
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
