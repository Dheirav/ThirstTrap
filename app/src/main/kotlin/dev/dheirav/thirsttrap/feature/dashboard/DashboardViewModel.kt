package dev.dheirav.thirsttrap.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.CheckResult
import dev.dheirav.thirsttrap.domain.PlantAttention
import android.net.Uri
import dev.dheirav.thirsttrap.data.PhotoRepositoryImpl
import dev.dheirav.thirsttrap.domain.PhotoRepository
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.averageWateringIntervalDays
import dev.dheirav.thirsttrap.domain.newId
import dev.dheirav.thirsttrap.domain.tzOffsetMinutesAt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val items: List<PlantAttention> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: PlantRepository,
    private val photos: PhotoRepository,
    private val reminders: dev.dheirav.thirsttrap.domain.ReminderRepository,
) : ViewModel() {

    private val _photoError = MutableStateFlow<String?>(null)
    val photoError: StateFlow<String?> = _photoError.asStateFlow()

    fun addPhoto(plantId: String, uri: Uri) {
        viewModelScope.launch {
            val saved = (photos as? PhotoRepositoryImpl)?.importPhoto(plantId, uri)
            // A photo that silently fails to save is how two bugs stayed hidden
            // for a whole afternoon. Say so.
            if (saved == null) _photoError.value = "That photo could not be saved."
        }
    }

    fun clearPhotoError() { _photoError.value = null }

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    fun toggleArchived() { _showArchived.value = !_showArchived.value }

    /** Archived plants keep their history; they just need a way back to it. */
    val archived: StateFlow<List<dev.dheirav.thirsttrap.domain.Plant>> =
        repository.observePlants(includeArchived = true)
            .map { all -> all.filter { it.archived } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun unarchive(plantId: String) {
        viewModelScope.launch { repository.archivePlant(plantId, archived = false) }
    }

    val uiState: StateFlow<DashboardUiState> =
        repository.observeDashboard { System.currentTimeMillis() }
            .map { DashboardUiState(items = it, loaded = true) }
            .stateIn(
                scope = viewModelScope,
                // Keeps the flow alive across a rotation without leaking past
                // real backgrounding.
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DashboardUiState(),
            )

    /** The last logged event, so the undo snackbar can remove exactly it. */
    private val _lastLogged = MutableStateFlow<CareEvent?>(null)
    val lastLogged: StateFlow<CareEvent?> = _lastLogged.asStateFlow()

    fun logWatered(plantId: String, amountMl: Double?, onLogged: (CareEvent) -> Unit) =
        log(plantId, CareEventType.WATERED, null, amountMl, onLogged)

    /**
     * A check is not a watering. Recording it as one would make the card read
     * "watered today" for a plant you deliberately left alone, and would corrupt
     * the interval any reminder derives from the log.
     */
    fun logStillWet(plantId: String, onLogged: (CareEvent) -> Unit) =
        log(plantId, CareEventType.CHECKED, CheckResult.STILL_HEAVY, null, onLogged)

    private fun log(
        plantId: String,
        type: CareEventType,
        checkResult: CheckResult?,
        amountMl: Double?,
        onLogged: (CareEvent) -> Unit,
    ) {
        val now = System.currentTimeMillis()
        val event = CareEvent(
            id = newId(),
            plantId = plantId,
            timestampMillis = now,
            tzOffsetMinutes = tzOffsetMinutesAt(now),
            type = type,
            checkResult = checkResult,
            amountMl = amountMl,
        )
        viewModelScope.launch {
            repository.logEvent(event)
            // Watering and checking are the two most common actions in the app
            // and neither touched the reminder, so a plant you had just watered
            // could still be asked about on the old schedule. Both count as
            // assessments, which is why a check resets the clock too.
            reminders.rescheduleFromModel(plantId, now)
            _lastLogged.value = event
            onLogged(event)
        }
    }

    /** Undo is a real delete, not a tombstone - docs/UI-SPEC.md section 2. */
    fun undo(event: CareEvent) {
        viewModelScope.launch {
            repository.deleteEvent(event.id)
            // The reminder was pushed out on the strength of that event, so
            // undoing it has to put the schedule back too.
            reminders.rescheduleFromModel(event.plantId, System.currentTimeMillis())
        }
    }

    suspend fun averageIntervalDays(plantId: String): Double? = null // surfaced in step 5
}
