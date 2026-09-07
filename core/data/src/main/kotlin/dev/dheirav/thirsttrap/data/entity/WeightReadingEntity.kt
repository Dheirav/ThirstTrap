package dev.dheirav.thirsttrap.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Kept separate from care_events on purpose: readings are high-frequency
 * numeric data feeding a model, care events are narrative history. Mixing them
 * would make both the curve query and the timeline slower and uglier.
 *
 * Note there is no drying_segments table, which docs/DATA-MODEL.md originally
 * called for. Segmentation turned out to be a pure function of the readings
 * plus the watering events, so a stored copy would be a cache that can fall out
 * of step with its inputs - a whole class of bug in exchange for arithmetic
 * that costs nothing at this data volume. The EWMA prior is likewise folded
 * from the closed segments on read.
 */
@Entity(
    tableName = "weight_readings",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plant_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["plant_id", "timestamp"])],
)
data class WeightReadingEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "plant_id") val plantId: String,
    val timestamp: Long,
    @ColumnInfo(name = "tz_offset_minutes") val tzOffsetMinutes: Int,
    /** Total weight including the pot. */
    val grams: Double,
    /** routine | pre_water | post_water | calibration */
    val context: String,
    @ColumnInfo(name = "care_event_id") val careEventId: String? = null,
    /** A weigh-in the user has marked as bad - kept, but excluded from fits. */
    val excluded: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
