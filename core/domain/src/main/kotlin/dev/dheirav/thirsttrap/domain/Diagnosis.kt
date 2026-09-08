package dev.dheirav.thirsttrap.domain

/**
 * Guided decision trees for the common panics. Requirements item 19.
 *
 * These exist because the useful question is almost never the one people ask.
 * "Is my plant dying?" has no answer; "is that fuzz moving when you dunk it?"
 * does. Every branch asks for one observation the user can actually make in
 * front of the plant, and every leaf says what to do rather than what it is.
 *
 * The trees are deliberately blunt about uncertainty. Where a symptom has
 * several plausible causes and no way to tell them apart from the armchair,
 * the leaf says so.
 */
data class DiagnosisTree(
    val id: String,
    val title: String,
    val opener: String,
    val root: DiagnosisNode,
)

sealed interface DiagnosisNode {
    data class Question(
        val prompt: String,
        /** Something the user can check right now, not knowledge they need. */
        val how: String? = null,
        val answers: List<Answer>,
    ) : DiagnosisNode

    data class Outcome(
        val verdict: String,
        val whatToDo: List<String>,
        /** True when this is reassurance rather than a problem. */
        val benign: Boolean = false,
    ) : DiagnosisNode
}

data class Answer(val label: String, val next: DiagnosisNode)

private typealias Q = DiagnosisNode.Question
private typealias Out = DiagnosisNode.Outcome

/** The four the requirements name. */
val diagnosisTrees: List<DiagnosisTree> = listOf(

    DiagnosisTree(
        id = "fuzz",
        title = "White fuzz on the roots or stem",
        opener = "Almost everyone meets this in water propagation and assumes the worst. " +
            "Usually it is the plant, not a fungus.",
        root = Q(
            prompt = "Dunk the fuzzy part in water and swirl it gently. What happens?",
            how = "Root hairs lie flat and vanish into a smooth surface when wet. " +
                "Mould stays fluffy, clumps together, and often comes away.",
            answers = listOf(
                Answer(
                    "It flattens and looks like a normal root",
                    Out(
                        verdict = "Root hairs. Exactly what you want to see.",
                        benign = true,
                        whatToDo = listOf(
                            "Nothing to do. This is the plant growing.",
                            "Do not scrub them off - they are how it drinks.",
                        ),
                    ),
                ),
                Answer(
                    "It stays fluffy or comes away in clumps",
                    Q(
                        prompt = "Does the stem below it feel firm or soft?",
                        how = "Pinch it gently between finger and thumb.",
                        answers = listOf(
                            Answer(
                                "Firm",
                                Out(
                                    verdict = "Surface mould on a healthy cutting. " +
                                        "Annoying, not fatal.",
                                    whatToDo = listOf(
                                        "Rinse the cutting and change the water.",
                                        "Change the water every two or three days from now on.",
                                        "Move it somewhere with a little more air movement.",
                                        "Log it as a treatment, so you can see whether it returns.",
                                    ),
                                ),
                            ),
                            Answer(
                                "Soft or slimy",
                                Out(
                                    verdict = "Rot has started. The mould is a symptom, " +
                                        "not the cause.",
                                    whatToDo = listOf(
                                        "Cut back to firm, pale tissue with a clean blade.",
                                        "Rinse well and start again in fresh water.",
                                        "If no firm tissue is left, this one is gone - take a " +
                                            "cutting from higher up if any remains.",
                                        "Log it, so a pattern across cuttings becomes visible.",
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    ),

    DiagnosisTree(
        id = "browning",
        title = "Browning or blackening",
        opener = "The texture tells you far more than the colour does.",
        root = Q(
            prompt = "Press the brown area gently. Firm and dry, or soft and wet?",
            how = "Dry and papery means the tissue died and stopped. Soft and wet means " +
                "something is still happening.",
            answers = listOf(
                Answer(
                    "Dry, papery, firm",
                    Q(
                        prompt = "Where is it?",
                        answers = listOf(
                            Answer(
                                "Leaf tips and edges",
                                Out(
                                    verdict = "Almost always water or air, rarely disease.",
                                    whatToDo = listOf(
                                        "Check whether the pot dries out completely between waterings.",
                                        "Dry indoor air does this too, especially near heating.",
                                        "Trim the brown if it bothers you - it will not turn green again.",
                                        "Weighing this one would settle the watering half properly.",
                                    ),
                                ),
                            ),
                            Answer(
                                "Whole leaves, lowest first",
                                Out(
                                    verdict = "Often just the plant retiring old leaves.",
                                    benign = true,
                                    whatToDo = listOf(
                                        "One or two at a time from the bottom is normal.",
                                        "If it is spreading upward or moving fast, come back " +
                                            "through the soft-and-wet branch instead.",
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                Answer(
                    "Soft, wet, or smells off",
                    Out(
                        verdict = "Rot, and it spreads. Worth acting on today.",
                        whatToDo = listOf(
                            "Take it out of the pot and look at the roots.",
                            "Healthy roots are firm and pale. Rotten ones are brown, soft, " +
                                "and pull apart.",
                            "Cut away everything soft with a clean blade.",
                            "Repot into fresh, barely damp medium - and log the repot, which " +
                                "clears the weight calibration, since the pot has changed.",
                            "Water far less until new growth appears.",
                        ),
                    ),
                ),
            ),
        ),
    ),

    DiagnosisTree(
        id = "cutface",
        title = "Reading a cut face",
        opener = "Before a cutting goes in water, the cut end says whether it is worth trying.",
        root = Q(
            prompt = "Look straight at the cut surface. What do you see?",
            how = "A fresh, clean cut is uniform and pale. Look for a ring or patch of a " +
                "different colour.",
            answers = listOf(
                Answer(
                    "Uniform and pale all the way across",
                    Out(
                        verdict = "A good cutting. Go ahead.",
                        benign = true,
                        whatToDo = listOf(
                            "Into water, sphagnum or damp soil, whichever you prefer.",
                            "Add it as a plant with source \"cutting\" and it appears on " +
                                "the propagation board.",
                        ),
                    ),
                ),
                Answer(
                    "A brown or dark ring, or a dark core",
                    Out(
                        verdict = "The rot reaches further up than the cut. It will not " +
                            "root like this.",
                        whatToDo = listOf(
                            "Cut higher, a centimetre at a time, until the face is clean.",
                            "If the whole stem is discoloured, this one will not take.",
                        ),
                    ),
                ),
                Answer(
                    "Crushed, torn or squashed",
                    Out(
                        verdict = "A torn cut struggles to callus and invites rot.",
                        whatToDo = listOf(
                            "Recut cleanly with a blade or sharp scissors, not by pulling.",
                            "For succulents and cacti, let it dry a day or two before planting.",
                        ),
                    ),
                ),
            ),
        ),
    ),

    DiagnosisTree(
        id = "leafdrop",
        title = "Dropping leaves",
        opener = "Timing separates the causes better than the leaves do.",
        root = Q(
            prompt = "Has anything about where it lives changed in the last month?",
            how = "Moved, repotted, a new draught, heating switched on, seasons turning.",
            answers = listOf(
                Answer(
                    "Yes, something changed",
                    Out(
                        verdict = "Almost certainly a sulk, not an illness.",
                        benign = true,
                        whatToDo = listOf(
                            "Leave it where it is - moving it again restarts the clock.",
                            "Keep water steady and wait a few weeks.",
                            "Log the move, so next time you can see how long it took to settle.",
                        ),
                    ),
                ),
                Answer(
                    "No, nothing changed",
                    Q(
                        prompt = "Are the dropped leaves yellow and soft, or dry and crisp?",
                        answers = listOf(
                            Answer(
                                "Yellow and soft",
                                Out(
                                    verdict = "Usually too much water rather than too little.",
                                    whatToDo = listOf(
                                        "Check the pot actually drains, and empty the saucer.",
                                        "Let it dry further between waterings than you have been.",
                                        "Weighing it is the only way to know how wet it really gets.",
                                    ),
                                ),
                            ),
                            Answer(
                                "Dry and crisp",
                                Out(
                                    verdict = "Usually too little water, or air far too dry.",
                                    whatToDo = listOf(
                                        "Check whether water runs down the sides without soaking " +
                                            "in - a shrunken root ball does that.",
                                        "A long soak from the bottom rewets it properly.",
                                        "A sudden jump in drying rate is the sign of this, and " +
                                            "the weight screen flags it if you weigh regularly.",
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    ),
)

fun diagnosisTreeById(id: String): DiagnosisTree? = diagnosisTrees.firstOrNull { it.id == id }
