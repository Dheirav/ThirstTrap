package dev.dheirav.thirsttrap.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.dheirav.thirsttrap.data.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Query("SELECT * FROM photos WHERE plant_id = :plantId ORDER BY taken_at DESC")
    fun observeForPlant(plantId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos ORDER BY taken_at DESC")
    fun observeAll(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE care_event_id = :eventId ORDER BY taken_at DESC")
    fun observeForEvent(eventId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :photoId")
    suspend fun byId(photoId: String): PhotoEntity?

    @Query("SELECT * FROM photos WHERE plant_id = :plantId ORDER BY taken_at DESC LIMIT 1")
    suspend fun latestForPlant(plantId: String): PhotoEntity?

    @Query("SELECT * FROM photos")
    suspend fun all(): List<PhotoEntity>

    @Query("SELECT created_at FROM photos WHERE id = :id")
    suspend fun createdAtOf(id: String): Long?

    @Upsert
    suspend fun upsert(photo: PhotoEntity)

    @Query("UPDATE photos SET caption = :caption WHERE id = :photoId")
    suspend fun setCaption(photoId: String, caption: String?)

    @Query("DELETE FROM photos WHERE id = :photoId")
    suspend fun delete(photoId: String)
}
