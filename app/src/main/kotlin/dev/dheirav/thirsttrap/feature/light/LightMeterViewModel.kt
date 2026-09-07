package dev.dheirav.thirsttrap.feature.light

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.LightLevel
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.assessLightFor
import dev.dheirav.thirsttrap.domain.classifyLux
import dev.dheirav.thirsttrap.domain.newId
import dev.dheirav.thirsttrap.domain.tzOffsetMinutesAt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LightUiState(
    val hasSensor: Boolean = true,
    val lux: Float? = null,
    val level: LightLevel? = null,
    val verdict: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class LightMeterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val plants: PlantRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), SensorEventListener {

    private val plantId: String = checkNotNull(savedStateHandle["id"])

    val plant: StateFlow<Plant?> =
        plants.observePlant(plantId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _state = MutableStateFlow(LightUiState())
    val state: StateFlow<LightUiState> = _state.asStateFlow()

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)

    /**
     * A rolling mean over recent samples. Raw lux jitters constantly - a hand
     * moving, a cloud - and a number that never settles is one nobody can read.
     */
    private val window = ArrayDeque<Float>()

    init {
        if (sensor == null) _state.value = LightUiState(hasSensor = false)
    }

    fun start() {
        val s = sensor ?: return
        sensorManager?.registerListener(this, s, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val raw = event.values.firstOrNull() ?: return
        window.addLast(raw)
        while (window.size > SMOOTHING_SAMPLES) window.removeFirst()
        val mean = window.average().toFloat()

        val level = classifyLux(mean)
        _state.value = _state.value.copy(
            lux = mean,
            level = level,
            verdict = assessLightFor(plant.value?.lightNeeds, level),
            saved = false,
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** Records the measurement as an observation, so it lands in the timeline. */
    fun save() {
        val s = _state.value
        val lux = s.lux ?: return
        val level = s.level ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val p = plants.observePlant(plantId).first()
            plants.logEvent(
                CareEvent(
                    id = newId(),
                    plantId = plantId,
                    timestampMillis = now,
                    tzOffsetMinutes = tzOffsetMinutesAt(now),
                    type = CareEventType.OBSERVATION,
                    note = "Light here: ${lux.toInt()} lux - ${level.label.lowercase()}" +
                        (p?.location?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""),
                ),
            )
            _state.value = _state.value.copy(saved = true)
        }
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    private companion object {
        const val SMOOTHING_SAMPLES = 12
    }
}
