package dev.dheirav.thirsttrap.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.MILLIS_PER_DAY
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.ReadingContext
import dev.dheirav.thirsttrap.domain.WeightReading
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The weigh-in has to carry the watering, even when nobody touched the chip.
 *
 * The backfill existed and could not fire in the case it was written for. It
 * triggered only on a POST_WATER reading, and the chip is only ever suggested
 * as POST_WATER when a watering is already on the log, which is the one case
 * where there is nothing to backfill. Someone who waters and then weighs
 * without logging got ROUTINE and lost the watering.
 *
 * The numbers below are a real diary, 20 October 2026. A fig rose 223 g to
 * 271 g on a range of about 73 g, was stored as ROUTINE, and the dashboard
 * went on saying "watered 7 days ago" while the drying model had already
 * segmented the same reading as a fresh cycle.
 */
@RunWith(AndroidJUnit4::class)
class WeightBackfillTest {

    private lateinit var db: ThirstTrapDatabase
    private lateinit var weights: WeightRepositoryImpl

    private val plantId = "fig"
    private val day = MILLIS_PER_DAY.toLong()
    private val t0 = 100L * day

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ThirstTrapDatabase::class.java,
        ).build()
        weights = WeightRepositoryImpl(
            db.weightDao(), db.plantDao(), db.careEventDao(),
            ContainerSharing(db.plantDao(), db.careEventDao()),
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun weigh(at: Long, grams: Double, context: ReadingContext) =
        db.weightDao().upsert(
            WeightReading(
                id = "w$at$grams", plantId = plantId, timestampMillis = at,
                tzOffsetMinutes = 330, grams = grams, context = context,
            ).toReadingEntity(at),
        )

    private suspend fun water(at: Long) = db.careEventDao().insert(
        CareEvent(
            id = "e$at", plantId = plantId, timestampMillis = at,
            tzOffsetMinutes = 330, type = CareEventType.WATERED,
        ).toEntity(at, at),
    )

    /**
     * A week of real shape: watered and weighed on day one, so the pot has a
     * wet and a dry end, then left to dry. Range comes out near 73 g.
     */
    private suspend fun seedHistory() {
        db.plantDao().upsert(
            Plant(id = plantId, name = "Creeping fig", depletionTrigger = 0.5)
                .toEntity(createdAt = t0, updatedAt = t0),
        )
        water(t0 - 7 * day)
        weigh(t0 - 7 * day - 600_000L, 202.0, ReadingContext.PRE_WATER)
        weigh(t0 - 7 * day, 275.0, ReadingContext.POST_WATER)
        weigh(t0 - 5 * day, 257.0, ReadingContext.ROUTINE)
        weigh(t0 - 3 * day, 235.0, ReadingContext.ROUTINE)
        weigh(t0 - 1 * day, 228.0, ReadingContext.ROUTINE)
        weigh(t0 - 60 * 60 * 1000L, 223.0, ReadingContext.ROUTINE)
    }

    private suspend fun waterings() = db.careEventDao().observeForPlant(plantId).first()
        .filter { it.type == CareEventType.WATERED.name }

    @Test
    fun aPotThatGainedWeightGetsItsWateringBack() = runBlocking {
        seedHistory()
        assertEquals("only the seeded watering to start", 1, waterings().size)

        // Watered in the real world, weighed, chip left on ROUTINE.
        val backfilled = weights.addReading(plantId, 271.0, ReadingContext.ROUTINE)

        assertTrue("addReading should report that it inferred one", backfilled)
        val all = waterings()
        assertEquals("the inferred watering", 2, all.size)
        val inferred = all.maxByOrNull { it.timestamp }!!
        assertTrue(
            "it should say which rule inferred it, got ${inferred.note}",
            inferred.note.orEmpty().contains("gained weight"),
        )
        assertTrue(
            "it has to sort before the reading or segmentation splits the wrong side",
            inferred.timestamp < System.currentTimeMillis(),
        )
    }

    @Test
    fun ordinaryDryingInventsNothing() = runBlocking {
        seedHistory()
        // Lighter than last time, which is every ordinary day.
        val backfilled = weights.addReading(plantId, 219.0, ReadingContext.ROUTINE)
        assertTrue("no watering should be invented", !backfilled)
        assertEquals(1, waterings().size)
    }

    @Test
    fun aSmallRiseInventsNothing() = runBlocking {
        seedHistory()
        // Up 4 g on a 73 g range: under the 8 percent segmentation threshold,
        // so it is scale drift or a wet saucer, not a drink.
        val backfilled = weights.addReading(plantId, 227.0, ReadingContext.ROUTINE)
        assertTrue("4 g is not a watering", !backfilled)
        assertEquals(1, waterings().size)
    }

    @Test
    fun aWateringAlreadyLoggedIsNotDoubled() = runBlocking {
        seedHistory()
        // Logged the watering properly, then weighed. The event exists.
        water(System.currentTimeMillis() - 300_000L)
        val before = waterings().size

        val backfilled = weights.addReading(plantId, 271.0, ReadingContext.ROUTINE)

        assertTrue("the log already explains the rise", !backfilled)
        assertEquals(before, waterings().size)
    }

    @Test
    fun anUncalibratedPotSaysNothing() = runBlocking {
        // No anchors can be derived from a single reading, so there is no range
        // to measure a rise against, and 50 g means nothing on its own.
        db.plantDao().upsert(
            Plant(id = plantId, name = "New").toEntity(createdAt = t0, updatedAt = t0),
        )
        weigh(t0 - day, 223.0, ReadingContext.ROUTINE)

        val backfilled = weights.addReading(plantId, 271.0, ReadingContext.ROUTINE)

        assertTrue("nothing to compare against", !backfilled)
        assertEquals(0, waterings().size)
    }
}
