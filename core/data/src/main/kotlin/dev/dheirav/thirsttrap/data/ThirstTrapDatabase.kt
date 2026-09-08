package dev.dheirav.thirsttrap.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import dev.dheirav.thirsttrap.data.dao.CareEventDao
import dev.dheirav.thirsttrap.data.dao.AmbientDao
import dev.dheirav.thirsttrap.data.dao.LocationDao
import dev.dheirav.thirsttrap.data.dao.PlantDao
import dev.dheirav.thirsttrap.data.entity.CareEventEntity
import dev.dheirav.thirsttrap.data.dao.PhotoDao
import dev.dheirav.thirsttrap.data.dao.WeightDao
import dev.dheirav.thirsttrap.data.dao.ReminderDao
import dev.dheirav.thirsttrap.data.entity.AmbientReadingEntity
import dev.dheirav.thirsttrap.data.entity.LocationNoteEntity
import dev.dheirav.thirsttrap.data.entity.PlantEntity
import dev.dheirav.thirsttrap.data.entity.PhotoEntity
import dev.dheirav.thirsttrap.data.entity.WeightReadingEntity
import dev.dheirav.thirsttrap.data.entity.ReminderEntity

/**
 * Version 1. `exportSchema` is on and `schemas/` is committed, because that JSON
 * is the only way to test that a future migration preserves real data.
 *
 * `fallbackToDestructiveMigration` must never appear in a release build - see
 * docs/DATA-MODEL.md. This is a diary; wiping it on upgrade is the worst bug
 * the project could ship.
 */
/**
 * The one place the schema version is written down.
 *
 * It used to be duplicated as a private constant in ExportRepositoryImpl, which
 * stopped at 4 while the database went to 7 - so every backup written since has
 * recorded the wrong schema version, and the importer's "written by a newer
 * version of the app" warning has been comparing against a stale number.
 */
const val DATABASE_VERSION = 9

@Database(
    entities = [PlantEntity::class, CareEventEntity::class, ReminderEntity::class, PhotoEntity::class, WeightReadingEntity::class, AmbientReadingEntity::class, LocationNoteEntity::class],
    version = DATABASE_VERSION,
    exportSchema = true,
    // v2 only adds the reminders table, so Room can generate the migration.
    // Anything that alters or drops a column must be written by hand and
    // covered by a MigrationTestHelper test - a diary must survive upgrades.
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        // v7 only adds the ambient_readings table, so Room can generate it.
        AutoMigration(from = 6, to = 7),
        // v8 adds a nullable column to care_events; Room can generate it.
        AutoMigration(from = 7, to = 8),
        // v9 only adds the location_notes table.
        AutoMigration(from = 8, to = 9),
    ],
)
abstract class ThirstTrapDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun careEventDao(): CareEventDao
    abstract fun reminderDao(): ReminderDao
    abstract fun photoDao(): PhotoDao
    abstract fun weightDao(): WeightDao
    abstract fun ambientDao(): AmbientDao
    abstract fun locationDao(): LocationDao

    companion object {
        const val NAME = "thirsttrap.db"
    }
}
