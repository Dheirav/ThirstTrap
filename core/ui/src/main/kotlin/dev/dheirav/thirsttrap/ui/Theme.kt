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
private val Leaf = Color(0xFF4C6B4F)
private val LeafLight = Color(0xFFCFE5CE)
private val Bark = Color(0xFF4A5B52)
private val Terracotta = Color(0xFFA6624A)

private val LightScheme = lightColorScheme(
    primary = Leaf,
    onPrimary = Color.White,
    primaryContainer = LeafLight,
    onPrimaryContainer = Color(0xFF0B1F10),
    secondary = Bark,
    tertiary = Terracotta,
    background = Color(0xFFFBFDF8),
    surface = Color(0xFFFBFDF8),
    surfaceVariant = Color(0xFFDEE5DA),
    onSurfaceVariant = Color(0xFF424940),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB2CDB1),
    onPrimary = Color(0xFF1E3623),
    primaryContainer = Color(0xFF354D38),
    onPrimaryContainer = LeafLight,
    secondary = Color(0xFFB6CCBF),
    tertiary = Color(0xFFEDB9A5),
    background = Color(0xFF10140F),
    surface = Color(0xFF10140F),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BD),
)

@Composable
fun ThirstTrapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
