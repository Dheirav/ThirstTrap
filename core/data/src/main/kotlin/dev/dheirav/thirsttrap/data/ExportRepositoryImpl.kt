package dev.dheirav.thirsttrap.data

import android.content.Context
import android.net.Uri
import dev.dheirav.thirsttrap.data.dao.CareEventDao
import dev.dheirav.thirsttrap.data.dao.PhotoDao
import dev.dheirav.thirsttrap.data.dao.WeightDao
import dev.dheirav.thirsttrap.data.dao.AmbientDao
import dev.dheirav.thirsttrap.data.dao.PlantDao
import dev.dheirav.thirsttrap.data.dao.ReminderDao
import dev.dheirav.thirsttrap.domain.CURRENT_EXPORT_FORMAT
import dev.dheirav.thirsttrap.domain.EXPORT_DATA_FILE
import dev.dheirav.thirsttrap.domain.EXPORT_PHOTO_DIR
import dev.dheirav.thirsttrap.domain.ExportBundle
import dev.dheirav.thirsttrap.domain.ExportManifest
import dev.dheirav.thirsttrap.domain.ImportResult
import dev.dheirav.thirsttrap.domain.tzOffsetMinutesAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ExportRepositoryImpl @Inject constructor(
    private val context: Context,
    private val plantDao: PlantDao,
    private val eventDao: CareEventDao,
    private val photoDao: PhotoDao,
    private val reminderDao: ReminderDao,
    private val weightDao: WeightDao,
    private val ambientDao: AmbientDao,
    private val fertilizerDao: dev.dheirav.thirsttrap.data.dao.FertilizerDao,
    private val fertilizers: dev.dheirav.thirsttrap.domain.FertilizerRepository,
    private val store: PhotoStore,
) {

    private val databaseVersion get() = DATABASE_VERSION

    private val json = Json {
        prettyPrint = true
        // An older build must be able to read a newer export rather than
        // refusing it outright.
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun suggestedFileName(): String {
        val date = java.time.LocalDate.now().toString()
        return "thirsttrap-export-$date.zip"
    }

    /**
     * Streams the whole archive to [destination].
     *
     * Streamed, never assembled in memory: the requirements budget close to a
     * gigabyte of photos for three years, and building that as a byte array
     * would fail exactly when someone has the most to lose.
     */
    suspend fun exportTo(destination: Uri, appVersionName: String): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val out = context.contentResolver.openOutputStream(destination)
                    ?: error("could not open $destination for writing")
                out.use { writeArchive(it, appVersionName) }
            }.onFailure { TTLog.e(TTLog.DATA, { "export failed" }, it) }
        }

    /**
     * Debug-only: writes an export to a fixed file with no picker involved, so
     * the archive can be inspected without driving the SAF UI.
     */
    suspend fun exportToFileForDebug(appVersionName: String): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val target = File(context.cacheDir, "debug-export.zip")
                target.outputStream().use { writeArchive(it, appVersionName) }
                target
            }.onFailure { TTLog.e(TTLog.DATA, { "debug export failed" }, it) }
        }

    /** @return how many photo files made it into the archive. */
    private suspend fun writeArchive(out: java.io.OutputStream, appVersionName: String): Int {
        val plants = plantDao.observePlants(includeArchived = true).first().map { it.toDomain() }
        val events = eventDao.observeAll().first().map { it.toDomain() }
        val photos = photoDao.all().map { it.toDomain() }
        val reminders = reminderDao.observeAll().first().map { it.toDomainReminder() }

        val now = System.currentTimeMillis()
        val weightReadings = weightDao.all().map { it.toDomainReading() }
        val ambient = ambientDao.all().map { it.toDomain() }
        val fertilizers = fertilizerDao.all().map { it.toDomain() }
        val bundle = ExportBundle(
            plants, events, photos, reminders, weightReadings, ambient, fertilizers,
        )
        val manifest = ExportManifest(
            formatVersion = CURRENT_EXPORT_FORMAT,
            databaseVersion = DATABASE_VERSION,
            appVersionName = appVersionName,
            exportedAtMillis = now,
            tzOffsetMinutes = tzOffsetMinutesAt(now),
            // Everything the archive holds, not a subset. The manifest is what
            // you read to decide whether a backup is complete, and it was
            // silently omitting the weight readings and the room log - the two
            // things added most recently and the least reconstructable.
            counts = mapOf(
                "plants" to plants.size,
                "events" to events.size,
                "photos" to photos.size,
                "reminders" to reminders.size,
                "weightReadings" to weightReadings.size,
                "ambient" to ambient.size,
                "fertilizers" to fertilizers.size,
            ),
        )

        var written = 0
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(ExportManifest.FILE))
            zip.write(json.encodeToString(ExportManifest.serializer(), manifest).toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(EXPORT_DATA_FILE))
            zip.write(json.encodeToString(ExportBundle.serializer(), bundle).toByteArray())
            zip.closeEntry()

            for (photo in photos) {
                val file = store.absoluteFile(photo.relativePath)
                if (!file.exists()) {
                    TTLog.w(TTLog.DATA, { "export: missing file ${photo.relativePath}" })
                    continue
                }
                // The same relative path the database holds, so an import is a
                // straight copy plus a row.
                zip.putNextEntry(ZipEntry(photo.relativePath))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                written++
            }
        }
        TTLog.i(TTLog.DATA) {
            "export: ${plants.size} plants, ${events.size} events, $written photos"
        }
        return written
    }

    /** Debug-only counterpart to [exportToFileForDebug]. */
    suspend fun importFromCacheForDebug(): Result<ImportResult> = withContext(Dispatchers.IO) {
        val f = File(context.cacheDir, "debug-export.zip")
        if (!f.exists()) return@withContext Result.failure(IllegalStateException("no debug-export.zip"))
        importFrom(Uri.fromFile(f))
    }

    /**
     * Reads an archive back.
     *
     * **Idempotent**: every row is upserted by its UUID, so importing the same
     * file twice changes nothing. That is what the text primary keys were for.
     */
    suspend fun importFrom(source: Uri): Result<ImportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val warnings = mutableListOf<String>()
            var bundle: ExportBundle? = null
            var manifest: ExportManifest? = null
            var photoFiles = 0
            var skipped = 0

            // A file:// URI comes from the debug path; everything else goes
            // through the resolver as a normal SAF document.
            val input = if (source.scheme == "file") {
                File(requireNotNull(source.path)).inputStream()
            } else {
                context.contentResolver.openInputStream(source)
                    ?: error("could not open $source for reading")
            }

            ZipInputStream(input.buffered()).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == ExportManifest.FILE ->
                            manifest = json.decodeFromString(
                                ExportManifest.serializer(), zip.readBytes().decodeToString(),
                            )

                        name == EXPORT_DATA_FILE ->
                            bundle = json.decodeFromString(
                                ExportBundle.serializer(), zip.readBytes().decodeToString(),
                            )

                        name.startsWith("$EXPORT_PHOTO_DIR/") && !entry.isDirectory -> {
                            // Refuse anything that climbs out of the photo
                            // directory. A zip is untrusted input even when the
                            // user believes they wrote it themselves.
                            val target = File(context.filesDir, name).canonicalFile
                            val root = File(context.filesDir, EXPORT_PHOTO_DIR).canonicalFile
                            if (!target.path.startsWith(root.path + File.separator)) {
                                warnings += "skipped suspicious path: $name"
                                skipped++
                            } else {
                                target.parentFile?.mkdirs()
                                target.outputStream().use { zip.copyTo(it) }
                                photoFiles++
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val data = bundle ?: error("no $EXPORT_DATA_FILE in the archive")
            manifest?.let {
                if (it.formatVersion > CURRENT_EXPORT_FORMAT) {
                    warnings += "This backup was written by a newer version of the app " +
                        "(format ${it.formatVersion}). Anything it does not recognise was skipped."
                }
            }

            val now = System.currentTimeMillis()
            // created_at means "when this row entered this database", so a row
            // that is already here keeps the answer it already had. Stamping
            // "now" unconditionally made a re-import rewrite the date every
            // plant was added, which contradicts what the screen promises:
            // importing the same file twice should change nothing.
            //
            // updated_at is kept for the same reason and one more: the backup
            // does not carry it, so there is nothing in the file that says the
            // row changed. Writing "now" would be inventing a modification.
            data.plants.forEach {
                plantDao.upsert(
                    it.toEntity(
                        createdAt = plantDao.createdAtOf(it.id) ?: now,
                        updatedAt = plantDao.updatedAtOf(it.id) ?: now,
                    ),
                )
            }
            data.events.forEach {
                eventDao.upsert(
                    it.toEntity(
                        createdAt = eventDao.createdAtOf(it.id) ?: now,
                        updatedAt = eventDao.updatedAtOf(it.id) ?: now,
                    ),
                )
            }
            data.reminders.forEach {
                reminderDao.upsert(it.toReminderEntity(reminderDao.createdAtOf(it.id) ?: now))
            }
            data.photos.forEach {
                photoDao.upsert(it.toPhotoEntity(photoDao.createdAtOf(it.id) ?: now))
            }
            data.weightReadings.forEach {
                weightDao.upsert(it.toReadingEntity(weightDao.createdAtOf(it.id) ?: now))
            }
            data.ambient.forEach { ambientDao.upsert(it.toEntity()) }
            data.fertilizers.forEach { fertilizers.upsert(it) }

            val result = ImportResult(
                plants = data.plants.size,
                events = data.events.size,
                photos = data.photos.size,
                reminders = data.reminders.size,
                photoFiles = photoFiles,
                skippedPhotoFiles = skipped,
                warnings = warnings,
            )
            TTLog.i(TTLog.DATA) { "import: $result" }
            result
        }.onFailure { TTLog.e(TTLog.DATA, { "import failed" }, it) }
    }
}
