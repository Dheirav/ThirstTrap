package dev.dheirav.thirsttrap.feature.postmortem

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.Photo
import dev.dheirav.thirsttrap.domain.PhotoRepository
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.PlantStatus
import dev.dheirav.thirsttrap.domain.PostMortemRecap
import dev.dheirav.thirsttrap.domain.averageWateringIntervalDays
import dev.dheirav.thirsttrap.domain.buildPostMortemObservations
import dev.dheirav.thirsttrap.domain.newId
import dev.dheirav.thirsttrap.domain.tzOffsetMinutesAt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PostMortemUiState(
    val plant: Plant? = null,
    val recap: PostMortemRecap? = null,
    val photos: List<Photo> = emptyList(),
    val cause: String = "",
    val differently: String = "",
)

@HiltViewModel
class PostMortemViewModel @Inject constructor(
    private val plants: PlantRepository,
    private val photos: PhotoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val plantId: String = checkNotNull(savedStateHandle["id"])

    private val _input = MutableStateFlow("" to "")

    val state: StateFlow<PostMortemUiState> =
        combine(
            plants.observePlant(plantId),
            plants.observeEvents(plantId),
            photos.observeForPlant(plantId),
            _input,
        ) { plant, events, plantPhotos, input ->
            val now = System.currentTimeMillis()
            val waterings = events.filter { it.type == CareEventType.WATERED }
            val avg = averageWateringIntervalDays(waterings.map { it.timestampMillis })

            PostMortemUiState(
                plant = plant,
                photos = plantPhotos,
                cause = input.first,
                differently = input.second,
                recap = PostMortemRecap(
                    daysOwned = events.minByOrNull { it.timestampMillis }
                        ?.let { ((now - it.timestampMillis) / 86_400_000L).toInt() },
                    totalWaterings = waterings.size,
                    averageIntervalDays = avg,
                    lastWateredDaysAgo = waterings.maxByOrNull { it.timestampMillis }
                        ?.let { ((now - it.timestampMillis) / 86_400_000L).toInt() },
                    photoCount = plantPhotos.size,
                    // The events that change a plant's circumstances, which are
                    // the ones worth re-reading.
                    keyEvents = events.filter {
                        it.type in setOf(
                            CareEventType.REPOTTED, CareEventType.MEDIUM_CHANGED,
                            CareEventType.TREATED, CareEventType.PEST_OR_DISEASE,
                            CareEventType.PRUNED, CareEventType.MOVED,
                        )
                    }.sortedByDescending { it.timestampMillis },
                    observations = buildPostMortemObservations(events, avg, now),
                ),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PostMortemUiState())

    fun onCause(v: String) { _input.value = v to _input.value.second }
    fun onDifferently(v: String) { _input.value = _input.value.first to v }

    fun pathOf(photo: Photo): String = photos.absolutePath(photo)

    /**
     * Records the death and keeps everything. A dead plant's history is the
     * point of having written any of it down.
     */
    fun record(onDone: () -> Unit) {
        val s = state.value
        val plant = s.plant ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            plants.logEvent(
                CareEvent(
                    id = newId(),
                    plantId = plantId,
                    timestampMillis = now,
                    tzOffsetMinutes = tzOffsetMinutesAt(now),
                    type = CareEventType.DIED,
                    cause = s.cause.trim().takeIf { it.isNotEmpty() },
                    note = s.differently.trim().takeIf { it.isNotEmpty() }
                        ?.let { "Next time: $it" },
                ),
            )
            plants.setStatus(plantId, PlantStatus.DEAD)
            plants.archivePlant(plantId, archived = true)
            onDone()
        }
    }
}
