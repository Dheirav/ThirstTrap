package dev.dheirav.thirsttrap.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Keyed by the location name itself, stored lowercase.
 *
 * The display name is kept separately so "Windowsill" and "windowsill" are one
 * place while the user's own capitalisation survives.
 */
@Entity(tableName = "location_notes")
data class LocationNoteEntity(
    @PrimaryKey @ColumnInfo(name = "name_key") val nameKey: String,
    val name: String,
    val note: String? = null,
    val lux: Float? = null,
    @ColumnInfo(name = "lux_measured_at") val luxMeasuredAt: Long? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
