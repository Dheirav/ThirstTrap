package dev.dheirav.thirsttrap.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.hypot
import kotlin.math.pow

/**
 * Guards the bug that made every card in dark mode purple.
 *
 * Leaving a `surfaceContainer*` role unset does not fall back to something
 * neutral - it falls back to M3's baseline palette, which is purple-tinted.
 * The result passed review, compiled, and ran: the only way to catch it is to
 * measure the colours.
 *
 * These assertions are about perception, not taste. A hue 145 degrees from the
 * rest of the palette is a bug in any palette; a card 1.08:1 from its
 * background is invisible whatever colour it is.
 */
class ThemeTest {

    // --- OKLCH, because sRGB distance does not match what an eye reports ---

    private fun channel(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private data class Lch(val l: Double, val c: Double, val h: Double)

    private fun Color.oklch(): Lch {
        val r = channel(red); val g = channel(green); val b = channel(blue)
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        val lightness = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
        val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
        val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        var hue = Math.toDegrees(atan2(bb, a))
        if (hue < 0) hue += 360.0
        return Lch(lightness * 100, hypot(a, bb), hue)
    }

    private fun Color.relativeLuminance(): Double =
        0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)

    private fun contrast(a: Color, b: Color): Double {
        val x = a.relativeLuminance()
        val y = b.relativeLuminance()
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }

    /** Shortest way round the circle - 359 and 1 are two degrees apart. */
    private fun hueGap(a: Double, b: Double): Double {
        val d = abs(a - b) % 360.0
        return if (d > 180) 360 - d else d
    }

    private fun ColorScheme.surfaces() = listOf(
        "surfaceContainerLowest" to surfaceContainerLowest,
        "surface" to surface,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
    )

    @Test
    fun `every surface sits on the same hue as the background`() {
        listOf("dark" to DarkScheme, "light" to LightScheme).forEach { (which, scheme) ->
            val reference = scheme.background.oklch().h
            scheme.surfaces().forEach { (name, colour) ->
                val lch = colour.oklch()
                // Near-neutrals have unstable hue; below this chroma the hue is
                // arithmetic noise and not something an eye can see.
                if (lch.c < 0.004) return@forEach
                val gap = hueGap(lch.h, reference)
                assertTrue(
                    "$which $name is ${gap.toInt()} degrees off the background hue " +
                        "(${lch.h.toInt()} vs ${reference.toInt()}). An unset role falls " +
                        "back to M3's purple baseline - set it explicitly.",
                    gap < 30.0,
                )
            }
        }
    }

    @Test
    fun `the surface ramp actually climbs`() {
        listOf("dark" to DarkScheme, "light" to LightScheme).forEach { (which, scheme) ->
            val ls = scheme.surfaces().map { it.first to it.second.oklch().l }
            // Light mode's ramp descends, dark mode's ascends; either way each
            // step has to move, or the roles are decorative.
            val ascending = which == "dark"
            ls.zipWithNext().forEach { (a, b) ->
                val moved = if (ascending) b.second > a.second else b.second < a.second
                assertTrue(
                    "$which: ${a.first} (L ${a.second.toInt()}) to ${b.first} " +
                        "(L ${b.second.toInt()}) does not move the right way",
                    moved,
                )
            }
        }
    }

    @Test
    fun `a filled card is actually distinguishable from the background`() {
        // Material's filled Card takes surfaceContainerHighest. At 1.08:1 - what
        // the unset dark scheme produced - it is below perceptual threshold, and
        // the 1.dp elevation is a shadow, which is invisible on near-black.
        listOf("dark" to DarkScheme, "light" to LightScheme).forEach { (which, scheme) ->
            val ratio = contrast(scheme.surfaceContainerHighest, scheme.background)
            assertTrue(
                "$which: a card is only ${"%.2f".format(ratio)}:1 against the background",
                ratio >= 1.20,
            )
        }
    }

    @Test
    fun `text has a lightness ladder, not just three shades of one colour`() {
        listOf("dark" to DarkScheme, "light" to LightScheme).forEach { (which, scheme) ->
            val tiers = listOf(
                "onSurface" to scheme.onSurface.oklch().l,
                "onSurfaceVariant" to scheme.onSurfaceVariant.oklch().l,
                "outline" to scheme.outline.oklch().l,
            )
            tiers.zipWithNext().forEach { (a, b) ->
                assertTrue(
                    "$which: ${a.first} (L ${a.second.toInt()}) and ${b.first} " +
                        "(L ${b.second.toInt()}) are the same lightness, so the " +
                        "hierarchy rests entirely on hue",
                    abs(a.second - b.second) >= 8.0,
                )
            }
        }
    }

    @Test
    fun `body text meets WCAG AA on the surfaces it is actually drawn on`() {
        listOf("dark" to DarkScheme, "light" to LightScheme).forEach { (which, scheme) ->
            listOf(
                "onSurface" to scheme.onSurface,
                "onSurfaceVariant" to scheme.onSurfaceVariant,
                "primary" to scheme.primary,
                "tertiary" to scheme.tertiary,
            ).forEach { (name, fg) ->
                listOf(
                    "background" to scheme.background,
                    "card" to scheme.surfaceContainerHighest,
                ).forEach { (bgName, bg) ->
                    val ratio = contrast(fg, bg)
                    assertTrue(
                        "$which: $name on $bgName is ${"%.2f".format(ratio)}:1, under AA",
                        ratio >= 4.5,
                    )
                }
            }
        }
    }
}
