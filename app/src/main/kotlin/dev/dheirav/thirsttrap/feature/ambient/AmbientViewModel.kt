package dev.dheirav.thirsttrap.feature.ambient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.AmbientReading
import dev.dheirav.thirsttrap.domain.AmbientRepository
import dev.dheirav.thirsttrap.domain.AmbientSource
import dev.dheirav.thirsttrap.domain.PlantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

data class AmbientUiState(
    val readings: List<AmbientReading> = emptyList(),
    /** Locations already in use by plants, so nobody has to retype "windowsill". */
    val knownLocations: List<String> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class AmbientViewModel @Inject constructor(
    private val ambient: AmbientRepository,
    plants: PlantRepository,
) : ViewModel() {

    val state: StateFlow<AmbientUiState> =
        combine(
            ambient.observeAll(),
            plants.observePlants(includeArchived = false),
        ) { readings, plantList ->
            // Locations come from the plants, not from past ambient readings:
            // the useful list is the places that have something growing in
            // them, including ones never measured before.
            val locations = (
                plantList.mapNotNull { it.location?.takeIf { l -> l.isNotBlank() } } +
                    readings.map { it.location }
                )
                .distinctBy { it.lowercase() }
                .sorted()
            AmbientUiState(readings = readings, knownLocations = locations, loaded = true)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AmbientUiState())

    private val _saved = MutableStateFlow(0)
    val saved: StateFlow<Int> = _saved.asStateFlow()

    /**
     * Records a reading. Either field may be null - plenty of people own a
     * hygrometer and no thermometer, or the reverse - but not both.
     */
    fun record(location: String, temperatureC: Double?, humidityPercent: Double?, note: String?) {
        if (location.isBlank()) return
        if (temperatureC == null && humidityPercent == null) return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            ambient.record(
                AmbientReading(
                    id = UUID.randomUUID().toString(),
                    location = location.trim(),
                    timestampMillis = now,
                    tzOffsetMinutes = TimeZone.getDefault().getOffset(now) / 60_000,
                    temperatureC = temperatureC,
                    humidityPercent = humidityPercent,
                    source = AmbientSource.MANUAL,
                    note = note?.takeIf { it.isNotBlank() },
                ),
            )
            _saved.value += 1
        }
    }

    fun delete(id: String) = viewModelScope.launch { ambient.delete(id) }
}
