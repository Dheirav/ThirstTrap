package dev.dheirav.thirsttrap.domain

/**
 * How many sticker codes fit across a page, and how big each cell is.
 *
 * Here rather than beside the drawing code because it is arithmetic with edge
 * cases and no graphics in it: one plant, a hundred plants, and a page too
 * narrow to fit even one cell at the size asked for.
 */
data class SheetLayout(
    val columns: Int,
    val rows: Int,
    val cellPx: Int,
    /** Left over after the columns, so the grid can be centred rather than left-hung. */
    val gutterPx: Int,
)

/**
 * Lay [count] cells into [widthPx], each at least [minCellPx] across.
 *
 * Columns come from the page width rather than from the number of plants, so a
 * sheet of four and a sheet of forty print at the same size and a label cut
 * from one matches a label cut from the other. The alternative, fitting the
 * grid to the count, makes a four-plant sheet print four enormous codes and is
 * the kind of thing that looks clever until you tape them to pots.
 */
fun stickerSheetLayout(count: Int, widthPx: Int, minCellPx: Int): SheetLayout {
    require(count >= 0) { "count cannot be negative" }
    require(widthPx > 0 && minCellPx > 0) { "page and cell must be positive" }
    // At least one column even on a page too narrow for the requested size: a
    // code printed smaller than asked still scans, and a sheet with no columns
    // at all cannot be drawn.
    val columns = maxOf(1, widthPx / minCellPx)
    val cell = widthPx / columns
    val rows = if (count == 0) 0 else (count + columns - 1) / columns
    return SheetLayout(
        columns = columns,
        rows = rows,
        cellPx = cell,
        gutterPx = widthPx - cell * columns,
    )
}
