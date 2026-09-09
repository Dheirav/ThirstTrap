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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One bottle, and what it means for the can size currently chosen. */
data class FertilizerRow(val fertilizer: Fertilizer, val dose: DoseAdvice)

data class FertilizerUiState(
    val rows: List<FertilizerRow> = emptyList(),
    val canMl: Double = 1000.0,
    val loaded: Boolean = false,
)

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
) : ViewModel() {

    private val _canMl = MutableStateFlow(1000.0)

    val state: StateFlow<FertilizerUiState> =
        combine(repository.observeAll(), _canMl) { list, can ->
            FertilizerUiState(
                rows = list.map { FertilizerRow(it, doseFor(it.dilution, can)) },
                canMl = can,
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FertilizerUiState())

    fun onCan(ml: Double) { _canMl.value = ml }

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
