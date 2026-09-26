package dev.dheirav.thirsttrap.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One use of the app's own controls. Actions only, never content - see the
 * domain [dev.dheirav.thirsttrap.domain.UsageEvent] for the rule and the why.
 *
 * Deliberately no foreign key to plants: a usage row about a since-deleted
 * plant is still evidence about the flow it happened in, and cascading it away
 * would quietly bias the friction report toward surviving plants.
 */
@Entity(
    tableName = "usage_events",
    indices = [Index(value = ["flow", "timestamp"])],
)
data class UsageEventEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    @ColumnInfo(name = "tz_offset_minutes") val tzOffsetMinutes: Int,
    /** UsageKind name. */
    val kind: String,
    val flow: String,
    @ColumnInfo(name = "plant_id") val plantId: String? = null,
    val detail: String? = null,
)
