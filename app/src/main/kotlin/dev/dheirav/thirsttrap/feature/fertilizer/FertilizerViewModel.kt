package dev.dheirav.thirsttrap.feature.fertilizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.DoseAdvice
import dev.dheirav.thirsttrap.domain.Fertilizer
import dev.dheirav.thirsttrap.domain.FertilizerRepository
import dev.dheirav.thirsttrap.domain.doseFor
import dev.dheirav.thirsttrap.domain.newId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One bottle, and what it means for the can size currently chosen. */
data class FertilizerRow(val fertilizer: Fertilizer, val dose: DoseAdvice)

data class FertilizerUiState(
    val rows: List<FertilizerRow> = emptyList(),
    /** Exactly what is in the field, so a half-typed number stays half-typed. */
    val canText: String = "",
    /** The sizes this person has saved. Theirs, not a shipped guess. */
    val savedSizes: List<Double> = emptyList(),
    val loaded: Boolean = false,
) {
    val canMl: Double? get() = canText.trim().toDoubleOrNull()?.takeIf { it > 0 }

    /** Offer to save only what is typed, valid, and not already a chip. */
    val canSaveSize: Boolean
        get() = canMl?.let { ml -> savedSizes.none { kotlin.math.abs(it - ml) < 0.001 } } ?: false
}

/**
 * The inventory and the calculator are one screen, because they are one
 * question. Nobody wants to know a dilution ratio; they want to know how much
 * to pour into the can they are holding. Choosing the can size once and reading
 * the whole cupboard off a column beats a separate calculator you have to feed
 * a bottle into.
 */
@HiltViewModel
class FertilizerViewModel @Inject constructor(
    private val repository: FertilizerRepository,
    private val settings: dev.dheirav.thirsttrap.domain.SettingsRepository,
) : ViewModel() {

    private val _canText = MutableStateFlow<String?>(null)

    init {
        // Seeded from the remembered can, once. After that the field is the
        // truth, so typing over it is not fought by the flow.
        viewModelScope.launch {
            val remembered = settings.settings.first().wateringCanMl
            if (_canText.value == null) {
                _canText.value = remembered.let {
                    if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
                }
            }
        }
    }

    val state: StateFlow<FertilizerUiState> =
        combine(repository.observeAll(), _canText, settings.settings) { list, text, prefs ->
            val can = text?.trim()?.toDoubleOrNull()?.takeIf { it > 0 }
            FertilizerUiState(
                rows = list.map {
                    FertilizerRow(it, if (can == null) DoseAdvice.Unknown else doseFor(it.dilution, can))
                },
                canText = text.orEmpty(),
                savedSizes = prefs.wateringCanSizesMl,
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FertilizerUiState())

    /** Keeps the typed size as a chip, so it is typed once and then chosen. */
    fun saveCurrentSize() {
        val ml = _canText.value?.trim()?.toDoubleOrNull()?.takeIf { it > 0 } ?: return
        viewModelScope.launch {
            val current = settings.settings.first().wateringCanSizesMl
            settings.setWateringCanSizes(current + ml)
        }
    }

    fun forgetSize(ml: Double) {
        viewModelScope.launch {
            val current = settings.settings.first().wateringCanSizesMl
            settings.setWateringCanSizes(current.filter { kotlin.math.abs(it - ml) >= 0.001 })
        }
    }

    /** Digits and one decimal point; nothing else can be a volume. */
    fun onCanText(v: String) {
        val cleaned = v.filter { it.isDigit() || it == '.' }.take(7)
        _canText.value = cleaned
        cleaned.toDoubleOrNull()?.takeIf { it > 0 }?.let { ml ->
            viewModelScope.launch { settings.setWateringCanMl(ml) }
        }
    }

    fun save(existing: Fertilizer?, name: String, dilution: String, npk: String, note: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.upsert(
                Fertilizer(
                    id = existing?.id ?: newId(),
                    name = trimmed,
                    dilutionText = dilution,
                    npk = npk,
                    note = note,
                ),
            )
        }
    }

    fun delete(id: String) = viewModelScope.launch { repository.delete(id) }
}
