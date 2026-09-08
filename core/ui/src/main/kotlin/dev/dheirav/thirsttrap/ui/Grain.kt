package dev.dheirav.thirsttrap.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.ImageBitmap

/**
 * A barely-there paper grain over large flat surfaces.
 *
 * Two reasons, one practical and one not. Practically, a large near-black area
 * of a single colour bands visibly on an OLED panel, and noise is the standard
 * fix. Less practically, it is most of what separates "dark theme" from
 * "unlit": flat #0F1511 is a void, and the same colour with grain reads as a
 * surface.
 *
 * The rule from the research is that if a user notices it, there is too much of
 * it. This is a 128px tile of Gaussian noise centred on mid-grey, composited
 * with [BlendMode.Overlay] - which leaves a mid-grey source pixel exactly
 * neutral, so the noise is zero-mean and textures the surface without shifting
 * its colour. Straight alpha compositing cannot do that: at a strength low
 * enough not to lift a near-black background it quantises to nothing at all,
 * which is exactly what the first attempt here did.
 */
fun Modifier.grain(strength: Float = 0.55f): Modifier = composed {
    val tile = grainBrush()
    drawWithCache {
        onDrawWithContent {
            drawContent()
            drawRect(brush = tile, alpha = strength, blendMode = BlendMode.Overlay)
        }
    }
}

@Composable
private fun grainBrush(): ShaderBrush {
    val image = ImageBitmap.imageResource(R.drawable.grain)
    return ShaderBrush(ImageShader(image, TileMode.Repeated, TileMode.Repeated))
}
