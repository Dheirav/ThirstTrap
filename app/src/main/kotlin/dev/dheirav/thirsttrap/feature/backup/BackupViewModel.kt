package dev.dheirav.thirsttrap.feature.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.BuildConfig
import dev.dheirav.thirsttrap.data.ExportRepositoryImpl
import dev.dheirav.thirsttrap.domain.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BackupStatus {
    data object Idle : BackupStatus
    data object Working : BackupStatus
    data class Exported(val photoCount: Int) : BackupStatus
    data class Imported(val result: ImportResult) : BackupStatus
    /** Export failure is serious and must never be swallowed - UI-SPEC section 9. */
    data class Failed(val what: String, val reason: String) : BackupStatus
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exporter: ExportRepositoryImpl,
) : ViewModel() {

    private val _status = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val status: StateFlow<BackupStatus> = _status.asStateFlow()

    fun suggestedFileName(): String = exporter.suggestedFileName()

    fun export(destination: Uri) {
        _status.value = BackupStatus.Working
        viewModelScope.launch {
            exporter.exportTo(destination, BuildConfig.VERSION_NAME)
                .onSuccess { _status.value = BackupStatus.Exported(it) }
                .onFailure {
                    _status.value = BackupStatus.Failed("Export", it.message ?: it::class.simpleName.orEmpty())
                }
        }
    }

    fun import(source: Uri) {
        _status.value = BackupStatus.Working
        viewModelScope.launch {
            exporter.importFrom(source)
                .onSuccess { _status.value = BackupStatus.Imported(it) }
                .onFailure {
                    _status.value = BackupStatus.Failed("Import", it.message ?: it::class.simpleName.orEmpty())
                }
        }
    }

    fun clearStatus() { _status.value = BackupStatus.Idle }
}
