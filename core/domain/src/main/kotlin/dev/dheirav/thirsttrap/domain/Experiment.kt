package dev.dheirav.thirsttrap.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * F11. A named study: one variable, some subjects, a conclusion.
 *
 * The shape comes from a real experiment this diary already ran informally -
 * flax germination, banana water against plain, verdict on day 5. The pieces
 * that experiment had to keep in chat messages are exactly the fields here:
 * what was varied, who the subjects were and which arm each was in, and what
 * was concluded. Everything else (photos, observations, weights) already
 * lives on the subject plants; an experiment points at them rather than
 * duplicating them.
 *
 * Concluding is a one-way door on purpose. An experiment whose conclusion can
 * be quietly rewritten later is a lab notebook in pencil - the write-up is the
 * result, dated. Getting it wrong is what a new experiment is for.
 */
@Serializable
data class Experiment(
    val id: String,
    val name: String,
    /** What is being varied, e.g. "banana water 1:10 vs plain water". */
    val variable: String,
    val startedAtMillis: Long,
    val tzOffsetMinutes: Int,
    val concludedAtMillis: Long? = null,
    val conclusion: String? = null,
    val note: String? = null,
) {
    val isConcluded: Boolean get() = concludedAtMillis != null
}

/** One plant's role in an experiment: which arm it belongs to. */
@Serializable
data class ExperimentSubject(
    val experimentId: String,
    val plantId: String,
    /** The arm, e.g. "banana water" or "control". */
    val label: String,
)

data class ExperimentWithSubjects(
    val experiment: Experiment,
    val subjects: List<ExperimentSubject> = emptyList(),
)

/**
 * "Day 5" for the header, counted in civil days from the start, in the
 * timezone the experiment started in. Day 1 is the start day itself - nobody
 * calls the sowing day "day 0" out loud.
 */
fun experimentDayNumber(startedAtMillis: Long, tzOffsetMinutes: Int, nowMillis: Long): Int {
    val startDay = Math.floorDiv(startedAtMillis + tzOffsetMinutes * 60_000L, MILLIS_PER_DAY.toLong())
    val nowDay = Math.floorDiv(nowMillis + tzOffsetMinutes * 60_000L, MILLIS_PER_DAY.toLong())
    return (nowDay - startDay).toInt() + 1
}

interface ExperimentRepository {
    fun observeAll(): Flow<List<ExperimentWithSubjects>>
    fun observe(experimentId: String): Flow<ExperimentWithSubjects?>
    suspend fun create(name: String, variable: String, note: String?): String
    suspend fun addSubject(experimentId: String, plantId: String, label: String)
    suspend fun removeSubject(experimentId: String, plantId: String)
    suspend fun conclude(experimentId: String, conclusion: String)
    suspend fun delete(experimentId: String)
}
