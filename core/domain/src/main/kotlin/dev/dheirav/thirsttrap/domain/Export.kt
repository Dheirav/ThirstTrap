package dev.dheirav.thirsttrap.domain

import kotlinx.serialization.Serializable

/**
 * The export format.
 *
 * Deliberately the **domain** entities, not the Room rows: a backup should
 * survive a change to how the app happens to store things today. The manifest
 * carries a version so a future importer can tell what it is reading.
 *
 * Backup and portability are called non-negotiable in the requirements, with
 * the Vera shutdown as the cautionary tale - an app that strands its users'
 * data. An export nobody can re-import is an archive, not a backup, which is
 * why the import side ships alongside.
 */
@Serializable
data class ExportBundle(
    val plants: List<Plant> = emptyList(),
    val events: List<CareEvent> = emptyList(),
    val photos: List<Photo> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
)

@Serializable
data class ExportManifest(
    /** The bundle format, not the database schema. Bumped on a breaking change. */
    val formatVersion: Int = CURRENT_EXPORT_FORMAT,
    val databaseVersion: Int,
    val appVersionName: String,
    val exportedAtMillis: Long,
    val tzOffsetMinutes: Int,
    val counts: Map<String, Int> = emptyMap(),
) {
    companion object {
        const val FILE = "manifest.json"
    }
}

const val CURRENT_EXPORT_FORMAT = 1
const val EXPORT_DATA_FILE = "thirsttrap.json"
const val EXPORT_PHOTO_DIR = "photos"

/** What an import did, so the UI can say something specific rather than "done". */
data class ImportResult(
    val plants: Int = 0,
    val events: Int = 0,
    val photos: Int = 0,
    val reminders: Int = 0,
    val photoFiles: Int = 0,
    val skippedPhotoFiles: Int = 0,
    val warnings: List<String> = emptyList(),
)
