package dev.dheirav.thirsttrap.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "experiments")
data class ExperimentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val variable: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "tz_offset_minutes") val tzOffsetMinutes: Int,
    @ColumnInfo(name = "concluded_at") val concludedAt: Long? = null,
    val conclusion: String? = null,
    val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/**
 * Membership row: which arm a plant belongs to.
 *
 * Cascades both ways on purpose. Deleting the experiment removes its
 * memberships (the plants stay - they were only subjects). Deleting a plant
 * removes it from any experiment, because a subject row pointing at nothing
 * would render as a ghost column on the experiment page.
 */
@Entity(
    tableName = "experiment_subjects",
    primaryKeys = ["experiment_id", "plant_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExperimentEntity::class,
            parentColumns = ["id"],
            childColumns = ["experiment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plant_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("plant_id")],
)
data class ExperimentSubjectEntity(
    @ColumnInfo(name = "experiment_id") val experimentId: String,
    @ColumnInfo(name = "plant_id") val plantId: String,
    val label: String,
)
