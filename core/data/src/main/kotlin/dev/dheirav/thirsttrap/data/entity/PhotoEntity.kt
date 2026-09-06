package dev.dheirav.thirsttrap.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plant_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CareEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["care_event_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    // Drives both the photo grid and the compare view's filmstrip.
    indices = [
        Index(value = ["plant_id", "taken_at"], orders = [Index.Order.ASC, Index.Order.DESC]),
        Index("care_event_id"),
    ],
)
data class PhotoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "plant_id") val plantId: String,
    @ColumnInfo(name = "care_event_id") val careEventId: String? = null,
    /**
     * Relative to the app's files dir - `photos/<plant>/<photo>.jpg`.
     * NEVER absolute: absolute paths break across reinstall and restore, which
     * is the commonest way a photo-heavy app loses its images.
     */
    @ColumnInfo(name = "relative_path") val relativePath: String,
    /** Capture time, which is not necessarily the logging time. */
    @ColumnInfo(name = "taken_at") val takenAt: Long,
    @ColumnInfo(name = "tz_offset_minutes") val tzOffsetMinutes: Int,
    @ColumnInfo(name = "width_px") val widthPx: Int? = null,
    @ColumnInfo(name = "height_px") val heightPx: Int? = null,
    val bytes: Long? = null,
    val caption: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
