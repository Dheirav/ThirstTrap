package dev.dheirav.thirsttrap.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import dev.dheirav.thirsttrap.data.dao.CareEventDao
import dev.dheirav.thirsttrap.data.dao.PlantDao
import dev.dheirav.thirsttrap.data.entity.CareEventEntity
import dev.dheirav.thirsttrap.data.dao.PhotoDao
import dev.dheirav.thirsttrap.data.dao.ReminderDao
import dev.dheirav.thirsttrap.data.entity.PlantEntity
import dev.dheirav.thirsttrap.data.entity.PhotoEntity
import dev.dheirav.thirsttrap.data.entity.ReminderEntity

/**
 * Version 1. `exportSchema` is on and `schemas/` is committed, because that JSON
 * is the only way to test that a future migration preserves real data.
 *
 * `fallbackToDestructiveMigration` must never appear in a release build - see
 * docs/DATA-MODEL.md. This is a diary; wiping it on upgrade is the worst bug
 * the project could ship.
 */
@Database(
    entities = [PlantEntity::class, CareEventEntity::class, ReminderEntity::class, PhotoEntity::class],
    version = 4,
    exportSchema = true,
    // v2 only adds the reminders table, so Room can generate the migration.
    // Anything that alters or drops a column must be written by hand and
    // covered by a MigrationTestHelper test - a diary must survive upgrades.
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
    ],
)
abstract class ThirstTrapDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun careEventDao(): CareEventDao
    abstract fun reminderDao(): ReminderDao
    abstract fun photoDao(): PhotoDao

    companion object {
        const val NAME = "thirsttrap.db"
    }
}
