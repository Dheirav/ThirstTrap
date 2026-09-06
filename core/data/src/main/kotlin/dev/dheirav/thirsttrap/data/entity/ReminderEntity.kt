package dev.dheirav.thirsttrap.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plant_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // The daily worker asks "what is due" on every run; this is that query.
    indices = [Index("next_due_at"), Index("plant_id")],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "plant_id") val plantId: String,
    val kind: String,
    val title: String? = null,
    @ColumnInfo(name = "interval_days") val intervalDays: Int? = null,
    @ColumnInfo(name = "next_due_at") val nextDueAt: Long,
    val enabled: Boolean = true,
    @ColumnInfo(name = "snoozed_until") val snoozedUntil: Long? = null,
    @ColumnInfo(name = "last_fired_at") val lastFiredAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
