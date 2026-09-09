package dev.dheirav.thirsttrap.feature.plantedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.Medium
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.PlantSource
import dev.dheirav.thirsttrap.domain.PlantStatus
import dev.dheirav.thirsttrap.domain.Reminder
import dev.dheirav.thirsttrap.domain.ReminderKind
import dev.dheirav.thirsttrap.domain.ReminderRepository
import dev.dheirav.thirsttrap.domain.computeNextDue
import dev.dheirav.thirsttrap.domain.newId
import dev.dheirav.thirsttrap.domain.resolveIntervalDays
import dev.dheirav.thirsttrap.domain.tzOffsetMinutesAt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * When did you last water it?
 *
 * A plant you add today already has a history - you did not acquire it this
 * morning. Without this the app records "never watered", which sorts the plant
 * wrongly on the dashboard and starts its first reminder from the wrong day.
 * Answering it writes a backdated watering event, so the log stays the single
 * source of truth rather than adding a column.
 */
enum class LastWatered(val label: String, val daysAgo: Int?) {
    TODAY("Today", 0),
    YESTERDAY("Yesterday", 1),
    THREE_DAYS("3 days ago", 3),
    A_WEEK("A week ago", 7),
    UNKNOWN("Not sure", null),
}

data class PlantEditUiState(
    val id: String? = null,
    val name: String = "",
    val species: String = "",
    val location: String = "",
    val containerDesc: String = "",
    val defaultWaterMl: String = "",
    val checkIntervalDays: String = "",
    val targetDryness: String = "",
    val lightNeeds: String = "",
    val fertilizerCadenceDays: String = "",
    val medium: Medium = Medium.SOIL,
    val source: PlantSource = PlantSource.UNKNOWN,
    val status: PlantStatus = PlantStatus.ACTIVE,
    val archived: Boolean = false,
    val lastWatered: LastWatered = LastWatered.UNKNOWN,
    val loading: Boolean = true,
) {
    val isNew: Boolean get() = id == null
    val canSave: Boolean get() = name.isNotBlank()
}

@HiltViewModel
class PlantEditViewModel @Inject constructor(
    private val repository: PlantRepository,
    private val reminders: ReminderRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val plantId: String? = savedStateHandle.get<String>("id")?.takeIf { it.isNotBlank() }

    private val _state = MutableStateFlow(PlantEditUiState(loading = plantId != null))
    val state: StateFlow<PlantEditUiState> = _state.asStateFlow()

    init {
        plantId?.let { id ->
            viewModelScope.launch {
                val existing = reminders.observeForPlant(id).first().firstOrNull()
                repository.observePlant(id).first()?.let { p ->
                    _state.value = PlantEditUiState(
                        id = p.id,
                        name = p.name,
                        species = p.species.orEmpty(),
                        location = p.location.orEmpty(),
                        containerDesc = p.containerDesc.orEmpty(),
                        defaultWaterMl = p.defaultWaterMl?.toInt()?.toString().orEmpty(),
                        targetDryness = p.targetDryness.orEmpty(),
                        lightNeeds = p.lightNeeds.orEmpty(),
                        fertilizerCadenceDays = p.fertilizerCadenceDays?.toString().orEmpty(),
                        medium = p.medium,
                        source = p.source,
                        status = p.status,
                        archived = p.archived,
                        checkIntervalDays = existing?.intervalDays?.toString().orEmpty(),
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
    fun onLastWatered(v: LastWatered) { _state.value = _state.value.copy(lastWatered = v) }
    fun onTargetDryness(v: String) { _state.value = _state.value.copy(targetDryness = v) }
    fun onLightNeeds(v: String) { _state.value = _state.value.copy(lightNeeds = v) }
    fun onFertilizerCadence(v: String) {
        _state.value = _state.value.copy(fertilizerCadenceDays = v.filter { it.isDigit() }.take(3))
    }
    fun onCheckInterval(v: String) {
        _state.value = _state.value.copy(checkIntervalDays = v.filter { it.isDigit() }.take(3))
    }
    fun onDefaultWater(v: String) {
        _state.value = _state.value.copy(defaultWaterMl = v.filter { it.isDigit() }.take(5))
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        val plantId = s.id ?: newId()
        val isNew = s.isNew
        viewModelScope.launch {
            repository.upsertPlant(
                Plant(
                    id = plantId,
                    name = s.name.trim(),
                    species = s.species.trim().takeIf { it.isNotEmpty() },
                    medium = s.medium,
                    location = s.location.trim().takeIf { it.isNotEmpty() },
                    status = s.status,
                    containerDesc = s.containerDesc.trim().takeIf { it.isNotEmpty() },
                    defaultWaterMl = s.defaultWaterMl.toDoubleOrNull(),
                    targetDryness = s.targetDryness.trim().takeIf { it.isNotEmpty() },
                    lightNeeds = s.lightNeeds.trim().takeIf { it.isNotEmpty() },
                    fertilizerCadenceDays = s.fertilizerCadenceDays.toIntOrNull(),
                    source = s.source,
                    archived = s.archived,
                ),
            )

            // An explicit cadence, if the user gave one. Null means "work it
            // out from the log", which is what resolveIntervalDays does.
            if (!isNew) {
                reminders.observeForPlant(plantId).first().firstOrNull()?.let { r ->
                    reminders.upsert(r.copy(intervalDays = s.checkIntervalDays.toIntOrNull()))
                }
            }

            if (isNew) {
                // Seed the log with what the user told us, so the plant does not
                // read as "never watered" and the first reminder counts from the
                // right day.
                val now = System.currentTimeMillis()
                val daysAgo = s.lastWatered.daysAgo
                val lastAssessed = daysAgo?.let { now - it * 86_400_000L }

                if (lastAssessed != null) {
                    repository.logEvent(
                        CareEvent(
                            id = newId(),
                            plantId = plantId,
                            timestampMillis = lastAssessed,
                            tzOffsetMinutes = tzOffsetMinutesAt(lastAssessed),
                            type = CareEventType.WATERED,
                            note = "Recorded when the plant was added",
                        ),
                    )
                }

                // Every new plant gets a check reminder. Without one it is
                // invisible until the user happens to open the app.
                //
                // Created with the default, then immediately replanned: the row
                // has to exist before rescheduleFromModel can update it, and a
                // plant being edited rather than created may already have a
                // history worth using.
                reminders.upsert(
                    Reminder(
                        id = newId(),
                        plantId = plantId,
                        kind = ReminderKind.CHECK,
                        intervalDays = s.checkIntervalDays.toIntOrNull(),
                        nextDueAtMillis = computeNextDue(
                            lastAssessed,
                            resolveIntervalDays(null, null, null),
                            now,
                        ),
                    ),
                )
                reminders.rescheduleFromModel(plantId, now)
            }
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
