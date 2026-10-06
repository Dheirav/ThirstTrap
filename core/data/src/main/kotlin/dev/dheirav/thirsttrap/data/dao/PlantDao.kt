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

    /**
     * When this row first entered this database, or null if it never has.
     *
     * Import needs it: restoring a backup must not rewrite the date a plant was
     * added just because the row is being written again.
     */
    @Query("SELECT created_at FROM plants WHERE id = :id")
    suspend fun createdAtOf(id: String): Long?

    @Query("SELECT updated_at FROM plants WHERE id = :id")
    suspend fun updatedAtOf(id: String): Long?

    @Upsert
    suspend fun upsert(plant: PlantEntity)

    @Query("UPDATE plants SET archived = :archived, updated_at = :now WHERE id = :plantId")
    suspend fun setArchived(plantId: String, archived: Boolean, now: Long)

    @Query("UPDATE plants SET status = :status, updated_at = :now WHERE id = :plantId")
    suspend fun setStatus(plantId: String, status: String, now: Long)

    @Query("DELETE FROM plants WHERE id = :plantId")
    suspend fun delete(plantId: String)

    /** Everyone sharing this container, the plant itself included. */
    @Query("SELECT * FROM plants WHERE container_id = :containerId")
    suspend fun inContainer(containerId: String): List<PlantEntity>

    @Query("SELECT container_id FROM plants WHERE id = :plantId")
    suspend fun containerOf(plantId: String): String?
}

@Dao
interface CareEventDao {

    @Query("SELECT * FROM care_events WHERE plant_id = :plantId ORDER BY timestamp DESC")
    fun observeForPlant(plantId: String): Flow<List<CareEventEntity>>

    @Query("SELECT * FROM care_events ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CareEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: CareEventEntity)

    @Query("SELECT created_at FROM care_events WHERE id = :id")
    suspend fun createdAtOf(id: String): Long?

    @Query("SELECT updated_at FROM care_events WHERE id = :id")
    suspend fun updatedAtOf(id: String): Long?

    @Upsert
    suspend fun upsert(event: CareEventEntity)

    @Query("DELETE FROM care_events WHERE id = :eventId")
    suspend fun delete(eventId: String)

    // ---- shared containers ----
    //
    // An edit or a delete has to reach every copy of a shared event, not just
    // the one the user happened to be looking at. The group id is how the set
    // is found again; without it a corrected watering leaves a stale twin on
    // the other plant in the pot.

    @Query("SELECT share_group_id FROM care_events WHERE id = :eventId")
    suspend fun shareGroupOf(eventId: String): String?

    @Query("SELECT * FROM care_events WHERE share_group_id = :groupId")
    suspend fun inShareGroup(groupId: String): List<CareEventEntity>

    @Query("DELETE FROM care_events WHERE share_group_id = :groupId")
    suspend fun deleteShareGroup(groupId: String)
}
