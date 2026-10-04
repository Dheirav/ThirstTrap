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

/*
 * THE CONVENTION, because the values alone are not enough.
 *
 * Two idioms are in use across the app: eleven screens put verticalArrangement
 * on the root and nothing on the children, ten put nothing on the root and a
 * padding(top =) on every child, and a few do both. Where both appear the gap
 * the reader sees is the SUM, so the number written at a call site is not the
 * number on screen, which is how a scale stops meaning anything.
 *
 * The rule, for anything written from here on:
 *
 *   - Gaps that are all the same -> verticalArrangement on the parent, nothing
 *     on the children.
 *   - Gaps that vary -> padding on each child, nothing on the parent.
 *   - Never both on the same container. A base rhythm plus per-child extras is
 *     defensible and this app uses it in places, but it has to be a decision
 *     written down at that container, not an accident.
 *
 * The existing call sites are deliberately NOT converted. There are 133 of them
 * across 21 screens, the payoff is maintainability rather than appearance, and
 * doing it without being able to look at the result risks visibly wrong spacing
 * on screens nobody has checked. It is worth doing with a device in hand.
 */
