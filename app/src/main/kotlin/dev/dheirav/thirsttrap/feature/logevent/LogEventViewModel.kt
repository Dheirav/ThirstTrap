package dev.dheirav.thirsttrap.feature.logevent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.CheckResult
import dev.dheirav.thirsttrap.domain.Medium
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.WateringMethod
import dev.dheirav.thirsttrap.domain.newId
import dev.dheirav.thirsttrap.domain.tzOffsetMinutesAt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogEventUiState(
    val plantName: String = "",
    val type: CareEventType = CareEventType.WATERED,
    val note: String = "",
    val amountMl: String = "",
    val method: WateringMethod? = null,
    val checkResult: CheckResult? = null,
    val fertilizerName: String = "",
    val dilution: String = "",
    val toMedium: Medium? = null,
    val cause: String = "",
) {
    /** Which extra fields this type actually needs — docs/DATA-MODEL.md. */
    val showAmount get() = type == CareEventType.WATERED
    val showCheckResult get() = type == CareEventType.CHECKED
    val showFertilizer get() = type == CareEventType.FERTILIZED
    val showMedium get() = type == CareEventType.MEDIUM_CHANGED
    val showCause get() = type == CareEventType.DIED
}

@HiltViewModel
class LogEventViewModel @Inject constructor(
    private val repository: PlantRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val plantId: String = checkNotNull(savedStateHandle["id"])

    private val _state = MutableStateFlow(LogEventUiState())
    val state: StateFlow<LogEventUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val plant = repository.observePlant(plantId).first() ?: return@launch
            // Pre-fill from the plant's standard, falling back to the last
            // amount actually poured. Retyping the same number every time is
            // exactly the friction that sends people back to paper.
            val lastAmount = repository.observeEvents(plantId).first()
                .firstOrNull { it.type == CareEventType.WATERED && it.amountMl != null }?.amountMl
            val suggested = plant.defaultWaterMl ?: lastAmount
            _state.value = _state.value.copy(
                plantName = plant.name,
                amountMl = suggested?.toInt()?.toString().orEmpty(),
            )
        }
    }

    fun onType(v: CareEventType) { _state.value = _state.value.copy(type = v) }
    fun onNote(v: String) { _state.value = _state.value.copy(note = v) }
    fun onAmount(v: String) { _state.value = _state.value.copy(amountMl = v.filter { it.isDigit() }) }
    fun onMethod(v: WateringMethod) { _state.value = _state.value.copy(method = v) }
    fun onCheckResult(v: CheckResult) { _state.value = _state.value.copy(checkResult = v) }
    fun onFertilizer(v: String) { _state.value = _state.value.copy(fertilizerName = v) }
    fun onDilution(v: String) { _state.value = _state.value.copy(dilution = v) }
    fun onToMedium(v: Medium) { _state.value = _state.value.copy(toMedium = v) }
    fun onCause(v: String) { _state.value = _state.value.copy(cause = v) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val plant = repository.observePlant(plantId).first()

            repository.logEvent(
                CareEvent(
                    id = newId(),
                    plantId = plantId,
                    timestampMillis = now,
                    tzOffsetMinutes = tzOffsetMinutesAt(now),
                    type = s.type,
                    note = s.note.trim().takeIf { it.isNotEmpty() },
                    amountMl = s.amountMl.toDoubleOrNull(),
                    method = s.method,
                    checkResult = s.checkResult,
                    fertilizerName = s.fertilizerName.trim().takeIf { it.isNotEmpty() },
                    dilution = s.dilution.trim().takeIf { it.isNotEmpty() },
                    fromMedium = if (s.type == CareEventType.MEDIUM_CHANGED) plant?.medium else null,
                    toMedium = s.toMedium,
                    cause = s.cause.trim().takeIf { it.isNotEmpty() },
                ),
            )

            // A medium change and a repot both invalidate the weight anchors:
            // the pot itself changed weight, so every previous reading is now
            // meaningless. docs/WATERING-MODEL.md section 2.
            if (plant != null) {
                when (s.type) {
                    CareEventType.MEDIUM_CHANGED -> repository.upsertPlant(
                        plant.copy(
                            medium = s.toMedium ?: plant.medium,
                            anchors = null,
                            needsRecalibration = true,
                        ),
                    )
                    CareEventType.REPOTTED -> repository.upsertPlant(
                        plant.copy(anchors = null, needsRecalibration = true),
                    )
                    else -> Unit
                }
            }
            onDone()
        }
    }
}
