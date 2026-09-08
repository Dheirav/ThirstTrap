package dev.dheirav.thirsttrap.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.dheirav.thirsttrap.data.entity.AmbientReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AmbientDao {

    @Query("SELECT * FROM ambient_readings ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<AmbientReadingEntity>>

    @Query("SELECT * FROM ambient_readings WHERE location = :location COLLATE NOCASE ORDER BY timestamp DESC")
    fun observeForLocation(location: String): Flow<List<AmbientReadingEntity>>

    @Query("SELECT * FROM ambient_readings ORDER BY timestamp DESC")
    suspend fun all(): List<AmbientReadingEntity>

    /** Distinct locations that already have readings, for the entry screen. */
    @Query("SELECT DISTINCT location FROM ambient_readings ORDER BY location COLLATE NOCASE")
    suspend fun knownLocations(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reading: AmbientReadingEntity)

    @Delete
    suspend fun delete(reading: AmbientReadingEntity)

    @Query("DELETE FROM ambient_readings WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Used by import, which replaces the whole set rather than merging - see
     * ExportRepositoryImpl for why idempotency matters there.
     */
    @Query("DELETE FROM ambient_readings")
    suspend fun clear()
}
