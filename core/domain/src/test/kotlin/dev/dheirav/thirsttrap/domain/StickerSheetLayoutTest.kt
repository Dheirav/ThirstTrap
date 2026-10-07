package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StickerSheetLayoutTest {

    @Test
    fun cellSizeDoesNotDependOnHowManyPlantsThereAre() {
        // The point of the whole function: a label cut from a four-plant sheet
        // has to match one cut from a forty-plant sheet.
        val four = stickerSheetLayout(4, widthPx = 2480, minCellPx = 560)
        val forty = stickerSheetLayout(40, widthPx = 2480, minCellPx = 560)
        assertEquals(four.cellPx, forty.cellPx)
        assertEquals(four.columns, forty.columns)
    }

    @Test
    fun itFillsTheWidth() {
        val l = stickerSheetLayout(10, widthPx = 2480, minCellPx = 560)
        assertEquals(4, l.columns)
        assertTrue("cells must not exceed the page", l.cellPx * l.columns <= 2480)
        assertTrue("and must not waste a whole cell", l.gutterPx < l.cellPx)
    }

    @Test
    fun rowsRoundUp() {
        val l = stickerSheetLayout(9, widthPx = 2480, minCellPx = 560)
        assertEquals(4, l.columns)
        assertEquals("9 into 4 is 3 rows, not 2", 3, l.rows)
    }

    @Test
    fun aSinglePlantIsOneRow() {
        val l = stickerSheetLayout(1, widthPx = 2480, minCellPx = 560)
        assertEquals(1, l.rows)
    }

    @Test
    fun noPlantsIsNoRows() {
        assertEquals(0, stickerSheetLayout(0, widthPx = 2480, minCellPx = 560).rows)
    }

    @Test
    fun aPageTooNarrowForOneCellStillGetsOneColumn() {
        // Printing smaller than asked still scans; a sheet with zero columns
        // cannot be drawn at all.
        val l = stickerSheetLayout(3, widthPx = 300, minCellPx = 560)
        assertEquals(1, l.columns)
        assertEquals(300, l.cellPx)
        assertEquals(3, l.rows)
    }
}
