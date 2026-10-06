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
        Index("share_group_id"),
    ],
)
data class CareEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "plant_id") val plantId: String,
    /**
     * Ties together the copies of one shared-container event, so an edit or a
     * delete can find the whole set. Null for an ordinary event, which is every
     * event written before containers existed.
     *
     * The alternative was to store the event once on the container and union it
     * in at read time, which needs no copies and no group. It was not taken
     * because 33 call sites across 20 files read a plant's events and every one
     * would have had to learn about containers, against 5 that write one.
     */
    @ColumnInfo(name = "share_group_id") val shareGroupId: String? = null,
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
