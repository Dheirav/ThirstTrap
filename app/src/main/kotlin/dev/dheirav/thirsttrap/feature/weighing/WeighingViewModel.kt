package dev.dheirav.thirsttrap.feature.weighing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.Prediction
import dev.dheirav.thirsttrap.domain.ReadingContext
import dev.dheirav.thirsttrap.domain.ReminderRepository
import dev.dheirav.thirsttrap.domain.SuppressionReason
import dev.dheirav.thirsttrap.domain.WeightReading
import dev.dheirav.thirsttrap.domain.POST_WATER_WINDOW_MILLIS
import dev.dheirav.thirsttrap.domain.WeightRepository
import dev.dheirav.thirsttrap.domain.suggestReadingContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeighingRow(
    val plant: Plant,
    val last: WeightReading?,
    /** Recorded during this round, so the list can show what is already done. */
    val doneThisRound: WeightReading? = null,
    val lastWateredMillis: Long? = null,
    /**
     * Watered recently and not weighed since, so this pot's reading is the wet
     * anchor rather than an ordinary sample. Computed against the same window
     * as the keypad's chip: the row and the chip have to agree, or the list
     * says "just watered" over a keypad that has already decided otherwise.
     */
    val owesWetMark: Boolean = false,
)

data class WeighingUiState(
    val rows: List<WeighingRow> = emptyList(),
    val loaded: Boolean = false,
) {
    val remaining: Int get() = rows.count { it.doneThisRound == null }
}

/**
 * A weighing round: every pot in one sitting.
 *
 * Weighing is not a per-plant activity even though the data is per-plant. The
 * scale comes out once and every pot goes on it, so making somebody navigate
 * plant, menu, weight, back, plant, menu, weight for each one was asking them
 * to pay four taps of overhead per reading on the app's most-repeated action.
 */
@HiltViewModel
class WeighingViewModel @Inject constructor(
    plants: PlantRepository,
    private val weights: WeightRepository,
    private val reminders: ReminderRepository,
) : ViewModel() {

    /** Session start, so "done" means done in this round rather than ever. */
    private val startedAtMillis = System.currentTimeMillis()

    val state: StateFlow<WeighingUiState> =
        combine(
            plants.observePlants(includeArchived = false),
            weights.observeAllReadings(),
            // The round needs the waterings too, or every reading taken after a
            // watering round gets filed as routine and no wet anchor is ever
            // recaptured. Watering and weighing are one moment.
            plants.observeAllEvents(),
        ) { plantList, readings, events ->
            val now = System.currentTimeMillis()
            val byPlant = readings.groupBy { it.plantId }
            val wateredAt = events
                .filter { it.type == CareEventType.WATERED }
                .groupBy { it.plantId }
                .mapValues { (_, e) -> e.maxOf { it.timestampMillis } }
            WeighingUiState(
                rows = plantList
                    // A cutting in a jar of water weighs what the jar weighs.
                    .filter { it.isWeightTrackable }
                    .map { plant ->
                        val mine = byPlant[plant.id]?.sortedBy { r -> r.timestampMillis }.orEmpty()
                        val last = mine.lastOrNull { !it.excluded }
                        val watered = wateredAt[plant.id]
                        WeighingRow(
                            plant = plant,
                            last = last,
                            doneThisRound = mine.lastOrNull { it.timestampMillis >= startedAtMillis },
                            lastWateredMillis = watered,
                            owesWetMark = watered != null &&
                                now - watered in 0..POST_WATER_WINDOW_MILLIS &&
                                (last == null || last.timestampMillis < watered),
                        )
                    },
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeighingUiState())

    private val _entry = MutableStateFlow("")
    val entry: StateFlow<String> = _entry.asStateFlow()

    private val _context = MutableStateFlow(ReadingContext.ROUTINE)
    val context: StateFlow<ReadingContext> = _context.asStateFlow()

    /** Which plant the keypad is currently for, by index into [WeighingUiState.rows]. */
    private val _cursor = MutableStateFlow<Int?>(null)
    val cursor: StateFlow<Int?> = _cursor.asStateFlow()

    fun open(index: Int) {
        _cursor.value = index
        _entry.value = ""
        val row = state.value.rows.getOrNull(index)
        _context.value = if (row == null) {
            ReadingContext.ROUTINE
        } else {
            // Per pot, not per round: on one trip round the house some plants
            // were just watered and some were not.
            //
            // No prediction is passed, so the round never guesses PRE_WATER.
            // That would mean assembling the full weight state for every plant
            // on every emission, and the chip it would set is the one the user
            // is about to change anyway. The post-water case is the one that
            // silently corrupts an anchor if it is got wrong.
            suggestReadingContext(
                prediction = Prediction.NeedAnotherReading(SuppressionReason.NOT_CALIBRATED),
                lastWateredMillis = row.lastWateredMillis,
                lastReadingMillis = row.last?.timestampMillis,
                nowMillis = System.currentTimeMillis(),
            )
        }
    }

    fun close() {
        _cursor.value = null
        _entry.value = ""
    }

    fun onDigit(d: Char) { if (_entry.value.length < 6) _entry.value += d }
    fun onBackspace() { _entry.value = _entry.value.dropLast(1) }
    fun onContext(c: ReadingContext) { _context.value = c }

    /**
     * Saves and moves to the next pot that has not been weighed this round.
     *
     * Advancing past the ones already done is what makes this a round rather
     * than a list: put a pot down, pick the next one up, and the app is already
     * asking for the right number.
     */
    fun saveAndAdvance() {
        val index = _cursor.value ?: return
        val rows = state.value.rows
        val row = rows.getOrNull(index) ?: return
        val grams = _entry.value.toDoubleOrNull() ?: return
        val context = _context.value
        viewModelScope.launch {
            weights.addReading(row.plant.id, grams, context)
            // A reading changes the prediction, and the prediction is what the
            // reminder is made of. The per-plant weight screen already did this
            // and the round did not, so weighing everything in one sitting left
            // every schedule untouched.
            reminders.rescheduleFromModel(row.plant.id, System.currentTimeMillis())
            val next = rows.indices.firstOrNull { i ->
                i != index && rows[i].doneThisRound == null
            }
            if (next == null) {
                close()
            } else {
                open(next)
            }
        }
    }

    fun skip() {
        val index = _cursor.value ?: return
        val rows = state.value.rows
        val next = rows.indices.firstOrNull { it > index && rows[it].doneThisRound == null }
        if (next == null) close() else open(next)
    }
}
