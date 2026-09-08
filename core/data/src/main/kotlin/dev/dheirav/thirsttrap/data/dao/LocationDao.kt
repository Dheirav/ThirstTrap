package dev.dheirav.thirsttrap.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.dheirav.thirsttrap.data.entity.LocationNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("SELECT * FROM location_notes ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<LocationNoteEntity>>

    @Query("SELECT * FROM location_notes")
    suspend fun all(): List<LocationNoteEntity>

    @Query("SELECT * FROM location_notes WHERE name_key = :key")
    suspend fun get(key: String): LocationNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: LocationNoteEntity)

    @Query("DELETE FROM location_notes WHERE name_key = :key")
    suspend fun delete(key: String)
}
