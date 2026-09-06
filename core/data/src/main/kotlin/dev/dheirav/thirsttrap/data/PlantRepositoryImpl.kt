package dev.dheirav.thirsttrap.data

import dev.dheirav.thirsttrap.data.dao.CareEventDao
import dev.dheirav.thirsttrap.data.dao.PlantDao
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantAttention
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.PlantStatus
import dev.dheirav.thirsttrap.domain.Prediction
import dev.dheirav.thirsttrap.domain.SuppressionReason
import dev.dheirav.thirsttrap.domain.sortByAttention
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlantRepositoryImpl @Inject constructor(
    private val plantDao: PlantDao,
    private val eventDao: CareEventDao,
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
        ) { plantRows, eventRows ->
            val now = nowMillis()
            val eventsByPlant = eventRows.groupBy { it.plantId }

            val items = plantRows.map { row ->
                val plant = row.toDomain()
                val events = eventsByPlant[row.id].orEmpty()

                val lastWatered = events
                    .firstOrNull { it.type == CareEventType.WATERED.name }?.timestamp
                val lastChecked = events.firstOrNull {
                    it.type == CareEventType.WATERED.name || it.type == CareEventType.CHECKED.name
                }?.timestamp

                PlantAttention(
                    plant = plant,
                    lastWateredMillis = lastWatered,
                    lastCheckedMillis = lastChecked,
                    reminderDueMillis = null, // reminders land in M0.5 step 4
                    depletion = null,
                    prediction = Prediction.NeedAnotherReading(
                        if (plant.anchors == null) SuppressionReason.NOT_CALIBRATED
                        else SuppressionReason.NO_READINGS,
                    ),
                )
            }
            sortByAttention(items, now)
        }

    override fun observeEvents(plantId: String): Flow<List<CareEvent>> =
        eventDao.observeForPlant(plantId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun upsertPlant(plant: Plant) {
        val now = System.currentTimeMillis()
        plantDao.upsert(plant.toEntity(createdAt = now, updatedAt = now))
    }

    override suspend fun archivePlant(plantId: String, archived: Boolean) =
        plantDao.setArchived(plantId, archived, System.currentTimeMillis())

    override suspend fun setStatus(plantId: String, status: PlantStatus) =
        plantDao.setStatus(plantId, status.name, System.currentTimeMillis())

    override suspend fun deletePlant(plantId: String) = plantDao.delete(plantId)

    override suspend fun logEvent(event: CareEvent): String {
        val now = System.currentTimeMillis()
        eventDao.insert(event.toEntity(createdAt = now, updatedAt = now))
        return event.id
    }

    override suspend fun deleteEvent(eventId: String) = eventDao.delete(eventId)

    override suspend fun updateEvent(event: CareEvent) {
        val now = System.currentTimeMillis()
        eventDao.upsert(event.toEntity(createdAt = now, updatedAt = now))
    }
}
