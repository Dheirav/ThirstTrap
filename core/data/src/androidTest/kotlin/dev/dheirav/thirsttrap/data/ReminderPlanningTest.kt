package dev.dheirav.thirsttrap.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.dheirav.thirsttrap.domain.Anchors
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.MILLIS_PER_DAY
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.ReadingContext
import dev.dheirav.thirsttrap.domain.Reminder
import dev.dheirav.thirsttrap.domain.ReminderKind
import dev.dheirav.thirsttrap.domain.WeightReading
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The reminder has to follow the pot, not the calendar.
 *
 * resolveIntervalDays documented that priority order from the start and was
 * called from four places, three of which passed nulls for the prediction and
 * the logged average. The result was a flat weekly reminder everywhere except
 * the weight screen, which is the opposite of what this app is for. These tests
 * pin the behaviour to the repository, where the inputs are now gathered, so a
 * future caller cannot quietly reintroduce it.
 */
@RunWith(AndroidJUnit4::class)
class ReminderPlanningTest {

    private lateinit var db: ThirstTrapDatabase
    private lateinit var reminders: ReminderRepositoryImpl

    private val now = 100L * MILLIS_PER_DAY.toLong()
    private val plantId = "p1"

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ThirstTrapDatabase::class.java,
        ).build()
        reminders = ReminderRepositoryImpl(
            db.reminderDao(), db.plantDao(), db.careEventDao(), db.weightDao(),
        )
    }

    @After
    fun tearDown() = db.close()

    private fun daysFromNow(due: Long) = ((due - now) / MILLIS_PER_DAY).roundToInt()

    private suspend fun seedPlant(plant: Plant) =
        db.plantDao().upsert(plant.toEntity(createdAt = now, updatedAt = now))

    private suspend fun seedReminder(intervalDays: Int? = null) =
        db.reminderDao().upsert(
            Reminder(
                id = "r1", plantId = plantId, kind = ReminderKind.CHECK,
                intervalDays = intervalDays, nextDueAtMillis = now,
            ).toReminderEntity(now),
        )

    private suspend fun water(dayOffset: Double) = db.careEventDao().insert(
        CareEvent(
            id = "e${dayOffset}", plantId = plantId,
            timestampMillis = now + (dayOffset * MILLIS_PER_DAY).toLong(),
            tzOffsetMinutes = 330, type = CareEventType.WATERED,
        ).toEntity(now, now),
    )

    private suspend fun weigh(dayOffset: Double, grams: Double, context: ReadingContext) =
        db.weightDao().upsert(
            WeightReading(
                id = "w$dayOffset", plantId = plantId,
                timestampMillis = now + (dayOffset * MILLIS_PER_DAY).toLong(),
                tzOffsetMinutes = 330, grams = grams, context = context,
            ).toReadingEntity(now),
        )

    private suspend fun due(): Long =
        db.reminderDao().observeForPlant(plantId).first().first().nextDueAt

    @Test
    fun withNothingKnown_itFallsBackToAWeek() = runBlocking {
        seedPlant(Plant(id = plantId, name = "New"))
        seedReminder()
        reminders.rescheduleFromModel(plantId, now)
        assertEquals(7, daysFromNow(due()))
    }

    @Test
    fun theLoggedAverageIsUsedWhenThereIsNoPrediction() = runBlocking {
        // This branch had never run outside its unit tests: no caller ever
        // passed a logged average.
        seedPlant(Plant(id = plantId, name = "Ivy"))
        seedReminder()
        water(-12.0); water(-8.0); water(-4.0)   // every four days
        reminders.rescheduleFromModel(plantId, now)
        // Counted from the last watering, four days ago, so it is due today.
        assertEquals(4, ((due() - (now - 4 * MILLIS_PER_DAY.toLong())) / MILLIS_PER_DAY).roundToInt())
    }

    @Test
    fun thePredictionBeatsTheLoggedAverage() = runBlocking {
        // A pot drying faster than the calendar suggests is the entire premise:
        // the log says water every 10 days, the pot says it will be dry sooner.
        seedPlant(
            Plant(
                id = plantId, name = "Fig",
                anchors = Anchors(wetGrams = 1000.0, dryGrams = 600.0, dryIsProvisional = false),
                depletionTrigger = 0.5,
            ),
        )
        seedReminder()
        // Readings have to be recent, or the pot is already past its trigger by
        // now and the prediction is WaterNow rather than an Eta - which
        // resolveIntervalDays deliberately does not treat as an interval.
        water(-23.0); water(-13.0); water(-3.0)  // logged interval: 10 days
        weigh(-3.0, 1000.0, ReadingContext.POST_WATER)
        weigh(-2.0, 960.0, ReadingContext.ROUTINE)
        weigh(-1.0, 920.0, ReadingContext.ROUTINE)
        weigh(0.0, 880.0, ReadingContext.ROUTINE)    // 40 g a day, trigger at 800

        reminders.rescheduleFromModel(plantId, now)
        val interval = ((due() - (now - 3 * MILLIS_PER_DAY.toLong())) / MILLIS_PER_DAY).roundToInt()
        assertTrue(
            "expected the prediction to be used, not the 10 day average; got $interval",
            interval < 10,
        )
    }

    @Test
    fun anExplicitSettingBeatsEverything() = runBlocking {
        seedPlant(Plant(id = plantId, name = "Set"))
        seedReminder(intervalDays = 3)
        water(-20.0); water(-10.0)
        reminders.rescheduleFromModel(plantId, now)
        assertEquals(3, ((due() - (now - 10 * MILLIS_PER_DAY.toLong())) / MILLIS_PER_DAY).roundToInt())
    }

    @Test
    fun aCheckResetsTheClockJustAsAWateringDoes() = runBlocking {
        // Being careful must not be punished with a nag.
        seedPlant(Plant(id = plantId, name = "Careful"))
        seedReminder()
        water(-6.0)
        db.careEventDao().insert(
            CareEvent(
                id = "check", plantId = plantId,
                timestampMillis = now - MILLIS_PER_DAY.toLong(),
                tzOffsetMinutes = 330, type = CareEventType.CHECKED,
            ).toEntity(now, now),
        )
        reminders.rescheduleFromModel(plantId, now)
        // Measured from the check yesterday, not the watering six days ago.
        assertEquals(6, daysFromNow(due()))
    }

    @Test
    fun aPlantWithNoReminderRowIsLeftAlone() = runBlocking {
        seedPlant(Plant(id = plantId, name = "None"))
        reminders.rescheduleFromModel(plantId, now)
        assertTrue(db.reminderDao().observeForPlant(plantId).first().isEmpty())
    }

    @Test
    fun aTopUpPairIsNotARhythm() = runBlocking {
        // The Fittonia case, off the real database: watered, then watered again
        // a day later. The plain average calls that a one day cycle and the
        // plant falls due the next morning.
        seedPlant(Plant(id = plantId, name = "Fittonia"))
        seedReminder()
        water(-2.32); water(-1.0)

        reminders.rescheduleFromModel(plantId, now)

        assertEquals(
            "two waterings a day apart should fall back to the default week",
            7,
            ((due() - (now - MILLIS_PER_DAY.toLong())) / MILLIS_PER_DAY).roundToInt(),
        )
    }
}
