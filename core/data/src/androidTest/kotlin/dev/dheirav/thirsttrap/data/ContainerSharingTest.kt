package dev.dheirav.thirsttrap.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.Plant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Two plants in one jar.
 *
 * The thing being pinned is not "the row gets copied" but which rows do. A pot
 * shares its water, not its history: copying a repot or a milestone would fill
 * a cutting's timeline with a month of its parent's life it was never part of,
 * and that is the failure this feature could most easily cause.
 */
@RunWith(AndroidJUnit4::class)
class ContainerSharingTest {

    private lateinit var db: ThirstTrapDatabase
    private lateinit var sharing: ContainerSharing

    private val jar = "jar-1"

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ThirstTrapDatabase::class.java,
        ).build()
        sharing = ContainerSharing(db.plantDao(), db.careEventDao())
    }

    @After
    fun tearDown() = db.close()

    private fun plant(id: String, container: String?) =
        Plant(id = id, name = id, containerId = container)

    private suspend fun put(vararg plants: Plant) =
        plants.forEach { db.plantDao().upsert(it.toEntity(createdAt = 1, updatedAt = 1)) }

    private suspend fun log(plantId: String, type: CareEventType, id: String = "e-$plantId"): CareEvent {
        val e = CareEvent(
            id = id, plantId = plantId, timestampMillis = 5000, tzOffsetMinutes = 330, type = type,
        )
        db.careEventDao().insert(e.toEntity(createdAt = 5000, updatedAt = 5000))
        sharing.fanOut(e, now = 5000)
        return e
    }

    private suspend fun eventsOf(plantId: String) =
        db.careEventDao().observeForPlant(plantId).first()

    @Test
    fun wateringOnePlantRecordsItOnEveryPlantInTheJar() = runBlocking {
        put(plant("a", jar), plant("b", jar))

        log("a", CareEventType.WATERED)

        val b = eventsOf("b")
        assertEquals("the other plant in the jar was not watered", 1, b.size)
        assertEquals(CareEventType.WATERED.name, b[0].type)
        // Same moment, or the two timelines disagree about when the pot was
        // watered and the reminders drift apart again.
        assertEquals(5000L, b[0].timestamp)
        assertNotNull("a copy must carry the group id", b[0].shareGroupId)
        assertEquals(
            "the original must join the group it created",
            b[0].shareGroupId,
            eventsOf("a").single().shareGroupId,
        )
    }

    @Test
    fun aPlantInItsOwnPotIsUntouched() = runBlocking {
        put(plant("a", null), plant("b", null))

        log("a", CareEventType.WATERED)

        assertTrue("a plant in its own pot must not share", eventsOf("b").isEmpty())
        assertNull(
            "an unshared event must not look like a copy",
            eventsOf("a").single().shareGroupId,
        )
    }

    @Test
    fun onlyWhatBelongsToThePotIsShared() = runBlocking {
        put(plant("a", jar), plant("b", jar))

        // The three that are true of the pot.
        log("a", CareEventType.WATERED, id = "w")
        log("a", CareEventType.CHECKED, id = "c")
        log("a", CareEventType.FERTILIZED, id = "f")
        // The ones that are true of one plant. A cutting did not get repotted
        // because its parent did, and it certainly did not reach a milestone.
        log("a", CareEventType.REPOTTED, id = "r")
        log("a", CareEventType.MILESTONE, id = "m")
        log("a", CareEventType.OBSERVATION, id = "o")
        log("a", CareEventType.PRUNED, id = "p")
        log("a", CareEventType.DIED, id = "d")

        val shared = eventsOf("b").map { it.type }.toSet()
        assertEquals(
            setOf(
                CareEventType.WATERED.name,
                CareEventType.CHECKED.name,
                CareEventType.FERTILIZED.name,
            ),
            shared,
        )
    }

    @Test
    fun correctingASharedWateringCorrectsEveryCopy() = runBlocking {
        put(plant("a", jar), plant("b", jar))
        log("a", CareEventType.WATERED)

        val repo = repository()
        val mine = eventsOf("a").single().toDomain()
        repo.updateEvent(mine.copy(amountMl = 25.0, note = "actually 25"))

        assertEquals(
            "the copy still claims the old amount",
            25.0,
            eventsOf("b").single().amountMl!!,
            1e-9,
        )
        assertEquals("actually 25", eventsOf("b").single().note)
        // Identity must not move: the copy is still the other plant's row.
        assertEquals("b", eventsOf("b").single().plantId)
    }

    @Test
    fun deletingASharedWateringRemovesEveryCopy() = runBlocking {
        put(plant("a", jar), plant("b", jar))
        log("a", CareEventType.WATERED)

        repository().deleteEvent(eventsOf("a").single().id)

        assertTrue("the original survived", eventsOf("a").isEmpty())
        assertTrue("the copy was orphaned", eventsOf("b").isEmpty())
    }

    @Test
    fun aPlantTakenOutOfTheJarKeepsItsHistoryAndStopsReceiving() = runBlocking {
        put(plant("a", jar), plant("b", jar))
        log("a", CareEventType.WATERED, id = "while-shared")

        // Scooped out for the gift.
        put(plant("b", null))

        log("a", CareEventType.WATERED, id = "after-leaving")

        // It keeps the watering it was given while it shared the jar, and gets
        // nothing from the one after. That is the whole point of rule 3: the
        // cutting leaves with a real history rather than a blank page.
        val b = eventsOf("b")
        assertEquals("it should keep what it was given while it shared", 1, b.size)
        assertEquals("b", b.single().plantId)
        assertNotNull("the kept row is still a shared copy", b.single().shareGroupId)
        assertEquals("the parent should have both of its own", 2, eventsOf("a").size)
    }

    private fun repository() = PlantRepositoryImpl(
        db.plantDao(), db.careEventDao(), db.reminderDao(), db.photoDao(),
        PhotoStore(InstrumentationRegistry.getInstrumentation().targetContext),
        db.weightDao(), sharing,
    )
}
