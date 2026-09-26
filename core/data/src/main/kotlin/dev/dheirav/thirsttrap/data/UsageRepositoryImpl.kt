package dev.dheirav.thirsttrap.data

import dev.dheirav.thirsttrap.data.dao.UsageDao
import dev.dheirav.thirsttrap.data.entity.UsageEventEntity
import dev.dheirav.thirsttrap.domain.UsageEvent
import dev.dheirav.thirsttrap.domain.UsageKind
import dev.dheirav.thirsttrap.domain.UsageRepository
import dev.dheirav.thirsttrap.domain.newId
import dev.dheirav.thirsttrap.domain.tzOffsetMinutesAt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepositoryImpl @Inject constructor(
    private val dao: UsageDao,
) : UsageRepository {

    override suspend fun record(kind: UsageKind, flow: String, plantId: String?, detail: String?) {
        val now = System.currentTimeMillis()
        dao.insert(
            UsageEventEntity(
                id = newId(),
                timestamp = now,
                tzOffsetMinutes = tzOffsetMinutesAt(now),
                kind = kind.name,
                flow = flow,
                plantId = plantId,
                detail = detail,
            ),
        )
    }

    override suspend fun all(): List<UsageEvent> = dao.all().map {
        UsageEvent(
            id = it.id,
            timestampMillis = it.timestamp,
            tzOffsetMinutes = it.tzOffsetMinutes,
            kind = runCatching { UsageKind.valueOf(it.kind) }.getOrDefault(UsageKind.FLOW_OPENED),
            flow = it.flow,
            plantId = it.plantId,
            detail = it.detail,
        )
    }
}

fun UsageEvent.toEntity(): UsageEventEntity = UsageEventEntity(
    id = id,
    timestamp = timestampMillis,
    tzOffsetMinutes = tzOffsetMinutes,
    kind = kind.name,
    flow = flow,
    plantId = plantId,
    detail = detail,
)
