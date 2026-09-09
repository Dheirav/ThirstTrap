package dev.dheirav.thirsttrap.data

import dev.dheirav.thirsttrap.data.dao.CareEventDao
import dev.dheirav.thirsttrap.data.dao.PhotoDao
import dev.dheirav.thirsttrap.data.dao.WeightDao
import dev.dheirav.thirsttrap.data.dao.PlantDao
import dev.dheirav.thirsttrap.data.dao.ReminderDao
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantAttention
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.PlantStatus
import dev.dheirav.thirsttrap.domain.PropagationStage
import dev.dheirav.thirsttrap.domain.newId
import dev.dheirav.thirsttrap.domain.tzOffsetMinutesAt
import dev.dheirav.thirsttrap.domain.Prediction
import dev.dheirav.thirsttrap.domain.assembleWeightState
import dev.dheirav.thirsttrap.domain.averageWateringIntervalDays
import dev.dheirav.thirsttrap.domain.sortByAttention
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlantRepositoryImpl @Inject constructor(
    private val plantDao: PlantDao,
    private val eventDao: CareEventDao,
    private val reminderDao: ReminderDao,
    private val photoDao: PhotoDao,
    private val photoStore: PhotoStore,
    private val weightDao: WeightDao,
) : PlantRepository {

    override fun observePlants(includeArchived: Boolean): Flow<List<Plant>> =
        plantDao.observePlants(includeArchived).map { rows -> rows.map { it.toDomain() } }

    override fun observePlant(plantId: String): Flow<Plant?> =
        plantDao.observePlant(plantId).map { it?.toDomain() }

    /**
     * Joins plants to their events once and derives everything the dashboard
     * needs, so the UI does no reasoning of its own.
     *
     * Weight readings are not wired up yet, so every prediction is currently the
     * NOT_CALIBRATED suppression - which is the honest answer until the M2 UI
     * exists, and is exactly what the card is built to render.
     */
    override fun observeDashboard(nowMillis: () -> Long): Flow<List<PlantAttention>> =
        combine(
            plantDao.observePlants(includeArchived = false),
            eventDao.observeAll(),
            reminderDao.observeAll(),
            photoDao.observeAll(),
            weightDao.observeAll(),
        ) { plantRows, eventRows, reminderRows, photoRows, weightRows ->
            val now = nowMillis()
            val eventsByPlant = eventRows.groupBy { it.plantId }
            // An explicitly chosen cover wins; otherwise the most recent photo.
            // A plant's best photo is not always its newest.
            val photosById = photoRows.associateBy { it.id }
            val coverByPlant = photoRows
                .groupBy { it.plantId }
                .mapValues { (plantId, ps) ->
                    val chosen = plantRows.firstOrNull { it.id == plantId }?.coverPhotoId
                    chosen?.let { photosById[it] } ?: ps.maxByOrNull { it.takenAt }
                }
            val dueByPlant = reminderRows
                .filter { it.enabled }
                .groupBy { it.plantId }
                .mapValues { (_, rs) -> rs.minOf { it.nextDueAt } }

            val readingsByPlant = weightRows.groupBy { it.plantId }

            val items = plantRows.map { row ->
                val plant = row.toDomain()
                val events = eventsByPlant[row.id].orEmpty()

                // The whole drying model, run for this plant. Until there are
                // readings this returns the honest "not calibrated" refusal
                // rather than a number nobody should trust.
                val weight = assembleWeightState(
                    plant = plant,
                    readings = readingsByPlant[row.id].orEmpty().map { it.toDomainReading() },
                    wateringEventsMillis = events
                        .filter { it.type == CareEventType.WATERED.name }.map { it.timestamp },
                    repotEventsMillis = events.filter {
                        it.type == CareEventType.REPOTTED.name ||
                            it.type == CareEventType.MEDIUM_CHANGED.name
                    }.map { it.timestamp },
                    nowMillis = now,
                )

                val lastWatered = events
                    .firstOrNull { it.type == CareEventType.WATERED.name }?.timestamp
                val lastChecked = events.firstOrNull {
                    it.type == CareEventType.WATERED.name || it.type == CareEventType.CHECKED.name
                }?.timestamp

                PlantAttention(
                    plant = plant,
                    lastWateredMillis = lastWatered,
                    lastCheckedMillis = lastChecked,
                    reminderDueMillis = dueByPlant[row.id],
                    depletion = weight.depletion,
                    prediction = weight.prediction,
                    coverPhotoPath = coverByPlant[row.id]
                        ?.let { photoStore.absoluteFile(it.relativePath).absolutePath },
                    // An explicit standard wins; otherwise the last amount
                    // actually poured, so a habit becomes the default without
                    // anyone configuring anything.
                    suggestedWaterMl = plant.defaultWaterMl
                        ?: events.firstOrNull {
                            it.type == CareEventType.WATERED.name && it.amountMl != null
                        }?.amountMl,
                    averageIntervalDays = averageWateringIntervalDays(
                        events.filter { it.type == CareEventType.WATERED.name }
                            .map { it.timestamp },
                    ),
                )
            }
            sortByAttention(items, now)
        }

    override fun observeEvents(plantId: String): Flow<List<CareEvent>> =
        eventDao.observeForPlant(plantId).map { rows -> rows.map { it.toDomain() } }

    override fun observeAllEvents(): Flow<List<CareEvent>> =
        eventDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun upsertPlant(plant: Plant) {
        val now = System.currentTimeMillis()
        // Requirement 13 asks for a "moved plant" event. CareEventType.MOVED has
        // existed since the first schema and nothing had ever emitted one.
        //
        // Hooked here rather than in the edit screen so it fires wherever a
        // plant's location changes, and only on a real change - saving the edit
        // form without touching the location must not manufacture a move.
        val beforeRow = plantDao.observePlant(plant.id).first()
        val before = beforeRow?.toDomain()
        val from = before?.location?.takeIf { it.isNotBlank() }
        val to = plant.location?.takeIf { it.isNotBlank() }
        // A row is created once. Stamping created_at with "now" on every save
        // meant editing a plant's name reset the date it was added.
        plantDao.upsert(
            plant.toEntity(createdAt = beforeRow?.createdAt ?: now, updatedAt = now),
        )
        if (before != null && !from.equals(to, ignoreCase = true)) {
            eventDao.insert(
                CareEvent(
                    id = newId(),
                    plantId = plant.id,
                    timestampMillis = now,
                    tzOffsetMinutes = tzOffsetMinutesAt(now),
                    type = CareEventType.MOVED,
                    note = when {
                        from == null -> "Placed on the $to"
                        to == null -> "Taken off the $from"
                        else -> "Moved from $from to $to"
                    },
                ).toEntity(now, now),
            )
        }
        TTLog.i(TTLog.DATA) { "upsert plant ${plant.id} '${plant.name}'" }
    }

    override suspend fun archivePlant(plantId: String, archived: Boolean) =
        plantDao.setArchived(plantId, archived, System.currentTimeMillis())

    override suspend fun setStatus(plantId: String, status: PlantStatus) =
        plantDao.setStatus(plantId, status.name, System.currentTimeMillis())

    override suspend fun deletePlant(plantId: String) {
        TTLog.i(TTLog.DATA) { "DELETE plant $plantId (cascades to its events and photos)" }
        plantDao.delete(plantId)
    }

    override suspend fun logEvent(event: CareEvent): String {
        val now = System.currentTimeMillis()
        eventDao.insert(event.toEntity(createdAt = now, updatedAt = now))
        TTLog.i(TTLog.DATA) { "log ${event.type} for ${event.plantId} (${event.id})" }
        return event.id
    }

    override suspend fun deleteEvent(eventId: String) {
        TTLog.i(TTLog.DATA) { "delete event $eventId" }
        eventDao.delete(eventId)
    }

    override suspend fun setPropagationStage(plantId: String, stage: PropagationStage) {
        val row = plantDao.observePlant(plantId).first() ?: return
        val now = System.currentTimeMillis()
        plantDao.upsert(
            row.copy(
                propagationStage = stage.name,
                propagationStageSince = now,
                updatedAt = now,
            ),
        )
        // The move goes in the timeline too - "rooted on the 12th" is the kind
        // of thing worth having next time.
        eventDao.insert(
            CareEvent(
                id = newId(),
                plantId = plantId,
                timestampMillis = now,
                tzOffsetMinutes = tzOffsetMinutesAt(now),
                type = CareEventType.MILESTONE,
                note = "Moved to ${stage.label.lowercase()}",
                // The note is for a human reading the timeline; this is for
                // anything that needs to count.
                propagationStage = stage,
            ).toEntity(now, now),
        )
        TTLog.i(TTLog.DATA) { "propagation $plantId -> $stage" }
    }

    override suspend fun updateEvent(event: CareEvent) {
        val now = System.currentTimeMillis()
        eventDao.upsert(event.toEntity(createdAt = now, updatedAt = now))
    }
}
