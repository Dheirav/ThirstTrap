package dev.dheirav.thirsttrap.feature.weighing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.ReadingContext
import dev.dheirav.thirsttrap.domain.WeightReading
import dev.dheirav.thirsttrap.domain.WeightRepository
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
) : ViewModel() {

    /** Session start, so "done" means done in this round rather than ever. */
    private val startedAtMillis = System.currentTimeMillis()

    val state: StateFlow<WeighingUiState> =
        combine(
            plants.observePlants(includeArchived = false),
            weights.observeAllReadings(),
        ) { plantList, readings ->
            val byPlant = readings.groupBy { it.plantId }
            WeighingUiState(
                rows = plantList
                    // A cutting in a jar of water weighs what the jar weighs.
                    .filter { it.isWeightTrackable }
                    .map { plant ->
                        val mine = byPlant[plant.id]?.sortedBy { r -> r.timestampMillis }.orEmpty()
                        WeighingRow(
                            plant = plant,
                            last = mine.lastOrNull { !it.excluded },
                            doneThisRound = mine.lastOrNull { it.timestampMillis >= startedAtMillis },
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
        _context.value = ReadingContext.ROUTINE
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
