package dev.dheirav.thirsttrap.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "care_events",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plant_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // (plant_id, timestamp DESC) is the timeline query - the hottest read in
    // the app. The others serve stats and a future global feed.
    indices = [
        Index(value = ["plant_id", "timestamp"], orders = [Index.Order.ASC, Index.Order.DESC]),
        Index(value = ["type", "timestamp"]),
        Index("timestamp"),
    ],
)
data class CareEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "plant_id") val plantId: String,
    /** UTC millis. Paired with tz_offset_minutes - see docs/DATA-MODEL.md. */
    val timestamp: Long,
    @ColumnInfo(name = "tz_offset_minutes") val tzOffsetMinutes: Int,
    val type: String,
    val note: String? = null,
    @ColumnInfo(name = "amount_ml") val amountMl: Double? = null,
    val method: String? = null,
    @ColumnInfo(name = "check_result") val checkResult: String? = null,
    @ColumnInfo(name = "fertilizer_name") val fertilizerName: String? = null,
    val dilution: String? = null,
    @ColumnInfo(name = "from_medium") val fromMedium: String? = null,
    @ColumnInfo(name = "to_medium") val toMedium: String? = null,
    @ColumnInfo(name = "milestone_kind") val milestoneKind: String? = null,
    val cause: String? = null,
    /** Structured counterpart to the "Moved to rooting" note. */
    @ColumnInfo(name = "propagation_stage") val propagationStage: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
