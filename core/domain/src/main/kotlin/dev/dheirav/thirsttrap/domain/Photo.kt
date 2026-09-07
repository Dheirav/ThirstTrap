package dev.dheirav.thirsttrap.domain

import kotlinx.coroutines.flow.Flow

data class Photo(
    val id: String,
    val plantId: String,
    val careEventId: String? = null,
    /** Relative to the app's files dir. The data layer resolves it to a file. */
    val relativePath: String,
    val takenAtMillis: Long,
    val tzOffsetMinutes: Int,
    val widthPx: Int? = null,
    val heightPx: Int? = null,
    val bytes: Long? = null,
    val caption: String? = null,
)

interface PhotoRepository {

    fun observeForPlant(plantId: String): Flow<List<Photo>>

    fun observeAll(): Flow<List<Photo>>

    suspend fun latestForPlant(plantId: String): Photo?

    /** Resolves a stored relative path to something the UI can load. */
    fun absolutePath(photo: Photo): String

    suspend fun setCaption(photoId: String, caption: String?)

    /** Photos for one event, so the timeline can show them inline. */
    fun observeForEvent(eventId: String): Flow<List<Photo>>

    /** Deletes the row and the file behind it. */
    suspend fun delete(photoId: String)
}
