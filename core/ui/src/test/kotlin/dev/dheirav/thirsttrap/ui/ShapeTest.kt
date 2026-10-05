package dev.dheirav.thirsttrap.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The shape scale is square, and stays square.
 *
 * ThemeTest fails the build when a colour drifts, and there was no equivalent
 * for shape, which is how two shape languages came to share a screen. The
 * settings page ran Material's pill toggles beside blocks with hard corners,
 * and the dashboard drew an 18dp rounded ring around a photo clipped to this
 * scale's 0dp, so the frame bowed away from the image at every corner.
 *
 * This pins the scale rather than the widgets. A pill gets into the app one of
 * two ways: the shape scale grows a radius, which this catches, or a Material
 * component ignores the scale and reads its own token, which it cannot catch
 * and which is why Buttons.kt and Toggle.kt shadow those components by name.
 */
class ShapeTest {

    private val density = Density(1f)
    private val box = Size(100f, 100f)

    private fun corners(name: String, shape: androidx.compose.ui.graphics.Shape): List<Float> {
        val r = shape as? RoundedCornerShape
            ?: error("$name is not a RoundedCornerShape, so its corners cannot be checked")
        return listOf(r.topStart, r.topEnd, r.bottomEnd, r.bottomStart)
            .map { it.toPx(box, density) }
    }

    @Test
    fun `every corner in the shape scale is square or near enough`() {
        val scale = mapOf(
            "extraSmall" to AppShapes.extraSmall,
            "small" to AppShapes.small,
            "medium" to AppShapes.medium,
            "large" to AppShapes.large,
            "extraLarge" to AppShapes.extraLarge,
        )
        for ((name, shape) in scale) {
            for (px in corners(name, shape)) {
                // 2dp is the documented ceiling: the two large roles keep it so
                // a bottom sheet's corner does not read as a rendering bug.
                assertTrue(
                    "$name has a ${px}px corner at density 1, which is above the 2dp ceiling",
                    px <= 2f,
                )
            }
        }
    }

    @Test
    fun `the roles a photo and its ring share resolve to the same corner`() {
        // The depletion ring is drawn concentric with the plant photo by reading
        // this role's corner and adding its own offset. If medium ever stops
        // being uniform, that derivation needs revisiting rather than silently
        // following one corner of four.
        val c = corners("medium", AppShapes.medium)
        assertTrue("medium is not uniform across its four corners: $c", c.toSet().size == 1)
    }
}
