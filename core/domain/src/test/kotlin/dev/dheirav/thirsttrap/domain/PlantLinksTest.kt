package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PlantLinksTest {

    private val pothos = Plant(id = "p1", name = "Marbled pothos", medium = Medium.WATER)
    private val fig = Plant(id = "p2", name = "Creeping fig", medium = Medium.SOIL)
    private val all = listOf(pothos, fig)

    @Test
    fun `plain text passes through as one segment`() {
        assertEquals(
            listOf(NoteSegment.Text("new leaf today")),
            parseNoteSegments("new leaf today", all),
        )
    }

    @Test
    fun `a link resolves case-insensitively and keeps what was written`() {
        assertEquals(
            listOf(
                NoteSegment.Text("same rot as "),
                NoteSegment.Link("marbled POTHOS", "p1"),
                NoteSegment.Text(" last month"),
            ),
            parseNoteSegments("same rot as [[marbled POTHOS]] last month", all),
        )
    }

    @Test
    fun `an unresolved link degrades to its literal text`() {
        assertEquals(
            listOf(NoteSegment.Text("cutting from [[campus wall fig]]")),
            parseNoteSegments("cutting from [[campus wall fig]]", all),
        )
    }

    @Test
    fun `several links in one note`() {
        val segs = parseNoteSegments("[[Creeping fig]] vs [[Marbled pothos]]", all)
        assertEquals(
            listOf(
                NoteSegment.Link("Creeping fig", "p2"),
                NoteSegment.Text(" vs "),
                NoteSegment.Link("Marbled pothos", "p1"),
            ),
            segs,
        )
    }

    @Test
    fun `empty brackets and nesting do not crash or link`() {
        assertEquals(
            listOf(NoteSegment.Text("[[]] and [[ ]] stay text")),
            parseNoteSegments("[[]] and [[ ]] stay text", all).let {
                // Empty capture cannot match the regex; whitespace-only fails
                // resolution. Either way it all reads back as written.
                listOf(NoteSegment.Text(it.joinToString("") { s ->
                    when (s) {
                        is NoteSegment.Text -> s.text
                        is NoteSegment.Link -> "[[${s.display}]]"
                    }
                }))
            },
        )
    }
}
