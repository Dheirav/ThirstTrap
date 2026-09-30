package dev.dheirav.thirsttrap.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.dheirav.thirsttrap.data.entity.ExperimentEntity
import dev.dheirav.thirsttrap.data.entity.ExperimentSubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExperimentDao {

    @Query("SELECT * FROM experiments ORDER BY started_at DESC")
    fun observeAll(): Flow<List<ExperimentEntity>>

    @Query("SELECT * FROM experiments WHERE id = :id")
    fun observe(id: String): Flow<ExperimentEntity?>

    @Query("SELECT * FROM experiment_subjects")
    fun observeSubjects(): Flow<List<ExperimentSubjectEntity>>

    @Query("SELECT * FROM experiments ORDER BY started_at")
    suspend fun all(): List<ExperimentEntity>

    @Query("SELECT * FROM experiment_subjects")
    suspend fun allSubjects(): List<ExperimentSubjectEntity>

    @Query("SELECT created_at FROM experiments WHERE id = :id")
    suspend fun createdAtOf(id: String): Long?

    /** The sibling every other table had, which is why the importer stamped now. */
    @Query("SELECT updated_at FROM experiments WHERE id = :id")
    suspend fun updatedAtOf(id: String): Long?

    @Upsert
    suspend fun upsert(experiment: ExperimentEntity)

    @Upsert
    suspend fun upsertSubject(subject: ExperimentSubjectEntity)

    @Query("DELETE FROM experiment_subjects WHERE experiment_id = :experimentId AND plant_id = :plantId")
    suspend fun removeSubject(experimentId: String, plantId: String)

    @Query("DELETE FROM experiments WHERE id = :id")
    suspend fun delete(id: String)
}
