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
import dev.dheirav.thirsttrap.domain.suggestReadingContext
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
    private val settings: dev.dheirav.thirsttrap.domain.SettingsRepository,
    ambient: dev.dheirav.thirsttrap.domain.AmbientRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val plantId: String = checkNotNull(savedStateHandle["id"])

    val state: StateFlow<WeightState?> =
        repository.observeWeightState(plantId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Whether the room accounts for a change in the drying rate.
     *
     * Sits beside the diagnostic rather than inside it, because it is a
     * different kind of claim: the diagnostic is about the plant, this is about
     * the weather. Null whenever there is nothing honest to say, which is most
     * of the time.
     */
    val ambientExplanation: StateFlow<dev.dheirav.thirsttrap.domain.AmbientExplanation?> =
        kotlinx.coroutines.flow.combine(
            repository.observeWeightState(plantId),
            ambient.observeAll(),
        ) { weight, readings ->
            dev.dheirav.thirsttrap.domain.explainForPlant(
                weight,
                readings,
                System.currentTimeMillis(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    /** Only ever moves the chip when the user has not already chosen one. */
    fun suggestContext(s: WeightState) {
        if (_context.value != ReadingContext.ROUTINE) return
        _context.value = suggestReadingContext(s, System.currentTimeMillis())
    }

    private val _dismissed = MutableStateFlow<Set<String>>(emptySet())
    val dismissed: StateFlow<Set<String>> = _dismissed.asStateFlow()

    init {
        viewModelScope.launch { _dismissed.value = settings.dismissedDiagnostics() }
    }

    /**
     * Keyed by the segment that raised it, so the alert stays quiet for this
     * drying cycle and can speak again on the next one.
     */
    fun diagnosticKey(s: WeightState): String? {
        val d = s.diagnostic ?: return null
        val start = s.currentSegment?.first?.timestampMillis ?: return null
        return "$plantId:$start:${d.name}"
    }

    fun dismissDiagnostic(key: String) {
        viewModelScope.launch {
            settings.dismissDiagnostic(key)
            _dismissed.value = _dismissed.value + key
        }
    }

    private val _hint = MutableStateFlow<String?>(null)
    val hint: StateFlow<String?> = _hint.asStateFlow()

    fun clearHint() { _hint.value = null }

    fun save(onDone: () -> Unit) {
        val grams = _entry.value.toDoubleOrNull() ?: return
        if (grams <= 0) return
        val saved = _context.value
        viewModelScope.launch {
            repository.addReading(plantId, grams, saved)
            rescheduleFromPrediction()
            _entry.value = ""

            // The documented flow is "weigh before watering, weigh after". The
            // chip used to stay on PRE_WATER for that second weigh, which meant
            // it was stored as another pre-water reading and the wet anchor was
            // never re-captured - so it silently went stale as the plant grew.
            // Advance it, and say so, rather than relying on the user noticing.
            if (saved == ReadingContext.PRE_WATER) {
                _context.value = ReadingContext.POST_WATER
                _hint.value = "Water it, then weigh again - I'll take that as the new full mark."
            } else {
                _context.value = ReadingContext.ROUTINE
            }
            onDone()
        }
    }

    fun calibrate(wetGrams: Double, trigger: Double = DEFAULT_DEPLETION_TRIGGER, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.calibrate(plantId, wetGrams, trigger)
            onDone()
        }
    }

    fun updateReading(reading: dev.dheirav.thirsttrap.domain.WeightReading) =
        viewModelScope.launch { repository.updateReading(reading) }

    fun deleteReading(id: String) = viewModelScope.launch { repository.deleteReading(id) }

    fun setExcluded(readingId: String, excluded: Boolean) {
        viewModelScope.launch {
            repository.setExcluded(readingId, excluded)
            rescheduleFromPrediction()
        }
    }

    /**
     * Feature F17.21, and the point of the whole app: once the model has an
     * opinion, the reminder follows the measured pot rather than a calendar.
     *
     * The gathering moved into the repository so that every other caller gets
     * the same treatment. This screen used to be the only place that passed a
     * real prediction, which meant the reminder tracked the pot only if you
     * happened to open it.
     */
    private suspend fun rescheduleFromPrediction() {
        if (reminders.observeForPlant(plantId).first().isEmpty()) return
        reminders.rescheduleFromModel(plantId, System.currentTimeMillis())
    }
}
