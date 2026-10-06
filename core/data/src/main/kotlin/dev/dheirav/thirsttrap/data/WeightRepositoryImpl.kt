package dev.dheirav.thirsttrap.data

import dev.dheirav.thirsttrap.data.dao.CareEventDao
import dev.dheirav.thirsttrap.data.dao.PlantDao
import dev.dheirav.thirsttrap.data.dao.WeightDao
import dev.dheirav.thirsttrap.data.entity.WeightReadingEntity
import dev.dheirav.thirsttrap.domain.Anchors
import dev.dheirav.thirsttrap.domain.CareEvent
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
import dev.dheirav.thirsttrap.domain.impliesUnloggedWatering

@Singleton
class WeightRepositoryImpl @Inject constructor(
    private val weightDao: WeightDao,
    private val plantDao: PlantDao,
    private val eventDao: CareEventDao,
    private val sharing: ContainerSharing,
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

    override suspend fun addReading(plantId: String, grams: Double, context: ReadingContext): Boolean {
        val now = System.currentTimeMillis()

        val events = eventDao.observeForPlant(plantId).first()
        val lastWatered = events
            .filter { it.type == CareEventType.WATERED.name }
            .maxOfOrNull { it.timestamp }

        var backfilled = false
        if (impliesUnloggedWatering(context, lastWatered, now)) {
            // The watering has to sort between the previous reading and this
            // one, or segmentation puts the boundary on the wrong side of a
            // pre-water weigh taken moments earlier.
            val lastReading = weightDao.observeForPlant(plantId).first()
                .maxOfOrNull { it.timestamp }
            val wateredAt = maxOf(now - 60_000L, (lastReading ?: 0L) + 1_000L)
                .coerceAtMost(now - 1L)

            // The plant's set standard, or the last amount actually poured -
            // the same fallback the watering sheet itself uses.
            val amount = plantDao.observePlant(plantId).first()?.toDomain()?.defaultWaterMl
                ?: events.firstOrNull {
                    it.type == CareEventType.WATERED.name && it.amountMl != null
                }?.amountMl

            // This is a real watering of a real pot, so it shares like any
            // other. It is the easiest one to forget, because it is written
            // here and not through logEvent.
            val inferred = CareEvent(
                id = newId(),
                plantId = plantId,
                timestampMillis = wateredAt,
                tzOffsetMinutes = tzOffsetMinutesAt(wateredAt),
                type = CareEventType.WATERED,
                amountMl = amount,
                note = "Logged from the post-water weigh-in",
            )
            eventDao.insert(inferred.toEntity(createdAt = now, updatedAt = now))
            sharing.fanOut(inferred, now)
            backfilled = true
            TTLog.i(TTLog.DATA) { "backfilled watering (${amount ?: "?"} ml) for $plantId" }
        }

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
        return backfilled
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
