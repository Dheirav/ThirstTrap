package dev.dheirav.thirsttrap.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plants",
    indices = [Index("status"), Index("location"), Index("archived")],
)
data class PlantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val species: String? = null,
    @ColumnInfo(name = "species_source") val speciesSource: String? = null,
    @ColumnInfo(name = "species_confidence") val speciesConfidence: Double? = null,
    /** Epoch DAY, not millis - no clock time is known for an acquisition date. */
    @ColumnInfo(name = "acquired_date") val acquiredDate: Long? = null,
    val source: String,
    /** Current value only; every change also writes a medium_changed event. */
    val medium: String,
    val location: String? = null,
    val status: String,
    @ColumnInfo(name = "container_desc") val containerDesc: String? = null,
    @ColumnInfo(name = "pot_diameter_cm") val potDiameterCm: Double? = null,
    @ColumnInfo(name = "has_drainage") val hasDrainage: Boolean? = null,
    @ColumnInfo(name = "target_dryness") val targetDryness: String? = null,
    @ColumnInfo(name = "light_needs") val lightNeeds: String? = null,
    @ColumnInfo(name = "fertilizer_cadence_days") val fertilizerCadenceDays: Int? = null,
    @ColumnInfo(name = "depletion_trigger") val depletionTrigger: Double,
    @ColumnInfo(name = "wet_anchor_g") val wetAnchorG: Double? = null,
    @ColumnInfo(name = "dry_anchor_g") val dryAnchorG: Double? = null,
    @ColumnInfo(name = "dry_anchor_provisional") val dryAnchorProvisional: Boolean = true,
    @ColumnInfo(name = "slope_ewma_g_per_day") val slopeEwmaGPerDay: Double? = null,
    @ColumnInfo(name = "needs_recalibration") val needsRecalibration: Boolean = false,
    @ColumnInfo(name = "cover_photo_id") val coverPhotoId: String? = null,
    val archived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
