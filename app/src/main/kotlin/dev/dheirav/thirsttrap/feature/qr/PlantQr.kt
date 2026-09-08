package dev.dheirav.thirsttrap.feature.qr

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** The URI a sticker carries - the same one a reminder notification uses. */
fun plantUri(plantId: String): String = "thirsttrap://plant/$plantId"

/**
 * Drawn straight to the Canvas from the bit matrix rather than through a
 * Bitmap, so it stays crisp at whatever size it is shown or photographed at.
 *
 * High error correction, because these end up taped to a pot and get splashed,
 * scuffed and grown over. A QR at level H survives roughly 30% damage.
 */
@Composable
fun PlantQrCode(
    plantId: String,
    modifier: Modifier = Modifier,
    foreground: Color = Color.Black,
    background: Color = Color.White,
) {
    val matrix = remember(plantId) {
        QRCodeWriter().encode(
            plantUri(plantId),
            BarcodeFormat.QR_CODE,
            QR_SIZE,
            QR_SIZE,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 1,
            ),
        )
    }

    Canvas(modifier) {
        val cells = matrix.width
        val cell = size.minDimension / cells
        drawRect(background, size = Size(cell * cells, cell * cells))
        for (y in 0 until cells) {
            for (x in 0 until cells) {
                if (matrix.get(x, y)) {
                    drawRect(
                        color = foreground,
                        topLeft = Offset(x * cell, y * cell),
                        // A hair over one cell, or antialiasing leaves pale
                        // seams between modules that confuse some scanners.
                        size = Size(cell + 0.5f, cell + 0.5f),
                    )
                }
            }
        }
    }
}

private const val QR_SIZE = 512
