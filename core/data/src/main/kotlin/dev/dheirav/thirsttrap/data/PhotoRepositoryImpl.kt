package dev.dheirav.thirsttrap.data

import android.net.Uri
import dev.dheirav.thirsttrap.data.dao.PhotoDao
import dev.dheirav.thirsttrap.data.entity.PhotoEntity
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.Photo
import dev.dheirav.thirsttrap.domain.PhotoRepository
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.newId
import dev.dheirav.thirsttrap.domain.tzOffsetMinutesAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoRepositoryImpl @Inject constructor(
    private val dao: PhotoDao,
    private val store: PhotoStore,
    private val plants: dagger.Lazy<PlantRepository>,
) : PhotoRepository {

    override fun observeForPlant(plantId: String): Flow<List<Photo>> =
        dao.observeForPlant(plantId).map { rows -> rows.map { it.toDomain() } }

    override fun observeForEvent(eventId: String): Flow<List<Photo>> =
        dao.observeForEvent(eventId).map { rows -> rows.map { it.toDomain() } }

    override fun observeAll(): Flow<List<Photo>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun latestForPlant(plantId: String): Photo? =
        dao.latestForPlant(plantId)?.toDomain()

    override fun absolutePath(photo: Photo): String =
        store.absoluteFile(photo.relativePath).absolutePath

    /**
     * Gives an observation entry to any photo that has none.
     *
     * Photos taken before photo-to-event linking existed have a null
     * careEventId, which means they never appear in the timeline - the export
     * showed three of them. Idempotent, so it is safe on every launch.
     */
    suspend fun backfillPhotoEvents() {
        val orphans = dao.all().filter { it.careEventId == null }
        if (orphans.isEmpty()) return
        TTLog.i(TTLog.PHOTO) { "backfilling entries for ${orphans.size} photo(s)" }
        for (row in orphans) {
            val eventId = newId()
            plants.get().logEvent(
                CareEvent(
                    id = eventId,
                    plantId = row.plantId,
                    timestampMillis = row.takenAt,
                    tzOffsetMinutes = row.tzOffsetMinutes,
                    type = CareEventType.OBSERVATION,
                ),
            )
            dao.upsert(row.copy(careEventId = eventId))
        }
    }

    override suspend fun setCaption(photoId: String, caption: String?) =
        dao.setCaption(photoId, caption?.trim()?.takeIf { it.isNotEmpty() })

    override suspend fun delete(photoId: String) {
        val row = dao.byId(photoId) ?: return
        TTLog.i(TTLog.PHOTO) { "delete photo $photoId (${row.relativePath})" }
        dao.delete(photoId)
        // SQLite cascade cannot touch the filesystem, so the file goes here.
        store.delete(row.relativePath)
    }

    /**
     * Imports an image: compresses, strips metadata, writes the file, inserts
     * the row. Runs off the main thread - this decodes a bitmap.
     */
    /**
     * @param attachToEventId link to an existing entry, or null to create an
     *        observation entry of its own. A photo with no place in the
     *        timeline is a photo you will never find again.
     */
    suspend fun importPhoto(
        plantId: String,
        source: Uri,
        attachToEventId: String? = null,
    ): Photo? = withContext(Dispatchers.IO) {
        val photoId = newId()
        val saved = store.saveFrom(source, plantId, photoId)
        if (saved == null) {
            TTLog.w(TTLog.PHOTO, { "could not read image from $source" })
            return@withContext null
        }
        TTLog.i(TTLog.PHOTO) { "saved ${saved.relativePath} ${saved.bytes}B ${saved.width}x${saved.height}" }
        val now = System.currentTimeMillis()
        // A gallery import keeps its original capture time; a fresh camera shot
        // has none, so it falls back to now.
        val takenAt = saved.takenAt ?: now

        val eventId = attachToEventId ?: newId().also { id ->
            plants.get().logEvent(
                CareEvent(
                    id = id,
                    plantId = plantId,
                    timestampMillis = takenAt,
                    tzOffsetMinutes = tzOffsetMinutesAt(takenAt),
                    type = CareEventType.OBSERVATION,
                ),
            )
        }

        val entity = PhotoEntity(
            id = photoId,
            plantId = plantId,
            careEventId = eventId,
            relativePath = saved.relativePath,
            takenAt = takenAt,
            tzOffsetMinutes = tzOffsetMinutesAt(takenAt),
            widthPx = saved.width,
            heightPx = saved.height,
            bytes = saved.bytes,
            createdAt = now,
        )
        dao.upsert(entity)
        // The scratch capture has served its purpose; leaving it would keep a
        // full-resolution duplicate in the cache.
        store.clearStaleCaptures()
        entity.toDomain()
    }
}
