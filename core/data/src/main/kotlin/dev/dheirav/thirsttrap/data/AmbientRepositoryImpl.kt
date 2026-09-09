package dev.dheirav.thirsttrap.data

import dev.dheirav.thirsttrap.data.dao.AmbientDao
import dev.dheirav.thirsttrap.data.entity.AmbientReadingEntity
import dev.dheirav.thirsttrap.domain.AmbientReading
import dev.dheirav.thirsttrap.domain.AmbientRepository
import dev.dheirav.thirsttrap.domain.AmbientSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AmbientRepositoryImpl @Inject constructor(
    private val dao: AmbientDao,
) : AmbientRepository {

    override fun observeAll(): Flow<List<AmbientReading>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun all(): List<AmbientReading> = dao.all().map { it.toDomain() }


    override suspend fun record(reading: AmbientReading) {
        TTLog.i(TTLog.DATA) { "ambient ${reading.location} ${reading.temperatureC}C ${reading.humidityPercent}%" }
        dao.upsert(reading.toEntity())
    }

    override suspend fun delete(id: String) = dao.deleteById(id)
}

internal fun AmbientReadingEntity.toDomain() = AmbientReading(
    id = id,
    location = location,
    timestampMillis = timestamp,
    tzOffsetMinutes = tzOffsetMinutes,
    temperatureC = temperatureC,
    humidityPercent = humidityPercent,
    // An unrecognised source must not lose the reading - the numbers are the
    // point and the label is metadata.
    source = AmbientSource.entries.firstOrNull { it.name.equals(source, ignoreCase = true) }
        ?: AmbientSource.MANUAL,
    note = note,
)

internal fun AmbientReading.toEntity() = AmbientReadingEntity(
    id = id,
    location = location,
    timestamp = timestampMillis,
    tzOffsetMinutes = tzOffsetMinutes,
    temperatureC = temperatureC,
    humidityPercent = humidityPercent,
    source = source.name.lowercase(),
    note = note,
)
