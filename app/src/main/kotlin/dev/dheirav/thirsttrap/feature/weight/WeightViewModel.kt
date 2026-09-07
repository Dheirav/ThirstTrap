package dev.dheirav.thirsttrap.feature.weight

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.DEFAULT_DEPLETION_TRIGGER
import dev.dheirav.thirsttrap.domain.Prediction
import dev.dheirav.thirsttrap.domain.ReadingContext
import dev.dheirav.thirsttrap.domain.ReminderRepository
import dev.dheirav.thirsttrap.domain.WeightRepository
import dev.dheirav.thirsttrap.domain.computeNextDue
import dev.dheirav.thirsttrap.domain.resolveIntervalDays
import kotlinx.coroutines.flow.first
import dev.dheirav.thirsttrap.domain.WeightState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val repository: WeightRepository,
    private val reminders: ReminderRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val plantId: String = checkNotNull(savedStateHandle["id"])

    val state: StateFlow<WeightState?> =
        repository.observeWeightState(plantId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _entry = MutableStateFlow("")
    val entry: StateFlow<String> = _entry.asStateFlow()

    private val _context = MutableStateFlow(ReadingContext.ROUTINE)
    val context: StateFlow<ReadingContext> = _context.asStateFlow()

    fun onDigit(d: Char) {
        if (_entry.value.length < 6) _entry.value += d
    }

    fun onBackspace() {
        _entry.value = _entry.value.dropLast(1)
    }

    fun onContext(c: ReadingContext) { _context.value = c }

    /**
     * Defaults sensibly: a plant already past its trigger is almost certainly
     * being weighed just before watering it.
     */
    fun suggestContext(s: WeightState) {
        _context.value = if (s.prediction is Prediction.WaterNow) {
            ReadingContext.PRE_WATER
        } else {
            ReadingContext.ROUTINE
        }
    }

    fun save(onDone: () -> Unit) {
        val grams = _entry.value.toDoubleOrNull() ?: return
        if (grams <= 0) return
        viewModelScope.launch {
            repository.addReading(plantId, grams, _context.value)
            rescheduleFromPrediction()
            _entry.value = ""
            onDone()
        }
    }

    fun calibrate(wetGrams: Double, trigger: Double = DEFAULT_DEPLETION_TRIGGER, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.calibrate(plantId, wetGrams, trigger)
            onDone()
        }
    }

    fun setExcluded(readingId: String, excluded: Boolean) {
        viewModelScope.launch {
            repository.setExcluded(readingId, excluded)
            rescheduleFromPrediction()
        }
    }

    /**
     * Feature F17.21, and the point of the whole app: once the model has an
     * opinion, the reminder follows the measured pot rather than a calendar.
     * A capped or suppressed prediction is not trusted - resolveIntervalDays
     * falls back to the logged average, then to a week.
     */
    private suspend fun rescheduleFromPrediction() {
        val s = repository.observeWeightState(plantId).first()
        val existing = reminders.observeForPlant(plantId).first().firstOrNull() ?: return
        val interval = resolveIntervalDays(
            explicitIntervalDays = existing.intervalDays,
            prediction = s.prediction,
            loggedAverageDays = null,
        )
        val now = System.currentTimeMillis()
        reminders.reschedule(plantId, computeNextDue(now, interval, now))
    }
}
