package dev.dheirav.thirsttrap.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import dev.dheirav.thirsttrap.data.entity.CareEventEntity
import dev.dheirav.thirsttrap.data.entity.PlantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {

    @Query("SELECT * FROM plants WHERE (:includeArchived = 1 OR archived = 0) ORDER BY name COLLATE NOCASE")
    fun observePlants(includeArchived: Boolean): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE id = :plantId")
    fun observePlant(plantId: String): Flow<PlantEntity?>

    @Upsert
    suspend fun upsert(plant: PlantEntity)

    @Query("UPDATE plants SET archived = :archived, updated_at = :now WHERE id = :plantId")
    suspend fun setArchived(plantId: String, archived: Boolean, now: Long)

    @Query("UPDATE plants SET status = :status, updated_at = :now WHERE id = :plantId")
    suspend fun setStatus(plantId: String, status: String, now: Long)

    @Query("DELETE FROM plants WHERE id = :plantId")
    suspend fun delete(plantId: String)
}

@Dao
interface CareEventDao {

    @Query("SELECT * FROM care_events WHERE plant_id = :plantId ORDER BY timestamp DESC")
    fun observeForPlant(plantId: String): Flow<List<CareEventEntity>>

    @Query("SELECT * FROM care_events ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CareEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: CareEventEntity)

    @Upsert
    suspend fun upsert(event: CareEventEntity)

    @Query("DELETE FROM care_events WHERE id = :eventId")
    suspend fun delete(eventId: String)
}
