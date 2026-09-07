package dev.dheirav.thirsttrap.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.dheirav.thirsttrap.data.entity.WeightReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {

    @Query("SELECT * FROM weight_readings WHERE plant_id = :plantId ORDER BY timestamp")
    fun observeForPlant(plantId: String): Flow<List<WeightReadingEntity>>

    @Query("SELECT * FROM weight_readings ORDER BY timestamp")
    fun observeAll(): Flow<List<WeightReadingEntity>>

    @Query("SELECT * FROM weight_readings WHERE plant_id = :plantId ORDER BY timestamp")
    suspend fun forPlant(plantId: String): List<WeightReadingEntity>

    @Query("SELECT * FROM weight_readings")
    suspend fun all(): List<WeightReadingEntity>

    @Upsert
    suspend fun upsert(reading: WeightReadingEntity)

    @Query("UPDATE weight_readings SET excluded = :excluded WHERE id = :id")
    suspend fun setExcluded(id: String, excluded: Boolean)

    @Query("DELETE FROM weight_readings WHERE id = :id")
    suspend fun delete(id: String)
}
