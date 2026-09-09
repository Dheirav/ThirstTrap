package dev.dheirav.thirsttrap.data

import dev.dheirav.thirsttrap.data.dao.FertilizerDao
import dev.dheirav.thirsttrap.data.entity.FertilizerEntity
import dev.dheirav.thirsttrap.domain.Fertilizer
import dev.dheirav.thirsttrap.domain.FertilizerRepository
import dev.dheirav.thirsttrap.domain.parseDilution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FertilizerRepositoryImpl @Inject constructor(
    private val dao: FertilizerDao,
) : FertilizerRepository {

    override fun observeAll(): Flow<List<Fertilizer>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun upsert(fertilizer: Fertilizer) {
        val now = System.currentTimeMillis()
        dao.upsert(
            fertilizer.toEntity(
                createdAt = dao.createdAtOf(fertilizer.id) ?: now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun delete(id: String) = dao.delete(id)
}

fun Fertilizer.toEntity(createdAt: Long, updatedAt: Long): FertilizerEntity = FertilizerEntity(
    id = id,
    name = name.trim(),
    // The typed text, not the parse. See FertilizerEntity.
    dilutionText = dilutionText?.trim()?.takeIf { it.isNotEmpty() },
    npk = npk?.trim()?.takeIf { it.isNotEmpty() },
    note = note?.trim()?.takeIf { it.isNotEmpty() },
    archived = archived,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun FertilizerEntity.toDomain(): Fertilizer = Fertilizer(
    id = id,
    name = name,
    dilutionText = dilutionText,
    dilution = parseDilution(dilutionText),
    npk = npk,
    note = note,
    archived = archived,
)
