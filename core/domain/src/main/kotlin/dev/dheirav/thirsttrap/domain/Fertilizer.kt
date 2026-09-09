package dev.dheirav.thirsttrap.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * A bottle you own, and what its label says to do with it.
 *
 * Requirements item 14. The inventory half is bookkeeping; the calculator half
 * is the one that earns its place, because the arithmetic is done standing at a
 * sink holding a bottle, which is where people get it wrong.
 */
@Serializable
data class Fertilizer(
    val id: String,
    val name: String,
    /** Exactly as typed off the label. The fact; [dilution] is its reading. */
    val dilutionText: String? = null,
    /** [dilutionText] parsed, or null when it says nothing the app can act on. */
    val dilution: Dilution? = null,
    /** "3-1-2", free text. Recorded, never interpreted. */
    val npk: String? = null,
    val note: String? = null,
    val archived: Boolean = false,
)

/**
 * How strong to make it. Two forms, because bottles use two.
 *
 * There is no third case for "whatever the bottle said in words", because a
 * dilution the app cannot read is one it must not act on. [parseDilution]
 * returns null rather than guessing, and the calculator then declines to give
 * a number instead of giving a wrong one.
 */
@Serializable
sealed interface Dilution {
    /** 1:200, meaning one part concentrate to [partsWater] parts water. */
    @Serializable
    data class Ratio(val partsWater: Double) : Dilution

    /** "5 mL per litre". */
    @Serializable
    data class MlPerLitre(val ml: Double) : Dilution

    val label: String
        get() = when (this) {
            is Ratio -> "1:${partsWater.tidy()}"
            is MlPerLitre -> "${ml.tidy()} ml/L"
        }
}

private fun Double.tidy(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString().trimEnd('0').trimEnd('.')

/**
 * Reads what someone typed off a bottle.
 *
 * Deliberately narrow. It accepts the two forms it can act on and says nothing
 * about anything else: "a capful", "1 tsp per gallon" and "as directed" all
 * return null, and the screen then asks for a number rather than pretending to
 * have understood.
 */
fun parseDilution(text: String?): Dilution? {
    val t = text?.trim()?.lowercase()?.replace(",", ".") ?: return null
    if (t.isEmpty()) return null

    Regex("""^1\s*[:/]\s*(\d+(?:\.\d+)?)$""").find(t)?.let {
        val n = it.groupValues[1].toDoubleOrNull() ?: return null
        return if (n > 0) Dilution.Ratio(n) else null
    }
    // "5ml/l", "5 ml per litre", "5 ml per liter", "5 ml/litre", "5 per l"
    Regex("""^(\d+(?:\.\d+)?)\s*(?:ml)?\s*(?:/|per)\s*(?:1\s*)?(?:l|litre|liter)$""")
        .find(t)?.let {
            val n = it.groupValues[1].toDoubleOrNull() ?: return null
            return if (n > 0) Dilution.MlPerLitre(n) else null
        }
    return null
}

/** How much concentrate goes into [waterMl] of water. */
fun concentrateMl(dilution: Dilution, waterMl: Double): Double = when (dilution) {
    is Dilution.Ratio -> waterMl / dilution.partsWater
    is Dilution.MlPerLitre -> dilution.ml * waterMl / 1000.0
}

/**
 * Below this, the answer is real but nobody can pour it. A kitchen measuring
 * spoon starts around 1 ml and a syringe around 0.2, so anything under a fifth
 * of a millilitre is a number the app can compute and a person cannot act on.
 */
const val UNPOURABLE_ML = 0.2

/** What to actually do, including when the honest answer is "not like this". */
sealed interface DoseAdvice {
    /** Pour this much concentrate into the can. */
    data class Pour(val concentrateMl: Double, val waterMl: Double) : DoseAdvice

    /**
     * The dose is too small to measure at this can size. Carries the volume
     * that would make it measurable, because "use a bigger can" is useless
     * without a number, and mixing a batch is what people actually do.
     */
    data class TooSmall(val concentrateMl: Double, val suggestedWaterMl: Double) : DoseAdvice

    /** No usable dilution recorded, so there is nothing to compute. */
    data object Unknown : DoseAdvice
}

fun doseFor(dilution: Dilution?, waterMl: Double): DoseAdvice {
    if (dilution == null || waterMl <= 0) return DoseAdvice.Unknown
    val ml = concentrateMl(dilution, waterMl)
    if (ml >= UNPOURABLE_ML) return DoseAdvice.Pour(ml, waterMl)
    // The smallest whole litre that lifts the dose over the threshold.
    val needed = when (dilution) {
        is Dilution.Ratio -> UNPOURABLE_ML * dilution.partsWater
        is Dilution.MlPerLitre -> UNPOURABLE_ML * 1000.0 / dilution.ml
    }
    return DoseAdvice.TooSmall(ml, kotlin.math.ceil(needed / 1000.0) * 1000.0)
}

interface FertilizerRepository {
    fun observeAll(): Flow<List<Fertilizer>>
    suspend fun upsert(fertilizer: Fertilizer)
    suspend fun delete(id: String)
}
