package dev.dheirav.thirsttrap.feature.qr

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

/** The URI a sticker carries - the same one a reminder notification uses. */
fun plantUri(plantId: String): String = "thirsttrap://plant/$plantId"

/**
 * The QR at its natural module resolution - about 33x33 for a URI this length.
 *
 * Encoder.encode is used rather than QRCodeWriter.encode(content, format, w, h),
 * which returns a matrix of that many PIXELS. Asking it for 512x512 produced a
 * 512x512 matrix, and drawing a rectangle per cell meant 262,144 draw calls per
 * frame: a 7.5 second frame and a genuine ANR on the device. At natural
 * resolution it is roughly 1,100 rectangles.
 */
@Composable
fun PlantQrCode(
    plantId: String,
    modifier: Modifier = Modifier,
    foreground: Color = Color.Black,
    background: Color = Color.White,
) {
    val matrix = remember(plantId) {
        Encoder.encode(
            plantUri(plantId),
            // Level H survives roughly 30% damage. These get taped to pots and
            // are splashed, scuffed and grown over.
            ErrorCorrectionLevel.H,
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"),
        ).matrix
    }

    Canvas(modifier) {
        val cells = matrix.width
        // A quiet zone, or scanners struggle to find the code at all.
        val quiet = 2
        val total = cells + quiet * 2
        val cell = size.minDimension / total

        drawRect(background, size = Size(cell * total, cell * total))
        for (y in 0 until matrix.height) {
            for (x in 0 until cells) {
                if (matrix.get(x, y).toInt() == 1) {
                    drawRect(
                        color = foreground,
                        topLeft = Offset((x + quiet) * cell, (y + quiet) * cell),
                        // A hair over one cell, or antialiasing leaves pale
                        // seams between modules that confuse some scanners.
                        size = Size(cell + 0.5f, cell + 0.5f),
                    )
                }
            }
        }
    }
}
