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
import dev.dheirav.thirsttrap.domain.ReminderRepository
import dev.dheirav.thirsttrap.domain.WateringMethod
import dev.dheirav.thirsttrap.domain.WeightRepository
import dev.dheirav.thirsttrap.domain.WhenLogged
import dev.dheirav.thirsttrap.domain.newId
import dev.dheirav.thirsttrap.domain.resolveLoggedAt
import dev.dheirav.thirsttrap.domain.tzOffsetMinutesAt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
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
    val whenLogged: WhenLogged = WhenLogged.NOW,
    /** Set only when whenLogged is PICK. */
    val pickedDateMillis: Long? = null,
    /** Whether this plant currently has weight anchors worth losing. */
    val plantIsCalibrated: Boolean = false,
) {
    /**
     * Backdated entries land at midday rather than midnight, so a watering
     * recorded for "yesterday" cannot sort before one genuinely logged early
     * that morning, and cannot drift across a day boundary by timezone.
     */
    fun timestamp(nowMillis: Long): Long =
        resolveLoggedAt(whenLogged, nowMillis, tzOffsetMinutesAt(nowMillis), pickedDateMillis)

    val isBackdated: Boolean get() = whenLogged != WhenLogged.NOW

    /** Which extra fields this type actually needs — docs/DATA-MODEL.md. */
    val showAmount get() = type == CareEventType.WATERED
    val showCheckResult get() = type == CareEventType.CHECKED
    val showFertilizer get() = type == CareEventType.FERTILIZED
    val showMedium get() = type == CareEventType.MEDIUM_CHANGED
    val showCause get() = type == CareEventType.DIED

    /**
     * A repot or a medium change throws the weight setup away, because the pot
     * itself now weighs something different and every stored reading is against
     * the old one. That was happening silently: the app cleared the anchors on
     * save and the user found out days later, when the plant's weight screen
     * had gone back to asking to be set up. Say it while there is still a
     * chance to have meant something else.
     */
    val clearsWeightSetup: Boolean
        get() = plantIsCalibrated &&
            (type == CareEventType.REPOTTED || type == CareEventType.MEDIUM_CHANGED)
}



@HiltViewModel
class LogEventViewModel @Inject constructor(
    private val repository: PlantRepository,
    private val reminders: ReminderRepository,
    private val weights: WeightRepository,
    fertilizers: dev.dheirav.thirsttrap.domain.FertilizerRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val plantId: String = checkNotNull(savedStateHandle["id"])

    private val _state = MutableStateFlow(LogEventUiState())
    val state: StateFlow<LogEventUiState> = _state.asStateFlow()

    /**
     * The cupboard, so a feed is picked rather than retyped. An inventory you
     * have to copy out by hand at the moment of use is just a second place to
     * keep the same string.
     */
    val cupboard: StateFlow<List<dev.dheirav.thirsttrap.domain.Fertilizer>> =
        fertilizers.observeAll().stateIn(
            viewModelScope,
            kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    /** Picking a bottle fills both fields; either can still be edited after. */
    fun onPickFertilizer(f: dev.dheirav.thirsttrap.domain.Fertilizer) {
        _state.value = _state.value.copy(
            fertilizerName = f.name,
            dilution = f.dilutionText.orEmpty(),
        )
    }

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
                // Asked of the assembled state, not of the stored anchors.
                // Since D21 a pot is calibrated when its readings contain a
                // post-water weigh, and the stored column stays null for every
                // plant that got there that way - which is all of them.
                plantIsCalibrated = weights.observeWeightState(plantId).first().isCalibrated,
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
    fun onWhen(v: WhenLogged) { _state.value = _state.value.copy(whenLogged = v) }
    fun onPickedDate(millis: Long?) {
        _state.value = _state.value.copy(
            pickedDateMillis = millis,
            whenLogged = if (millis != null) WhenLogged.PICK else _state.value.whenLogged,
        )
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        val now = System.currentTimeMillis()
        val at = s.timestamp(now)
        viewModelScope.launch {
            val plant = repository.observePlant(plantId).first()

            repository.logEvent(
                CareEvent(
                    id = newId(),
                    plantId = plantId,
                    timestampMillis = at,
                    tzOffsetMinutes = tzOffsetMinutesAt(at),
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
            // Everything logged here can move the schedule: a watering or a
            // check resets the clock, and a repot throws the anchors away so
            // the prediction that was driving the interval no longer exists.
            reminders.rescheduleFromModel(plantId, now)
            onDone()
        }
    }
}
