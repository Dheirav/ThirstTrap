package dev.dheirav.thirsttrap.data

import dev.dheirav.thirsttrap.data.dao.ExperimentDao
import dev.dheirav.thirsttrap.data.entity.ExperimentEntity
import dev.dheirav.thirsttrap.data.entity.ExperimentSubjectEntity
import dev.dheirav.thirsttrap.domain.Experiment
import dev.dheirav.thirsttrap.domain.ExperimentRepository
import dev.dheirav.thirsttrap.domain.ExperimentSubject
import dev.dheirav.thirsttrap.domain.ExperimentWithSubjects
import dev.dheirav.thirsttrap.domain.newId
import dev.dheirav.thirsttrap.domain.tzOffsetMinutesAt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExperimentRepositoryImpl @Inject constructor(
    private val dao: ExperimentDao,
) : ExperimentRepository {

    override fun observeAll(): Flow<List<ExperimentWithSubjects>> =
        combine(dao.observeAll(), dao.observeSubjects()) { experiments, subjects ->
            val byExperiment = subjects.groupBy { it.experimentId }
            experiments.map { e ->
                ExperimentWithSubjects(
                    experiment = e.toDomain(),
                    subjects = byExperiment[e.id].orEmpty().map { it.toDomain() },
                )
            }
        }

    override fun observe(experimentId: String): Flow<ExperimentWithSubjects?> =
        combine(dao.observe(experimentId), dao.observeSubjects()) { e, subjects ->
            e?.let {
                ExperimentWithSubjects(
                    experiment = it.toDomain(),
                    subjects = subjects.filter { s -> s.experimentId == it.id }
                        .map { s -> s.toDomain() },
                )
            }
        }

    override suspend fun create(name: String, variable: String, note: String?): String {
        val now = System.currentTimeMillis()
        val id = newId()
        dao.upsert(
            ExperimentEntity(
                id = id,
                name = name,
                variable = variable,
                startedAt = now,
                tzOffsetMinutes = tzOffsetMinutesAt(now),
                note = note,
                createdAt = now,
                updatedAt = now,
            ),
        )
        TTLog.i(TTLog.DATA) { "experiment created: $name" }
        return id
    }

    override suspend fun addSubject(experimentId: String, plantId: String, label: String) {
        dao.upsertSubject(ExperimentSubjectEntity(experimentId, plantId, label))
    }

    override suspend fun removeSubject(experimentId: String, plantId: String) {
        dao.removeSubject(experimentId, plantId)
    }

    override suspend fun conclude(experimentId: String, conclusion: String) {
        val row = dao.observe(experimentId).first() ?: return
        // One-way on purpose: a concluded experiment stays concluded. The
        // domain doc has the why; the data layer just refuses quietly.
        if (row.concludedAt != null) return
        val now = System.currentTimeMillis()
        dao.upsert(row.copy(concludedAt = now, conclusion = conclusion, updatedAt = now))
        TTLog.i(TTLog.DATA) { "experiment $experimentId concluded" }
    }

    override suspend fun delete(experimentId: String) {
        TTLog.i(TTLog.DATA) { "DELETE experiment $experimentId (subjects rows cascade, plants stay)" }
        dao.delete(experimentId)
    }
}

fun ExperimentEntity.toDomain(): Experiment = Experiment(
    id = id,
    name = name,
    variable = variable,
    startedAtMillis = startedAt,
    tzOffsetMinutes = tzOffsetMinutes,
    concludedAtMillis = concludedAt,
    conclusion = conclusion,
    note = note,
)

fun Experiment.toEntity(createdAt: Long, updatedAt: Long): ExperimentEntity = ExperimentEntity(
    id = id,
    name = name,
    variable = variable,
    startedAt = startedAtMillis,
    tzOffsetMinutes = tzOffsetMinutes,
    concludedAt = concludedAtMillis,
    conclusion = conclusion,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ExperimentSubjectEntity.toDomain(): ExperimentSubject =
    ExperimentSubject(experimentId = experimentId, plantId = plantId, label = label)

fun ExperimentSubject.toEntity(): ExperimentSubjectEntity =
    ExperimentSubjectEntity(experimentId = experimentId, plantId = plantId, label = label)
