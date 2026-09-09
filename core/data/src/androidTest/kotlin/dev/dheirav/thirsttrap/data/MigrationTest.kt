package dev.dheirav.thirsttrap.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A diary must survive an upgrade.
 *
 * `ThirstTrapDatabase` says every migration should be covered here, and until
 * now none were - the directory existed and was empty. Room verifies an
 * auto-migration against the exported schemas at compile time, which catches a
 * malformed migration but says nothing about whether the rows on someone's
 * phone are still there afterwards. That is what this checks.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private companion object {
        const val TEST_DB = "migration-test.db"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ThirstTrapDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate6To7_keepsExistingRows_andAddsAmbientTable() {
        helper.createDatabase(TEST_DB, 6).use { db ->
            db.execSQL(
                """
                INSERT INTO plants (
                    id, name, source, medium, status, depletion_trigger,
                    dry_anchor_provisional, needs_recalibration, archived,
                    created_at, updated_at
                ) VALUES ('p1', 'Fittonia', 'bought', 'soil', 'active', 0.35, 0, 0, 0, 1000, 1000)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO weight_readings (id, plant_id, timestamp, tz_offset_minutes, grams, context, excluded, created_at)
                VALUES ('w1', 'p1', 2000, 330, 812.5, 'routine', 0, 2000)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true)

        db.query("SELECT name FROM plants WHERE id = 'p1'").use {
            assertTrue("the plant did not survive the migration", it.moveToFirst())
            assertEquals("Fittonia", it.getString(0))
        }
        // The reading matters most: it is the one thing in this app that cannot
        // be reconstructed from memory.
        db.query("SELECT grams FROM weight_readings WHERE id = 'w1'").use {
            assertTrue("the weight reading did not survive the migration", it.moveToFirst())
            assertEquals(812.5, it.getDouble(0), 0.0001)
        }
        db.query("SELECT COUNT(*) FROM ambient_readings").use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun migrate9To10_defaultsExistingPotsToBeingWeighed() {
        helper.createDatabase(TEST_DB, 9).use { db ->
            db.execSQL(
                """
                INSERT INTO plants (
                    id, name, source, medium, status, depletion_trigger,
                    dry_anchor_provisional, needs_recalibration, archived,
                    created_at, updated_at
                ) VALUES ('p1', 'Peperomia', 'bought', 'soil', 'active', 0.5, 0, 0, 0, 1000, 1000)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 10, true)

        // The default has to be "yes", or an upgrade would silently take every
        // existing pot out of the weighing round.
        db.query("SELECT weight_tracked FROM plants WHERE id = 'p1'").use {
            assertTrue("the plant did not survive the migration", it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }
    }

    @Test
    fun migrate10To11_addsTheFertilizerTableAndKeepsTheDiary() {
        helper.createDatabase(TEST_DB, 10).use { db ->
            db.execSQL(
                """
                INSERT INTO plants (
                    id, name, source, medium, status, depletion_trigger,
                    dry_anchor_provisional, needs_recalibration, weight_tracked,
                    archived, created_at, updated_at
                ) VALUES ('p1', 'Fittonia', 'bought', 'soil', 'active', 0.35, 0, 0, 1, 0, 1000, 1000)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 11, true)

        db.query("SELECT name FROM plants WHERE id = 'p1'").use {
            assertTrue("the plant did not survive the migration", it.moveToFirst())
            assertEquals("Fittonia", it.getString(0))
        }
        db.query("SELECT COUNT(*) FROM fertilizers").use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun migrateAll_fromTheOldestSchemaForward() {
        helper.createDatabase(TEST_DB, 1).close()
        // Every auto-migration in sequence, validated against the exported
        // schema at each step by runMigrationsAndValidate.
        helper.runMigrationsAndValidate(TEST_DB, DATABASE_VERSION, true)
    }
}
