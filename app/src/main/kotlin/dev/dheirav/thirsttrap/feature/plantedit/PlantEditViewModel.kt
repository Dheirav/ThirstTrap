package dev.dheirav.thirsttrap.feature.plantedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.Medium
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.PlantSource
import dev.dheirav.thirsttrap.domain.PlantStatus
import dev.dheirav.thirsttrap.domain.newId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlantEditUiState(
    val id: String? = null,
    val name: String = "",
    val species: String = "",
    val location: String = "",
    val containerDesc: String = "",
    val medium: Medium = Medium.SOIL,
    val source: PlantSource = PlantSource.UNKNOWN,
    val status: PlantStatus = PlantStatus.ACTIVE,
    val archived: Boolean = false,
    val loading: Boolean = true,
) {
    val isNew: Boolean get() = id == null
    val canSave: Boolean get() = name.isNotBlank()
}

@HiltViewModel
class PlantEditViewModel @Inject constructor(
    private val repository: PlantRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val plantId: String? = savedStateHandle.get<String>("id")?.takeIf { it.isNotBlank() }

    private val _state = MutableStateFlow(PlantEditUiState(loading = plantId != null))
    val state: StateFlow<PlantEditUiState> = _state.asStateFlow()

    init {
        plantId?.let { id ->
            viewModelScope.launch {
                repository.observePlant(id).first()?.let { p ->
                    _state.value = PlantEditUiState(
                        id = p.id,
                        name = p.name,
                        species = p.species.orEmpty(),
                        location = p.location.orEmpty(),
                        containerDesc = p.containerDesc.orEmpty(),
                        medium = p.medium,
                        source = p.source,
                        status = p.status,
                        archived = p.archived,
                        loading = false,
                    )
                }
            }
        }
    }

    fun onName(v: String) { _state.value = _state.value.copy(name = v) }
    fun onSpecies(v: String) { _state.value = _state.value.copy(species = v) }
    fun onLocation(v: String) { _state.value = _state.value.copy(location = v) }
    fun onContainer(v: String) { _state.value = _state.value.copy(containerDesc = v) }
    fun onMedium(v: Medium) { _state.value = _state.value.copy(medium = v) }
    fun onSource(v: PlantSource) { _state.value = _state.value.copy(source = v) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            repository.upsertPlant(
                Plant(
                    id = s.id ?: newId(),
                    name = s.name.trim(),
                    species = s.species.trim().takeIf { it.isNotEmpty() },
                    medium = s.medium,
                    location = s.location.trim().takeIf { it.isNotEmpty() },
                    status = s.status,
                    containerDesc = s.containerDesc.trim().takeIf { it.isNotEmpty() },
                    source = s.source,
                    archived = s.archived,
                ),
            )
            onDone()
        }
    }

    /** Archiving keeps the history - post-mortems are half the value. */
    fun archive(onDone: () -> Unit) {
        val id = _state.value.id ?: return
        viewModelScope.launch {
            repository.archivePlant(id, archived = true)
            onDone()
        }
    }

    /** Hard delete. Cascades to every event; the UI confirms before calling this. */
    fun delete(onDone: () -> Unit) {
        val id = _state.value.id ?: return
        viewModelScope.launch {
            repository.deletePlant(id)
            onDone()
        }
    }
}
