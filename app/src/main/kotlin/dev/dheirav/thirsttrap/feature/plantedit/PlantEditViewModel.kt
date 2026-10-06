package dev.dheirav.thirsttrap.feature.plantedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.DEFAULT_DEPLETION_TRIGGER_PCT
import dev.dheirav.thirsttrap.domain.carePrefill
import dev.dheirav.thirsttrap.domain.findSpeciesCare
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

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

/**
 * A pot this plant could be put in, offered as the plant already in it.
 *
 * There is no containers table, so a container has no name of its own and is
 * identified by who is already standing in it. "Share a pot with the Fittonia"
 * is also how someone would say it out loud, which is the better reason.
 */
data class ShareOption(
    val plantId: String,
    val plantName: String,
    /** The container that plant is already in, or null if it is in its own. */
    val containerId: String?,
    val weightTracked: Boolean,
)

data class PlantEditUiState(
    val id: String? = null,
    val name: String = "",
    val species: String = "",
    val location: String = "",
    val containerDesc: String = "",
    /** Null means this plant is in its own pot, which is the ordinary case. */
    val containerId: String? = null,
    /**
     * Whether the pot list is showing. Separate from containerId because the
     * switch goes on before a pot has been chosen, and a plant in its own pot
     * is the overwhelmingly common case: offering every other plant on the
     * shelf as a permanent row of chips puts the rare answer in front of the
     * ordinary one, and grows with the collection.
     */
    val sharingOn: Boolean = false,
    val shareOptions: List<ShareOption> = emptyList(),
    val defaultWaterMl: String = "",
    val checkIntervalDays: String = "",
    val targetDryness: String = "",
    val lightNeeds: String = "",
    val fertilizerCadenceDays: String = "",
    val medium: Medium = Medium.SOIL,
    val source: PlantSource = PlantSource.UNKNOWN,
    val status: PlantStatus = PlantStatus.ACTIVE,
    /**
     * Whole percent, because a slider over 0.05 steps of a Double invites
     * 0.6499999 into the database. Converted at the edges.
     */
    // The same constant the prefill tests against. Two copies of this number
    // would drift, and the failure is silent: the trigger simply stops being
    // prefilled, with nothing to notice.
    val depletionTriggerPct: Int = DEFAULT_DEPLETION_TRIGGER_PCT,
    val weightTracked: Boolean = true,
    /**
     * Set when the chosen neighbour had no container of its own, so saving has
     * to put that plant into the new one as well. Null when joining a pot that
     * already exists.
     */
    val mintedWith: String? = null,
    val weighingMethod: dev.dheirav.thirsttrap.domain.WeighingMethod =
        dev.dheirav.thirsttrap.domain.WeighingMethod.WHOLE_POT,
    val weighingStep: String = "1",
    /** What the plant had when the form opened, to spot a real change on save. */
    /**
     * The species whose notes filled part of this form, and which fields they
     * filled. Held so the form can say so, because a value that appears in a
     * field the user did not type is otherwise indistinguishable from one they
     * typed and forgot.
     */
    val prefillFrom: String? = null,
    val prefilled: List<String> = emptyList(),
    val originalWeighingMethod: dev.dheirav.thirsttrap.domain.WeighingMethod? = null,
    val originalWeighingStep: Double? = null,
    val archived: Boolean = false,
    val lastWatered: LastWatered = LastWatered.UNKNOWN,
    val loading: Boolean = true,
) {
    val isNew: Boolean get() = id == null
    val canSave: Boolean get() = name.isNotBlank()

    /** The scale's step, or 1 g when the field is empty or nonsense. */
    val weighingStepGrams: Double
        get() = weighingStep.trim().toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0

    /**
     * True only for an existing plant whose method or scale actually moved. A
     * new plant has no anchors to invalidate, and reopening the form without
     * touching anything must not throw a drying history away.
     */
    /**
     * The plant whose pot we are about to mint, so it can be put in it too.
     * Null when joining a container that already exists.
     */
    val sharesWith: ShareOption?
        get() = containerId?.let { cid -> shareOptions.firstOrNull { it.containerId == cid } }
            ?: mintedWith?.let { id -> shareOptions.firstOrNull { it.plantId == id } }

    fun pendingContainerFor(plantId: String): String? =
        if (mintedWith == plantId) containerId else null

    val weighingChanged: Boolean
        get() = originalWeighingMethod != null &&
            (
                originalWeighingMethod != weighingMethod ||
                    kotlin.math.abs((originalWeighingStep ?: 1.0) - weighingStepGrams) > 0.0001
                )
}

@HiltViewModel
class PlantEditViewModel @Inject constructor(
    private val repository: PlantRepository,
    private val reminders: ReminderRepository,
    private val settings: dev.dheirav.thirsttrap.domain.SettingsRepository,
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
                        containerId = p.containerId,
                        sharingOn = p.containerId != null,
                        // Carried over, because this is a whole-state
                        // replacement racing a coroutine that only copies into
                        // it. The pot list is loaded separately and usually
                        // lands first, having one suspend call to this one's
                        // two, so building a fresh state here threw it away and
                        // the "shares a pot with" row never appeared at all.
                        shareOptions = _state.value.shareOptions,
                        defaultWaterMl = p.defaultWaterMl?.toInt()?.toString().orEmpty(),
                        targetDryness = p.targetDryness.orEmpty(),
                        lightNeeds = p.lightNeeds.orEmpty(),
                        fertilizerCadenceDays = p.fertilizerCadenceDays?.toString().orEmpty(),
                        medium = p.medium,
                        source = p.source,
                        status = p.status,
                        // roundToInt, not toInt: 0.29 * 100 is 28.999... in a
                        // Double, and truncation would walk the trigger down a
                        // percent on every open-and-save.
                        depletionTriggerPct = (p.depletionTrigger * 100).roundToInt(),
                        weightTracked = p.weightTracked,
                        weighingMethod = p.weighingMethod,
                        weighingStep = p.weighingStepGrams.let {
                            if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
                        },
                        originalWeighingMethod = p.weighingMethod,
                        originalWeighingStep = p.weighingStepGrams,
                        archived = p.archived,
                        checkIntervalDays = existing?.intervalDays?.toString().orEmpty(),
                        loading = false,
                    )
                }
            }
        }
        // Who else there is to share a pot with. Archived plants are left out:
        // offering to put a cutting in with something that is no longer on the
        // shelf is offering a mistake.
        viewModelScope.launch {
            val others = repository.observePlants().first()
                .filter { it.id != plantId && !it.archived }
                .map { ShareOption(it.id, it.name, it.containerId, it.weightTracked) }
            _state.value = _state.value.copy(shareOptions = others)
        }
        // A new plant's form starts on the settings default rather than the
        // data-class 0.5, so what the user sees is what a plain save stores.
        if (plantId == null) {
            viewModelScope.launch {
                val pct = (settings.settings.first().defaultDepletionTrigger * 100).roundToInt()
                _state.value = _state.value.copy(depletionTriggerPct = pct)
            }
        }
    }

    fun onName(v: String) { _state.value = _state.value.copy(name = v) }
    /**
     * Typing a species fills the care profile from the catalogue.
     *
     * asTargetDryness() was documented as "prefills the plant's own care
     * profile" and had zero callers, while CareViewModel.applySuggestions
     * copied the same four values onto a plant behind a four-step path the user
     * had to find after saving. The numbers were already in the app; nothing
     * put them where they were needed.
     *
     * A field the user has touched always wins, which is the same precedence
     * CareViewModel uses: fill only what is still at its default. That is why
     * this cannot overwrite an answer, only supply a missing one.
     */
    fun onSpecies(v: String) {
        val s = _state.value
        val care = findSpeciesCare(v)
        if (care == null) {
            // Clearing or changing the species away from a match clears the
            // attribution, not the values: the numbers are the plant's now.
            _state.value = s.copy(species = v, prefillFrom = null, prefilled = emptyList())
            return
        }
        if (!s.isNew) {
            _state.value = s.copy(species = v)
            return
        }
        val fill = carePrefill(
            care = care,
            currentLight = s.lightNeeds,
            currentDryness = s.targetDryness,
            currentMedium = s.medium,
            currentTriggerPct = s.depletionTriggerPct,
        )
        _state.value = s.copy(
            species = v,
            lightNeeds = fill.light ?: s.lightNeeds,
            targetDryness = fill.dryness ?: s.targetDryness,
            medium = fill.medium ?: s.medium,
            depletionTriggerPct = fill.depletionTriggerPct ?: s.depletionTriggerPct,
            prefillFrom = if (fill.isEmpty) s.prefillFrom else care.name,
            prefilled = if (fill.isEmpty) s.prefilled else fill.filled,
        )
    }
    fun onLocation(v: String) { _state.value = _state.value.copy(location = v) }
    fun onContainer(v: String) { _state.value = _state.value.copy(containerDesc = v) }

    /**
     * The switch. Turning it off puts the plant back in its own pot, because
     * leaving a chosen container behind an off switch would save a share the
     * form says is not happening.
     */
    fun onSharingToggled(on: Boolean) {
        _state.value = if (on) {
            _state.value.copy(sharingOn = true)
        } else {
            _state.value.copy(sharingOn = false, containerId = null, mintedWith = null)
        }
    }

    /**
     * Share a pot with the plant in this option.
     *
     * If that plant is already in a container this joins it; if it is in its
     * own, a container is minted here and the save puts them both in it. The
     * id is generated now rather than at save time so that choosing the same
     * neighbour twice does not create two pots.
     */
    fun onShareWith(option: ShareOption) {
        val s = _state.value
        val target = option.containerId ?: s.pendingContainerFor(option.plantId) ?: newId()
        _state.value = s.copy(
            containerId = target,
            mintedWith = if (option.containerId == null) option.plantId else null,
            // A pot weighs as one object, so two plants in it would each build
            // a model of the same pot and both be wrong. The neighbour keeps
            // its weighing, since it is the one with the history.
            weightTracked = if (option.weightTracked) false else s.weightTracked,
        )
    }
    fun onMedium(v: Medium) { _state.value = _state.value.copy(medium = v) }
    fun onWeightTracked(v: Boolean) { _state.value = _state.value.copy(weightTracked = v) }

    fun onWeighingMethod(v: dev.dheirav.thirsttrap.domain.WeighingMethod) {
        _state.value = _state.value.copy(weighingMethod = v)
    }

    fun onWeighingStep(v: String) {
        _state.value = _state.value.copy(
            weighingStep = v.filter { it.isDigit() || it == '.' }.take(6),
        )
    }
    fun onSource(v: PlantSource) { _state.value = _state.value.copy(source = v) }
    fun onLastWatered(v: LastWatered) { _state.value = _state.value.copy(lastWatered = v) }
    fun onDepletionTrigger(pct: Int) {
        _state.value = _state.value.copy(depletionTriggerPct = pct.coerceIn(20, 80))
    }
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

    /** Whether to offer the care notes after adding, and whether there are any. */
    val offerCareOnAdd: StateFlow<Boolean> = settings.settings
        .map { it.offerCareOnAdd }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun stopOfferingCare() {
        viewModelScope.launch { settings.setOfferCareOnAdd(false) }
    }

    /**
     * [onSaved] receives the plant's id and whether this was a new plant, so
     * the screen can offer the species care notes at the one moment they are
     * worth reading: just after somebody typed the species in.
     */
    fun save(onSaved: (plantId: String, wasNew: Boolean) -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        val plantId = s.id ?: newId()
        val isNew = s.isNew
        viewModelScope.launch {
            // Edit what the form owns and leave the rest alone. Building a
            // fresh Plant here reset every field the form does not show, so
            // saving a name change would have quietly cleared the depletion
            // trigger, the cover photo, the propagation stage and the pot
            // measurements. Nothing had noticed because the plants that have
            // been edited so far happened to be sitting on the defaults.
            val existing = repository.observePlant(plantId).first()
            // Settings offers 30/50/75 as "how dry a new plant is allowed to
            // get", stores the choice and shows it back, and nothing had ever
            // read it: every plant was created on the data class default of
            // 0.5 whatever the user picked. A new plant starts on the chosen
            // value; an existing one keeps whatever it already has.
            val start = existing ?: Plant(
                id = plantId,
                name = s.name.trim(),
                depletionTrigger = settings.settings.first().defaultDepletionTrigger,
            )
            val edited = start.copy(
                name = s.name.trim(),
                species = s.species.trim().takeIf { it.isNotEmpty() },
                medium = s.medium,
                location = s.location.trim().takeIf { it.isNotEmpty() },
                status = s.status,
                containerDesc = s.containerDesc.trim().takeIf { it.isNotEmpty() },
                containerId = s.containerId,
                defaultWaterMl = s.defaultWaterMl.toDoubleOrNull(),
                targetDryness = s.targetDryness.trim().takeIf { it.isNotEmpty() },
                lightNeeds = s.lightNeeds.trim().takeIf { it.isNotEmpty() },
                fertilizerCadenceDays = s.fertilizerCadenceDays.toIntOrNull(),
                source = s.source,
                depletionTrigger = s.depletionTriggerPct / 100.0,
                weightTracked = s.weightTracked,
                weighingMethod = s.weighingMethod,
                weighingStepGrams = s.weighingStepGrams,
                // A change of method invalidates the anchors exactly as a repot
                // does: they describe a measurement, not a plant. Tipping a pot
                // that used to be lifted changes every number by a constant
                // factor, so the old wet and dry ends describe nothing.
                anchors = if (s.weighingChanged) null else start.anchors,
                needsRecalibration = s.weighingChanged || start.needsRecalibration,
                archived = s.archived,
            )
            repository.upsertPlant(edited)

            // Putting two plants in one pot is a change to both of them. The
            // neighbour is written only when its pot was minted here; joining
            // one it already had leaves it alone.
            s.mintedWith?.let { otherId ->
                repository.observePlant(otherId).first()?.let { other ->
                    repository.upsertPlant(other.copy(containerId = s.containerId))
                }
            }

            // An explicit cadence, if the user gave one. Null means "work it
            // out from the log", which is what resolveIntervalDays does.
            if (!isNew) {
                reminders.observeForPlant(plantId).first().firstOrNull()?.let { r ->
                    reminders.upsert(r.copy(intervalDays = s.checkIntervalDays.toIntOrNull()))
                }
                // The trigger moves the predicted watering date, and the check
                // interval is derived from that prediction - so an edit here
                // has to replan, or the reminder keeps the old trigger's date.
                reminders.rescheduleFromModel(plantId, System.currentTimeMillis())
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
            onSaved(plantId, isNew)
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
