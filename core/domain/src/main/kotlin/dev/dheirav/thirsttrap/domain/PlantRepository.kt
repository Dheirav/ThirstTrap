package dev.dheirav.thirsttrap.domain

import kotlinx.coroutines.flow.Flow

/**
 * Declared here, implemented in :core:data.
 *
 * Grouped by aggregate rather than by table: plants, their events and (later)
 * their photos are always read together, so a repository per entity would only
 * add ceremony. Weight readings get their own repository because they feed the
 * model rather than the timeline.
 */
interface PlantRepository {

    fun observePlants(includeArchived: Boolean = false): Flow<List<Plant>>

    fun observePlant(plantId: String): Flow<Plant?>

    /** Everything the dashboard needs, already resolved and sorted. */
    fun observeDashboard(nowMillis: () -> Long): Flow<List<PlantAttention>>

    fun observeEvents(plantId: String): Flow<List<CareEvent>>

    /** Every event, across every plant. For stats, which count the collection. */
    fun observeAllEvents(): Flow<List<CareEvent>>

    suspend fun upsertPlant(plant: Plant)

    suspend fun archivePlant(plantId: String, archived: Boolean)

    suspend fun setStatus(plantId: String, status: PlantStatus)

    /** Hard delete. Cascades to events; the caller is responsible for warning first. */
    suspend fun deletePlant(plantId: String)

    suspend fun logEvent(event: CareEvent): String

    suspend fun deleteEvent(eventId: String)

    suspend fun updateEvent(event: CareEvent)

    /** Moves a cutting along the board, recording the move as a milestone. */
    suspend fun setPropagationStage(plantId: String, stage: PropagationStage)
}
