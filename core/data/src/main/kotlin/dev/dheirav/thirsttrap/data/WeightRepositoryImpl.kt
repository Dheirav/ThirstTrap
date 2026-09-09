package dev.dheirav.thirsttrap.data

import dev.dheirav.thirsttrap.data.dao.CareEventDao
import dev.dheirav.thirsttrap.data.dao.PlantDao
import dev.dheirav.thirsttrap.data.dao.WeightDao
import dev.dheirav.thirsttrap.data.entity.WeightReadingEntity
import dev.dheirav.thirsttrap.domain.Anchors
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.ReadingContext
import dev.dheirav.thirsttrap.domain.WeightReading
import dev.dheirav.thirsttrap.domain.WeightRepository
import dev.dheirav.thirsttrap.domain.WeightState
import dev.dheirav.thirsttrap.domain.assembleWeightState
import dev.dheirav.thirsttrap.domain.newId
import dev.dheirav.thirsttrap.domain.tzOffsetMinutesAt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepositoryImpl @Inject constructor(
    private val weightDao: WeightDao,
    private val plantDao: PlantDao,
    private val eventDao: CareEventDao,
) : WeightRepository {

    override fun observeWeightState(plantId: String): Flow<WeightState> =
        combine(
            plantDao.observePlant(plantId).filterNotNull(),
            weightDao.observeForPlant(plantId),
            eventDao.observeForPlant(plantId),
        ) { plantRow, readingRows, eventRows ->
            // Watering and repot events are what split the readings into drying
            // cycles. Fitting across a watering would average a sawtooth.
            val watered = eventRows
                .filter { it.type == CareEventType.WATERED.name }
                .map { it.timestamp }
            val repotted = eventRows
                .filter {
                    it.type == CareEventType.REPOTTED.name ||
                        it.type == CareEventType.MEDIUM_CHANGED.name
                }
                .map { it.timestamp }

            assembleWeightState(
                plant = plantRow.toDomain(),
                readings = readingRows.map { it.toDomainReading() },
                wateringEventsMillis = watered,
                repotEventsMillis = repotted,
                nowMillis = System.currentTimeMillis(),
            )
        }

    override fun observeAllReadings(): Flow<List<WeightReading>> =
        weightDao.observeAll().map { rows -> rows.map { it.toDomainReading() } }

    override suspend fun addReading(plantId: String, grams: Double, context: ReadingContext) {
        val now = System.currentTimeMillis()
        weightDao.upsert(
            WeightReadingEntity(
                id = newId(),
                plantId = plantId,
                timestamp = now,
                tzOffsetMinutes = tzOffsetMinutesAt(now),
                grams = grams,
                context = context.name,
                createdAt = now,
            ),
        )
        TTLog.i(TTLog.DATA) { "weight ${grams}g ($context) for $plantId" }
    }

    override suspend fun setExcluded(readingId: String, excluded: Boolean) {
        TTLog.i(TTLog.DATA) { "reading $readingId excluded=$excluded" }
        weightDao.setExcluded(readingId, excluded)
    }

    override suspend fun updateReading(reading: WeightReading) {
        val existing = weightDao.all().firstOrNull { it.id == reading.id } ?: return
        TTLog.i(TTLog.DATA) { "edit reading ${reading.id} -> ${reading.grams}g ${reading.context}" }
        weightDao.upsert(
            existing.copy(
                grams = reading.grams,
                context = reading.context.name,
                excluded = reading.excluded,
            ),
        )
    }

    override suspend fun deleteReading(readingId: String) {
        TTLog.i(TTLog.DATA) { "delete reading $readingId" }
        weightDao.delete(readingId)
    }


}
