package dev.dheirav.thirsttrap.feature.compare

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.Photo
import dev.dheirav.thirsttrap.domain.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class CompareViewModel @Inject constructor(
    private val photos: PhotoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val plantId: String = checkNotNull(savedStateHandle["id"])

    val allPhotos: StateFlow<List<Photo>> =
        photos.observeForPlant(plantId)
            // Oldest first, so the filmstrip reads left-to-right as time passes.
            .map { list -> list.sortedBy { it.takenAtMillis } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _left = MutableStateFlow<String?>(null)
    val left: StateFlow<String?> = _left.asStateFlow()

    private val _right = MutableStateFlow<String?>(null)
    val right: StateFlow<String?> = _right.asStateFlow()

    private val _syncZoom = MutableStateFlow(true)
    val syncZoom: StateFlow<Boolean> = _syncZoom.asStateFlow()

    init {
        viewModelScope.launch {
            allPhotos.collect { list ->
                // Defaults to oldest and newest - the comparison you almost
                // always want when you open this screen.
                if (_left.value == null && list.isNotEmpty()) _left.value = list.first().id
                if (_right.value == null && list.size > 1) _right.value = list.last().id
            }
        }
    }

    fun selectLeft(id: String) { _left.value = id }
    fun selectRight(id: String) { _right.value = id }
    fun toggleSync() { _syncZoom.value = !_syncZoom.value }

    fun pathOf(photo: Photo): String = photos.absolutePath(photo)

    fun photoById(id: String?): Photo? = allPhotos.value.firstOrNull { it.id == id }

    /** Days between the two, which is the whole point of putting them side by side. */
    fun elapsedLabel(): String? {
        val a = photoById(_left.value) ?: return null
        val b = photoById(_right.value) ?: return null
        val days = abs(b.takenAtMillis - a.takenAtMillis) / 86_400_000.0
        return when {
            days < 1 -> "Same day"
            days < 2 -> "1 day apart"
            days < 60 -> "${days.toInt()} days apart"
            else -> "${(days / 30).toInt()} months apart"
        }
    }
}
