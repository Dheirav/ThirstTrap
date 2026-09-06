package dev.dheirav.thirsttrap.data

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.dheirav.thirsttrap.data.dao.CareEventDao
import dev.dheirav.thirsttrap.data.dao.PlantDao
import dev.dheirav.thirsttrap.data.entity.CareEventEntity
import dev.dheirav.thirsttrap.data.entity.PlantEntity

/**
 * Version 1. `exportSchema` is on and `schemas/` is committed, because that JSON
 * is the only way to test that a future migration preserves real data.
 *
 * `fallbackToDestructiveMigration` must never appear in a release build - see
 * docs/DATA-MODEL.md. This is a diary; wiping it on upgrade is the worst bug
 * the project could ship.
 */
@Database(
    entities = [PlantEntity::class, CareEventEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ThirstTrapDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun careEventDao(): CareEventDao

    companion object {
        const val NAME = "thirsttrap.db"
    }
}
