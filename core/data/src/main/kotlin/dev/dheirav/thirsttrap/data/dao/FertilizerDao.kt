package dev.dheirav.thirsttrap.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.dheirav.thirsttrap.data.entity.FertilizerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FertilizerDao {

    @Query("SELECT * FROM fertilizers WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<FertilizerEntity>>

    @Query("SELECT * FROM fertilizers")
    suspend fun all(): List<FertilizerEntity>

    @Query("SELECT created_at FROM fertilizers WHERE id = :id")
    suspend fun createdAtOf(id: String): Long?

    @Query("SELECT updated_at FROM fertilizers WHERE id = :id")
    suspend fun updatedAtOf(id: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: FertilizerEntity)

    @Query("DELETE FROM fertilizers WHERE id = :id")
    suspend fun delete(id: String)
}
