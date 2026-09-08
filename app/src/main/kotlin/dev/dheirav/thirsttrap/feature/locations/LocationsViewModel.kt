package dev.dheirav.thirsttrap.feature.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.LocationNote
import dev.dheirav.thirsttrap.domain.LocationRepository
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.knownLocations
import dev.dheirav.thirsttrap.domain.plantsPerLocation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocationRow(
    val name: String,
    val note: LocationNote?,
    val plantCount: Int,
)

data class LocationsUiState(
    val rows: List<LocationRow> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class LocationsViewModel @Inject constructor(
    private val locations: LocationRepository,
    plants: PlantRepository,
) : ViewModel() {

    val state: StateFlow<LocationsUiState> =
        combine(
            plants.observePlants(includeArchived = false),
            locations.observeAll(),
        ) { plantList, notes ->
            val counts = plantsPerLocation(plantList)
            val byKey = notes.associateBy { it.name.lowercase() }
            LocationsUiState(
                rows = knownLocations(plantList, notes).map { name ->
                    LocationRow(
                        name = name,
                        note = byKey[name.lowercase()],
                        plantCount = counts[name.lowercase()] ?: 0,
                    )
                },
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LocationsUiState())

    fun setNote(name: String, note: String?) =
        viewModelScope.launch { locations.setNote(name, note) }
}
