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
import dev.dheirav.thirsttrap.domain.LightFit
import dev.dheirav.thirsttrap.domain.assessLightFor
import dev.dheirav.thirsttrap.domain.classifyLux
import dev.dheirav.thirsttrap.domain.lightFitFor
import dev.dheirav.thirsttrap.domain.newId
import dev.dheirav.thirsttrap.domain.tzOffsetMinutesAt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One plant that lives in the place being measured, and how the spot suits it. */
data class PlantHere(val name: String, val fit: LightFit?)

data class LightUiState(
    val hasSensor: Boolean = true,
    val lux: Float? = null,
    val level: LightLevel? = null,
    val verdict: String? = null,
    /** Populated only when measuring a place rather than a single plant. */
    val here: List<PlantHere> = emptyList(),
    val saved: Boolean = false,
)

@HiltViewModel
class LightMeterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val plants: PlantRepository,
    private val locations: dev.dheirav.thirsttrap.domain.LocationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), SensorEventListener {

    /**
     * One screen, two ways in. From a plant it measures where that pot sits;
     * from Places it measures the spot itself, which is how people actually
     * think about light. You go and hold the phone at the window because you
     * want to know about the window, not because a particular plant asked.
     */
    private val plantId: String? = savedStateHandle["id"]
    private val place: String? = savedStateHandle.get<String>("place")

    val plant: StateFlow<Plant?> =
        (plantId?.let { plants.observePlant(it) } ?: flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The place being measured: named directly, or wherever the plant lives. */
    val target: StateFlow<String?> =
        if (place != null) MutableStateFlow(place).asStateFlow()
        else plant.map { it?.location?.takeIf { l -> l.isNotBlank() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isPlaceMode: Boolean get() = place != null

    /**
     * Eagerly, not WhileSubscribed: nothing collects this flow. It is read
     * synchronously out of a sensor callback, so a lazily started one would sit
     * at its initial empty value forever and the screen would report that
     * nothing lives in a place that has plants in it.
     */
    private val residents: StateFlow<List<Plant>> =
        if (place == null) MutableStateFlow<List<Plant>>(emptyList()).asStateFlow()
        else plants.observePlants(includeArchived = false)
            .map { all -> all.filter { it.location?.trim().equals(place, ignoreCase = true) } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
            here = residents.value.map { PlantHere(it.name, lightFitFor(it.lightNeeds, level)) },
            saved = false,
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /**
     * A reading belongs to the place. It additionally belongs to a plant's
     * timeline when a plant is what you came from, because that is the record
     * of where that pot was standing at the time.
     */
    fun save() {
        val s = _state.value
        val lux = s.lux ?: return
        val level = s.level ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val p = plantId?.let { plants.observePlant(it).first() }
            val where = place ?: p?.location?.takeIf { it.isNotBlank() }

            if (p != null) {
                plants.logEvent(
                    CareEvent(
                        id = newId(),
                        plantId = p.id,
                        timestampMillis = now,
                        tzOffsetMinutes = tzOffsetMinutesAt(now),
                        type = CareEventType.OBSERVATION,
                        note = "Light here: ${lux.toInt()} lux - ${level.label.lowercase()}" +
                            (where?.let { " ($it)" } ?: ""),
                    ),
                )
            }
            // Requirement 13 wants light notes per location, and a measurement
            // that vanishes when the screen closes is no use when you are
            // deciding where to put the next pot.
            where?.let { locations.recordLight(it, lux, now) }
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
