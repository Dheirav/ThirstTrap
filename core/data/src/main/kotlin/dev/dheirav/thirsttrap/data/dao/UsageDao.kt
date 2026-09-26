package dev.dheirav.thirsttrap.data.dao

import androidx.room.Dao
import androidx.room.Upsert
import androidx.room.Query
import dev.dheirav.thirsttrap.data.entity.UsageEventEntity

@Dao
interface UsageDao {

    // Upsert, not insert: "importing the same file twice changes nothing"
    // (D27) applies to this table like every other.
    @Upsert
    suspend fun insert(event: UsageEventEntity)

    @Query("SELECT * FROM usage_events ORDER BY timestamp")
    suspend fun all(): List<UsageEventEntity>
}
