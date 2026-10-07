package dev.dheirav.thirsttrap.feature.qr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.stickerSheetLayout
import dev.dheirav.thirsttrap.domain.stickerSheetPages
import java.io.File
import java.io.FileOutputStream

/**
 * Every pot's sticker on one page, to print once and cut up.
 *
 * StickerScreen shows one code at a time and tells you to photograph the
 * screen, which is fine for one pot and absurd for thirty: thirty screenshots,
 * each a different size, none of them next to its own name. The thing somebody
 * actually wants is a sheet of paper.
 *
 * Black on white regardless of the app's theme. These get printed, and printing
 * the dark theme wastes a cartridge to make a code that scans worse.
 */
object StickerSheet {

    /**
     * A4 at 300dpi, and the image is always a whole number of these.
     *
     * The width fixes the printed size of a code: four columns across 210mm
     * makes each label about 52mm, which is what you want taped to a pot.
     *
     * The height went wrong twice. First it was forced to a full page whatever
     * the content, so four plants printed one row above two-thirds of blank
     * paper. Then it was trimmed to the content, which removed the blank paper
     * and broke printing instead: 2480x764 is not a paper size, so what comes
     * out depends on whether the print dialog fits, fills or centres it. An
     * image that is exactly N pages prints the same from any of them. The blank
     * space under a short sheet is not waste to design around; that is what a
     * part-used sheet of labels looks like.
     */
    private const val PAGE_W = 2480
    private const val PAGE_H = 3508

    /**
     * Roughly 8mm at 300dpi, kept clear all the way round.
     *
     * Every printer has an edge it cannot reach, and a code bled to the paper's
     * edge comes out with a side missing. A QR survives about 30% damage at
     * this error correction level, but not a missing finder pattern.
     */
    private const val MARGIN = 96

    /**
     * About 47mm at 300dpi, which is a label you can still read the name on
     * after it has been taped to a pot and watered round for a season.
     */
    private const val MIN_CELL = 560

    /**
     * Renders the sheet, or null if there is nothing to put on it.
     *
     * Returns the bitmap rather than writing it, so the caller decides between
     * the gallery and a share sheet and this stays testable by eye in isolation.
     */
    fun render(plants: List<Plant>): Bitmap? {
        if (plants.isEmpty()) return null
        val layout = stickerSheetLayout(plants.size, PAGE_W - MARGIN * 2, MIN_CELL)
        val rowsPerPage = maxOf(1, (PAGE_H - MARGIN * 2) / layout.cellPx)
        val pages = stickerSheetPages(layout.rows, rowsPerPage)
        val bmp = Bitmap.createBitmap(PAGE_W, pages * PAGE_H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val ink = Paint().apply { color = Color.BLACK; isAntiAlias = false }
        val label = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = layout.cellPx * 0.085f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        val left = MARGIN + layout.gutterPx / 2f
        plants.forEachIndexed { i, plant ->
            val col = i % layout.columns
            val row = i / layout.columns
            // Each page carries its own margin, or rows after the first page
            // creep up into the strip the printer cannot reach.
            val page = row / rowsPerPage
            val rowOnPage = row % rowsPerPage
            val x = left + col * layout.cellPx
            val y = page * PAGE_H + MARGIN + rowOnPage * layout.cellPx.toFloat()
            drawCode(canvas, plant.id, x, y, layout.cellPx.toFloat(), ink)
            // The name under the code, because a sheet of thirty identical
            // squares is unusable until you have scanned every one of them.
            canvas.drawText(
                ellipsise(plant.name, label, layout.cellPx * 0.9f),
                x + layout.cellPx / 2f,
                y + layout.cellPx * 0.96f,
                label,
            )
        }
        return bmp
    }

    /** Writes a rendered sheet into the cache as a PNG, for sharing or saving. */
    fun writeToCache(context: Context, bmp: Bitmap, name: String): File? {
        val out = File(context.cacheDir, name)
        return runCatching {
            FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            out
        }.getOrNull()
    }

    /**
     * One code, drawn at its natural module size.
     *
     * The same reasoning as PlantQrCode: asking the writer for a pixel matrix
     * of this size would build a 560x560 matrix and draw a quarter of a million
     * rectangles per code.
     */
    private fun drawCode(canvas: Canvas, plantId: String, x: Float, y: Float, cell: Float, ink: Paint) {
        val matrix = Encoder.encode(
            plantUri(plantId),
            ErrorCorrectionLevel.H,
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"),
        ).matrix
        val modules = matrix.width
        val quiet = 2
        // The bottom tenth of the cell is the name, so the code squares up in
        // what is left rather than overlapping it.
        val codeBox = cell * 0.86f
        val size = codeBox / (modules + quiet * 2)
        val originX = x + (cell - codeBox) / 2f
        for (my in 0 until matrix.height) {
            for (mx in 0 until modules) {
                if (matrix.get(mx, my).toInt() != 1) continue
                val px = originX + (mx + quiet) * size
                val py = y + (my + quiet) * size
                canvas.drawRect(px, py, px + size + 0.5f, py + size + 0.5f, ink)
            }
        }
    }

    /** Keeps a long name inside its cell rather than letting it run into the next one. */
    private fun ellipsise(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var cut = text.length
        while (cut > 1 && paint.measureText(text.take(cut) + "…") > maxWidth) cut--
        return text.take(cut) + "…"
    }
}
