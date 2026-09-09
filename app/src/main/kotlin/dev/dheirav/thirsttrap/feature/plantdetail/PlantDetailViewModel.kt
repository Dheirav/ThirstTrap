package dev.dheirav.thirsttrap.feature.plantdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.Plant
import android.net.Uri
import dev.dheirav.thirsttrap.data.PhotoRepositoryImpl
import dev.dheirav.thirsttrap.domain.Photo
import dev.dheirav.thirsttrap.domain.PhotoRepository
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.averageWateringIntervalDays
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One day's worth of events, for a sticky header. */
data class TimelineDay(val label: String, val events: List<CareEvent>)

data class PlantDetailUiState(
    val plant: Plant? = null,
    val days: List<TimelineDay> = emptyList(),
    val totalEvents: Int = 0,
    val photos: List<Photo> = emptyList(),
    /** Keyed by care event id, so a timeline row can show its own photos. */
    val photosByEvent: Map<String, List<Photo>> = emptyMap(),
    val averageIntervalDays: Double? = null,
    val loaded: Boolean = false,
)

@HiltViewModel
class PlantDetailViewModel @Inject constructor(
    private val repository: PlantRepository,
    private val photos: PhotoRepository,
    private val reminders: dev.dheirav.thirsttrap.domain.ReminderRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val plantId: String = checkNotNull(savedStateHandle["id"])

    val state: StateFlow<PlantDetailUiState> =
        combine(
            repository.observePlant(plantId),
            repository.observeEvents(plantId),
            photos.observeForPlant(plantId),
        ) { plant, events, plantPhotos ->
            PlantDetailUiState(
                plant = plant,
                days = groupByLocalDay(events),
                totalEvents = events.size,
                photos = plantPhotos,
                photosByEvent = plantPhotos.groupBy { it.careEventId.orEmpty() }
                    .filterKeys { it.isNotEmpty() },
                averageIntervalDays = averageWateringIntervalDays(
                    events.filter { it.type == CareEventType.WATERED }.map { it.timestampMillis },
                ),
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlantDetailUiState())

    fun deleteEvent(event: CareEvent) {
        viewModelScope.launch {
            repository.deleteEvent(event.id)
            // Deleting the watering the schedule was counting from has to move
            // the schedule, or the plant stays due on the strength of an event
            // that no longer exists.
            reminders.rescheduleFromModel(event.plantId, System.currentTimeMillis())
        }
    }

    fun addPhoto(uri: Uri) {
        viewModelScope.launch {
            (photos as? PhotoRepositoryImpl)?.importPhoto(plantId, uri)
        }
    }

    fun deletePhoto(photoId: String) {
        viewModelScope.launch { photos.delete(photoId) }
    }

    fun setCaption(photoId: String, caption: String) {
        viewModelScope.launch { photos.setCaption(photoId, caption) }
    }

    fun pathOf(photo: Photo): String = photos.absolutePath(photo)

    /**
     * An explicit cover, rather than "whatever is newest". A plant's best
     * photo is not always its most recent one.
     */
    fun setCover(photoId: String) {
        viewModelScope.launch {
            repository.observePlant(plantId).first()?.let {
                repository.upsertPlant(it.copy(coverPhotoId = photoId))
            }
        }
    }

    /**
     * Grouped by the **local** civil day the event happened on, reconstructed
     * from its stored offset rather than the device's current zone. Water at
     * 23:00 IST, read it back in another timezone, and a naive conversion moves
     * it to the previous day - which is exactly the off-by-one the dual
     * timestamp columns exist to prevent.
     */
    private fun groupByLocalDay(events: List<CareEvent>): List<TimelineDay> {
        val now = System.currentTimeMillis()
        val todayIndex = localDayIndex(now, offsetMinutesNow())

        return events
            .groupBy { localDayIndex(it.timestampMillis, it.tzOffsetMinutes) }
            .toSortedMap(compareByDescending { it })
            .map { (dayIndex, dayEvents) ->
                TimelineDay(
                    label = when (todayIndex - dayIndex) {
                        0L -> "Today"
                        1L -> "Yesterday"
                        in 2L..6L -> "${todayIndex - dayIndex} days ago"
                        else -> formatDate(dayEvents.first())
                    },
                    events = dayEvents.sortedByDescending { it.timestampMillis },
                )
            }
    }

    private fun offsetMinutesNow(): Int =
        java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000

    private fun localDayIndex(utcMillis: Long, offsetMinutes: Int): Long =
        Math.floorDiv(utcMillis + offsetMinutes * 60_000L, 86_400_000L)

    private fun formatDate(event: CareEvent): String {
        val zone = java.time.ZoneOffset.ofTotalSeconds(event.tzOffsetMinutes * 60)
        val date = java.time.Instant.ofEpochMilli(event.timestampMillis).atZone(zone).toLocalDate()
        return date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
    }
}
