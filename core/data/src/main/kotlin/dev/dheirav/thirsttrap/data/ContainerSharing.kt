package dev.dheirav.thirsttrap.data

import dev.dheirav.thirsttrap.data.dao.CareEventDao
import dev.dheirav.thirsttrap.data.dao.PlantDao
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.newId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copying an event onto everyone else in the same pot.
 *
 * This lives on its own rather than inside PlantRepositoryImpl because there is
 * more than one way a watering gets written. The obvious one is logEvent, but
 * WeightRepositoryImpl also writes a WATERED when a post-water weigh-in implies
 * a watering nobody logged, and that is a real watering of a real pot. Two
 * copies of this logic would have drifted the first time one of them changed.
 *
 * Only what is true of the container is copied. Watering, checking and feeding
 * reach every root in the pot whichever plant you tapped, while repotting,
 * pruning, an observation, a milestone, a death and the propagation stages are
 * facts about one plant: copying those would fill a cutting's timeline with a
 * month of its parent's history it was never part of.
 *
 * Each copy is a real row on its own plant, so every existing read of a plant's
 * events keeps working untouched. They carry a shared group id so an edit or a
 * delete can find the whole set again.
 */
@Singleton
class ContainerSharing @Inject constructor(
    private val plantDao: PlantDao,
    private val eventDao: CareEventDao,
) {

    /**
     * Writes the copies for an event that has already been inserted, and stamps
     * the original with the group id so it belongs to the set it created.
     *
     * Does nothing, cheaply, for a plant in its own pot, which is every plant
     * until someone says otherwise.
     */
    suspend fun fanOut(event: CareEvent, now: Long) {
        if (event.type !in SHARED_WITH_CONTAINER) return
        // Already a copy. Without this, a path that passes a group id back in
        // would fan out again and square the rows.
        if (event.shareGroupId != null) return
        val containerId = plantDao.containerOf(event.plantId) ?: return
        val others = plantDao.inContainer(containerId).filter { it.id != event.plantId }
        if (others.isEmpty()) return

        val groupId = newId()
        // The original is stamped too, or the set is one short and deleting
        // from another plant in the pot would orphan it.
        eventDao.upsert(
            event.copy(shareGroupId = groupId)
                .toEntity(createdAt = eventDao.createdAtOf(event.id) ?: now, updatedAt = now),
        )
        for (other in others) {
            eventDao.insert(
                event.copy(id = newId(), plantId = other.id, shareGroupId = groupId)
                    .toEntity(createdAt = now, updatedAt = now),
            )
        }
        TTLog.i(TTLog.DATA) {
            "shared ${event.type} across ${others.size + 1} plants in container $containerId"
        }
    }

    companion object {
        /**
         * The event types that belong to the pot rather than to one plant in
         * it. Deliberately short.
         */
        val SHARED_WITH_CONTAINER = setOf(
            CareEventType.WATERED,
            CareEventType.CHECKED,
            CareEventType.FERTILIZED,
        )
    }
}
