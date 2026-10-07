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

/**
 * How many whole pages the grid needs, so the image is an exact number of them.
 *
 * The first version trimmed the image to its contents, which removed a blank
 * two-thirds of a page and broke printing: a 2480x764 image is not a paper size,
 * so what comes out depends on whether the print dialog decides to fit, fill or
 * centre it. An image that is exactly N pages tall prints the same way from any
 * of them, and a label is 52mm whether you printed four or forty.
 *
 * The blank space under a short sheet is not waste to design around. That is
 * what a part-used sheet of labels looks like.
 */
fun stickerSheetPages(rows: Int, rowsPerPage: Int): Int {
    require(rowsPerPage > 0) { "a page must hold at least one row" }
    if (rows <= 0) return 0
    return (rows + rowsPerPage - 1) / rowsPerPage
}
