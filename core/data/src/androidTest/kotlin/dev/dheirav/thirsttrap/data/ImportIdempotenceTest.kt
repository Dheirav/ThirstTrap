package dev.dheirav.thirsttrap.data

import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.Medium
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.ReadingContext
import dev.dheirav.thirsttrap.domain.Reminder
import dev.dheirav.thirsttrap.domain.ReminderKind
import dev.dheirav.thirsttrap.domain.WeightReading
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * "Importing the same file twice changes nothing" is written on the restore
 * screen, and it was not true. The row counts held, because everything is
 * matched by id, while three columns were rewritten on every import:
 * `created_at` and `updated_at` were stamped with the clock, and the reading
 * context was written in a different case from the one the app itself writes.
 *
 * None of that is visible in the UI, which is exactly why it survived. These
 * tests compare the whole table content rather than the counts.
 */
@RunWith(AndroidJUnit4::class)
class ImportIdempotenceTest {

    private lateinit var db: ThirstTrapDatabase
    private lateinit var repo: ExportRepositoryImpl

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, ThirstTrapDatabase::class.java).build()
        repo = ExportRepositoryImpl(
            context,
            db.plantDao(),
            db.careEventDao(),
            db.photoDao(),
            db.reminderDao(),
            db.weightDao(),
            db.ambientDao(),
            db.fertilizerDao(),
            FertilizerRepositoryImpl(db.fertilizerDao()),
            PhotoStore(context),
        )
    }

    @After
    fun tearDown() = db.close()

    /** Every table, whole rows, so a rewritten column cannot hide behind a count. */
    private fun snapshot(): Map<String, List<String>> {
        val tables = listOf(
            "plants", "care_events", "weight_readings", "photos", "reminders",
            "ambient_readings", "fertilizers",
        )
        return tables.associateWith { table ->
            db.openHelper.readableDatabase.query("SELECT * FROM $table ORDER BY id").use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add((0 until c.columnCount).joinToString("|") { c.getString(it) ?: "null" })
                    }
                }
            }
        }
    }

    private suspend fun seed() {
        db.plantDao().upsert(
            Plant(id = "p1", name = "Peperomia", medium = Medium.SOIL, depletionTrigger = 0.65)
                .toEntity(createdAt = 1_000, updatedAt = 2_000),
        )
        db.careEventDao().upsert(
            CareEvent(
                id = "e1",
                plantId = "p1",
                timestampMillis = 5_000,
                tzOffsetMinutes = 330,
                type = CareEventType.WATERED,
                amountMl = 75.0,
            ).toEntity(createdAt = 1_000, updatedAt = 2_000),
        )
        db.weightDao().upsert(
            WeightReading(
                id = "w1",
                plantId = "p1",
                timestampMillis = 6_000,
                tzOffsetMinutes = 330,
                grams = 276.0,
                context = ReadingContext.POST_WATER,
            ).toReadingEntity(createdAt = 1_000),
        )
        FertilizerRepositoryImpl(db.fertilizerDao()).upsert(
            dev.dheirav.thirsttrap.domain.Fertilizer(
                id = "f1", name = "Seaweed", dilutionText = "1:200", npk = "3-1-2",
            ),
        )
        db.reminderDao().upsert(
            Reminder(
                id = "r1",
                plantId = "p1",
                kind = ReminderKind.CHECK,
                nextDueAtMillis = 9_000,
            ).toReminderEntity(createdAt = 1_000),
        )
    }

    @Test
    fun importingTheSameFileTwiceChangesNothing() = runBlocking {
        seed()
        // The debug file path rather than exportTo, so the test does not need a
        // content provider registered; both funnel into the same writeArchive.
        val file = repo.exportToFileForDebug("test").getOrThrow()

        val before = snapshot()
        repo.importFrom(Uri.fromFile(file)).getOrThrow()
        val afterFirst = snapshot()
        repo.importFrom(Uri.fromFile(file)).getOrThrow()
        val afterSecond = snapshot()

        assertEquals("the first import changed the data it had just exported", before, afterFirst)
        assertEquals("the second import was not a no-op", afterFirst, afterSecond)
    }

    @Test
    fun aRoundTripDoesNotRewriteTheReadingContext() = runBlocking {
        seed()
        val file = repo.exportToFileForDebug("test").getOrThrow()
        repo.importFrom(Uri.fromFile(file)).getOrThrow()

        // The app writes ReadingContext.name; the import mapper used to
        // lowercase it, so a round trip silently changed every stored value
        // into a form that reads back correctly and compares as different.
        db.openHelper.readableDatabase
            .query("SELECT context FROM weight_readings WHERE id = 'w1'").use {
                assertTrue(it.moveToFirst())
                assertEquals(ReadingContext.POST_WATER.name, it.getString(0))
            }
    }

    @Test
    fun importDoesNotRestampWhenARowWasAlreadyHere() = runBlocking {
        seed()
        val file = repo.exportToFileForDebug("test").getOrThrow()
        repo.importFrom(Uri.fromFile(file)).getOrThrow()

        // created_at answers "when did this row enter this database", and the
        // row was already here, so the answer has not changed.
        db.openHelper.readableDatabase
            .query("SELECT created_at, updated_at FROM plants WHERE id = 'p1'").use {
                assertTrue(it.moveToFirst())
                assertEquals(1_000L, it.getLong(0))
                assertEquals(2_000L, it.getLong(1))
            }
    }
}
