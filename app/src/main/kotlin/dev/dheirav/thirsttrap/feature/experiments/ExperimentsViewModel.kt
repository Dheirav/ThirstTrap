package dev.dheirav.thirsttrap.feature.experiments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.ExperimentRepository
import dev.dheirav.thirsttrap.domain.ExperimentWithSubjects
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExperimentsUiState(
    val running: List<ExperimentWithSubjects> = emptyList(),
    val concluded: List<ExperimentWithSubjects> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class ExperimentsViewModel @Inject constructor(
    private val repository: ExperimentRepository,
) : ViewModel() {

    val state: StateFlow<ExperimentsUiState> =
        repository.observeAll().map { all ->
            ExperimentsUiState(
                running = all.filter { !it.experiment.isConcluded },
                // Concluded ones stay listed - the write-up is the point.
                concluded = all.filter { it.experiment.isConcluded },
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExperimentsUiState())

    fun create(name: String, variable: String, onCreated: (String) -> Unit) {
        if (name.isBlank() || variable.isBlank()) return
        viewModelScope.launch {
            onCreated(repository.create(name.trim(), variable.trim(), note = null))
        }
    }
}
