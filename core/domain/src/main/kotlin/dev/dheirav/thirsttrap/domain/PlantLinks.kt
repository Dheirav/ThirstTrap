package dev.dheirav.thirsttrap.domain

/**
 * F12. `[[marbled pothos]]` in a note becomes a tap-through to that plant.
 *
 * Resolution is by name, case-insensitive, against the living collection at
 * render time - not stored as an id. That is deliberate: a note is text the
 * user wrote, and rewriting it to hold ids would mean the note changes when
 * the app's understanding changes. A link that stops resolving (plant renamed
 * or deleted) degrades to its literal text, which still reads fine - the same
 * graceful failure as a wiki.
 */
sealed interface NoteSegment {
    data class Text(val text: String) : NoteSegment

    /** A resolved link: [display] is what was written, [plantId] where it goes. */
    data class Link(val display: String, val plantId: String) : NoteSegment
}

private val LINK = Regex("""\[\[([^\[\]]+)]]""")

fun parseNoteSegments(note: String, plants: List<Plant>): List<NoteSegment> {
    if ("[[" !in note) return listOf(NoteSegment.Text(note))
    val byName = plants.associateBy { it.name.trim().lowercase() }

    val out = mutableListOf<NoteSegment>()
    var last = 0
    for (m in LINK.findAll(note)) {
        if (m.range.first > last) out += NoteSegment.Text(note.substring(last, m.range.first))
        val name = m.groupValues[1].trim()
        val target = byName[name.lowercase()]
        out += if (target != null) {
            NoteSegment.Link(display = name, plantId = target.id)
        } else {
            // An unresolved link is not an error - it reads as the text it is,
            // and starts resolving the day a plant with that name exists.
            NoteSegment.Text(m.value)
        }
        last = m.range.last + 1
    }
    if (last < note.length) out += NoteSegment.Text(note.substring(last))
    // Adjacent text runs merge, so an unresolved link does not split the note
    // into fragments a caller has to reassemble.
    val merged = mutableListOf<NoteSegment>()
    for (s in out) {
        val prev = merged.lastOrNull()
        if (s is NoteSegment.Text && prev is NoteSegment.Text) {
            merged[merged.size - 1] = NoteSegment.Text(prev.text + s.text)
        } else {
            merged += s
        }
    }
    return merged
}
