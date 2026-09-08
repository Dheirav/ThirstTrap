package dev.dheirav.thirsttrap.feature.timelapse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.Photo
import dev.dheirav.thirsttrap.domain.PhotoRepository
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.TimelapseFrame
import dev.dheirav.thirsttrap.domain.buildTimelapse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimelapseUiState(
    val plant: Plant? = null,
    val frames: List<TimelapseFrame> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class TimelapseViewModel @Inject constructor(
    plants: PlantRepository,
    private val photos: PhotoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val plantId: String = checkNotNull(savedStateHandle["id"])

    val state: StateFlow<TimelapseUiState> =
        combine(
            plants.observePlant(plantId),
            photos.observeForPlant(plantId),
        ) { plant, photoList ->
            TimelapseUiState(plant = plant, frames = buildTimelapse(photoList), loaded = true)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelapseUiState())

    private val _index = MutableStateFlow(0)
    val index: StateFlow<Int> = _index.asStateFlow()

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private var playJob: Job? = null

    fun scrubTo(i: Int) {
        stop()
        _index.value = i.coerceIn(0, (state.value.frames.size - 1).coerceAtLeast(0))
    }

    /**
     * Steps through the plates on a fixed interval.
     *
     * Deliberately not proportional to the real gaps between photos. A plant
     * diary has bursts - five photos in an afternoon when something looked
     * wrong - and months of nothing, so honouring real time would make the
     * interesting part flash past and the boring part last a minute.
     */
    fun togglePlay() {
        if (_playing.value) {
            stop()
            return
        }
        val frames = state.value.frames
        if (frames.size < 2) return
        // Starting from the end would look like a stall, so wrap first.
        if (_index.value >= frames.lastIndex) _index.value = 0
        _playing.value = true
        playJob = viewModelScope.launch {
            while (isActive && _playing.value) {
                delay(FRAME_MILLIS)
                val last = state.value.frames.lastIndex
                if (_index.value >= last) {
                    _playing.value = false
                    break
                }
                _index.value += 1
            }
        }
    }

    private fun stop() {
        _playing.value = false
        playJob?.cancel()
        playJob = null
    }

    fun pathOf(photo: Photo): String = photos.absolutePath(photo)

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    private companion object {
        const val FRAME_MILLIS = 600L
    }
}
