package dev.dheirav.thirsttrap.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.dheirav.thirsttrap.domain.CareEventType

/**
 * A colour per kind of activity, not per level of urgency.
 *
 * The best idea in this category, taken from Planta: the colour names *what you
 * did* - water, soil, pruning, light - rather than how overdue something is.
 * Because the hue carries meaning, it never has to carry alarm, which is what
 * lets a timeline be scannable by colour in an app where nothing is ever a
 * failure.
 *
 * All of these sit within a few OKLCH lightness points of each other on
 * purpose. None is brighter, more saturated or more urgent than the others, so
 * no event type can read as a warning. A red here would undo the entire
 * argument of docs/UI-SPEC.md section 3.
 */
object EventColors {

    private val WaterDark = Color(0xFF8FB4CC)      // slate
    private val SoilDark = Color(0xFFC2A878)       // earth
    private val GrowthDark = Color(0xFF9FC49C)     // leaf
    private val PruneDark = Color(0xFFAFBE93)      // olive
    private val TroubleDark = Color(0xFFD6A98F)    // terracotta, never red
    private val LifeDark = Color(0xFFC5A9CC)       // the events that bracket a life

    private val WaterLight = Color(0xFF3E6480)
    private val SoilLight = Color(0xFF6F5628)
    private val GrowthLight = Color(0xFF3F6B45)
    private val PruneLight = Color(0xFF566334)
    private val TroubleLight = Color(0xFF8A4C2E)
    private val LifeLight = Color(0xFF6A4A73)

    @Composable
    @ReadOnlyComposable
    fun of(type: CareEventType): Color {
        // The theme's own surface tells us which scheme is active without
        // needing the flag threaded down from the top of the app.
        val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        return when (type) {
            CareEventType.WATERED,
            CareEventType.WATER_CHANGED -> if (dark) WaterDark else WaterLight

            CareEventType.REPOTTED,
            CareEventType.MEDIUM_CHANGED,
            CareEventType.FERTILIZED,
            CareEventType.WEEDED -> if (dark) SoilDark else SoilLight

            CareEventType.PRUNED -> if (dark) PruneDark else PruneLight

            CareEventType.PEST_OR_DISEASE,
            CareEventType.TREATED -> if (dark) TroubleDark else TroubleLight

            CareEventType.MILESTONE,
            CareEventType.MOVED,
            CareEventType.DIED -> if (dark) LifeDark else LifeLight

            CareEventType.CHECKED,
            CareEventType.OBSERVATION,
            CareEventType.UNKNOWN -> if (dark) GrowthDark else GrowthLight
        }
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
