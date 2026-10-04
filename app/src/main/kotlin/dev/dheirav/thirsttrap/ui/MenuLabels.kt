package dev.dheirav.thirsttrap.ui

/**
 * The names of the things in the app's two overflow menus, written once.
 *
 * These used to be string literals typed in two places: once where the menu
 * item is built, and again in the "Finding your way around" help page that
 * quotes it. On 2026-10-01 four help pages went stale within an hour of being
 * written, every time because a rename happened afterwards and only one of the
 * two copies moved. A map that is wrong is worse than no map, so the fix is to
 * make the drift impossible rather than to write a test that notices it.
 *
 * Only the names live here. The menu keeps its own gating and actions, and the
 * help page keeps its own descriptions, because those are genuinely different
 * concerns that happen to share a label.
 *
 * What this does **not** guarantee is completeness: adding a menu item without
 * adding it to the help page still goes unnoticed, and in fact had, twice. The
 * version that would guarantee it is a data-driven menu, where both render from
 * one list, and that was judged not worth it: the plant's menu has three
 * different conditions on it (advanced features, weight-trackable, whether the
 * species has care notes on file) and folding those into a list costs more
 * clarity than the problem is worth.
 */
object MenuLabels {

    /** The three bottom tabs. Quoted by the help page for the same reason. */
    object Tab {
        const val PLANTS = "Plants"
        const val DUE = "Due"
        const val SETTINGS = "Settings"
    }

    /** The dashboard's overflow: jobs that are about several plants at once. */
    object Dashboard {
        const val FEEDING = "Feeding"
        const val PROPAGATION = "Propagation board"
        const val SCAN = "Scan a pot sticker"
        const val PLACES = "Places"
        const val FIGURES = "Figures"
        const val EXPERIMENTS = "Experiments"

        /**
         * One line saying what each is for, held beside the label rather than
         * in the two screens that show it.
         *
         * This file already records four help pages going stale in an hour
         * because a label lived in two copies. These sentences were written in
         * WayfindingScreen and are now wanted on the More screen too, which is
         * the same trap one step later.
         */
        const val FEEDING_WHAT = "What is due a feed, and the dilution maths for a given can."
        const val PROPAGATION_WHAT = "Cuttings from cut to established, by stage."
        const val SCAN_WHAT = "Opens whichever plant's sticker you point the camera at."
        const val PLACES_WHAT = "A note and a light reading per spot in the house."
        const val FIGURES_WHAT =
            "How often you water, what became of things, and whether the predictions " +
                "have been any good."
        const val EXPERIMENTS_WHAT =
            "Two arms, one variable, for when you want an answer rather than an impression."
    }

    /** One plant's overflow: everything that is about that plant. */
    object Plant {
        const val LOG = "Log something"
        const val DIAGNOSE = "Something looks wrong"
        const val GALLERY = "Add from gallery"
        const val WEIGHT = "How thirsty it is"
        const val LIGHT = "Measure the light here"
        const val LOOKUP = "Look up this species"
        const val STICKER = "Pot sticker"
        const val EDIT = "Edit plant"
    }
}
