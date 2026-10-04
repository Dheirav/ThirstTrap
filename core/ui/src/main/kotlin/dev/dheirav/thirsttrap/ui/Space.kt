package dev.dheirav.thirsttrap.ui

import androidx.compose.ui.unit.dp

/**
 * The spacing scale. Seven steps, and nothing else.
 *
 * The app used fifteen distinct gap values. The Law of Proximity only carries
 * information if the distances are consistent: if 10dp and 12dp both appear,
 * neither can mean anything, because the reader cannot tell a deliberate
 * grouping from a typo. Seven steps are distinguishable; fifteen are noise.
 *
 * The strays map mechanically onto these: 6 and 10 become [Line], 14 becomes
 * [Entry], 18 and 20 become [Section], 28 becomes [Page]. Nothing moves more
 * than 4dp, which is why adopting this reads as tightening rather than redesign.
 */
object Space {
    /**
     * The one deliberate sub-grid value, and the only one.
     *
     * Named so that it stops being a free choice: 2dp is for a hairline's
     * breathing room, not for a gap between things. If a layout wants 2dp
     * anywhere else, it wants [Tight].
     */
    val Hair = 2.dp

    /** Inside one thing: a label and the value it labels. */
    val Tight = 4.dp

    /** Between lines of the same block. */
    val Line = 8.dp

    /** Between entries in a list, and the default inset of a boxed note. */
    val Entry = 12.dp

    /** Between blocks that are read together. */
    val Block = 16.dp

    /** Between sections of a page. */
    val Section = 24.dp

    /** The margin of a page, and the breathing room around an empty one. */
    val Page = 32.dp
}
