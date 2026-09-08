package dev.dheirav.thirsttrap.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A room's conditions at a moment, keyed by location name rather than by plant.
 *
 * Deliberately not a foreign key to anything. A location is free text on
 * [PlantEntity] - "windowsill", "north shelf" - and several plants share one.
 * Attaching ambient readings to a plant would mean logging the same measurement
 * four times for four pots on the same sill, and would lose the reading
 * entirely when that plant is deleted, which is exactly when the history of the
 * spot it lived in becomes interesting.
 *
 * The cost is that renaming a location orphans its readings. That is the right
 * trade: the alternative is a locations table that a user never asked for, and
 * F13 (location notes) is where that would properly belong if it ever lands.
 */
@Entity(
    tableName = "ambient_readings",
    indices = [Index(value = ["location", "timestamp"])],
)
data class AmbientReadingEntity(
    @PrimaryKey val id: String,
    val location: String,
    val timestamp: Long,
    @ColumnInfo(name = "tz_offset_minutes") val tzOffsetMinutes: Int,
    /** Null when the user recorded only humidity, which is common. */
    @ColumnInfo(name = "temperature_c") val temperatureC: Double? = null,
    @ColumnInfo(name = "humidity_percent") val humidityPercent: Double? = null,
    /** manual | weather */
    val source: String,
    val note: String? = null,
)
