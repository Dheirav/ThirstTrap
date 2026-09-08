package dev.dheirav.thirsttrap.data

import dev.dheirav.thirsttrap.data.dao.LocationDao
import dev.dheirav.thirsttrap.data.entity.LocationNoteEntity
import dev.dheirav.thirsttrap.domain.LocationNote
import dev.dheirav.thirsttrap.domain.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val dao: LocationDao,
) : LocationRepository {

    override fun observeAll(): Flow<List<LocationNote>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun get(name: String): LocationNote? = dao.get(name.key())?.toDomain()

    override suspend fun setNote(name: String, note: String?) {
        val now = System.currentTimeMillis()
        val existing = dao.get(name.key())
        dao.upsert(
            (existing ?: blank(name, now)).copy(
                name = name.trim(),
                note = note?.takeIf { it.isNotBlank() },
                updatedAt = now,
            ),
        )
    }

    override suspend fun recordLight(name: String, lux: Float, atMillis: Long) {
        val existing = dao.get(name.key())
        dao.upsert(
            (existing ?: blank(name, atMillis)).copy(
                name = name.trim(),
                lux = lux,
                luxMeasuredAt = atMillis,
                updatedAt = atMillis,
            ),
        )
        TTLog.i(TTLog.DATA) { "light ${lux.toInt()} lux at '$name'" }
    }

    override suspend fun delete(name: String) = dao.delete(name.key())

    private fun blank(name: String, now: Long) =
        LocationNoteEntity(nameKey = name.key(), name = name.trim(), updatedAt = now)
}

/** "Windowsill" and "windowsill" are one place. */
private fun String.key(): String = trim().lowercase()

internal fun LocationNoteEntity.toDomain() = LocationNote(
    name = name,
    note = note,
    lux = lux,
    luxMeasuredAtMillis = luxMeasuredAt,
    updatedAtMillis = updatedAt,
)
