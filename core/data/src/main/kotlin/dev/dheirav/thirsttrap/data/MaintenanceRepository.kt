package dev.dheirav.thirsttrap.data

import dev.dheirav.thirsttrap.data.dao.PhotoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class StorageReport(
    val photoCount: Int,
    val photoBytes: Long,
    val databaseBytes: Long,
    /** Files on disk with no row pointing at them. */
    val orphanFiles: Int,
    val orphanFileBytes: Long,
    /** Rows whose file has gone missing. */
    val orphanRows: Int,
)

/**
 * The two ways photo storage drifts out of step, both named in
 * docs/DATA-MODEL.md:
 *
 *  - **Orphan files** left by a hard delete or an interrupted capture. SQLite's
 *    cascade cannot touch the filesystem, so nothing else removes them.
 *  - **Orphan rows** whose file has vanished - a restore gone wrong, or storage
 *    cleared. These must render as a placeholder, never crash a grid.
 */
@Singleton
class MaintenanceRepository @Inject constructor(
    private val photoDao: PhotoDao,
    private val store: PhotoStore,
    private val database: ThirstTrapDatabase,
    private val context: android.content.Context,
) {

    suspend fun report(): StorageReport = withContext(Dispatchers.IO) {
        val rows = photoDao.all()
        val known = rows.map { it.relativePath }.toSet()

        var liveBytes = 0L
        var missingRows = 0
        for (row in rows) {
            val f = store.absoluteFile(row.relativePath)
            if (f.exists()) liveBytes += f.length() else missingRows++
        }

        val orphans = store.orphanFiles(known)
        val dbFile = context.getDatabasePath(ThirstTrapDatabase.NAME)

        StorageReport(
            photoCount = rows.size,
            photoBytes = liveBytes,
            databaseBytes = if (dbFile.exists()) dbFile.length() else 0L,
            orphanFiles = orphans.size,
            orphanFileBytes = orphans.sumOf { it.length() },
            orphanRows = missingRows,
        )
    }

    /**
     * Deletes files nothing references, and rows whose file is gone.
     *
     * @return how many of each were removed.
     */
    suspend fun cleanUp(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val rows = photoDao.all()
        val known = rows.map { it.relativePath }.toSet()

        val orphanFiles = store.orphanFiles(known)
        orphanFiles.forEach { runCatching { it.delete() } }

        var removedRows = 0
        for (row in rows) {
            if (!store.absoluteFile(row.relativePath).exists()) {
                photoDao.delete(row.id)
                removedRows++
            }
        }
        // Camera scratch files older than a day are certainly finished with.
        store.clearStaleCaptures(olderThanMillis = 86_400_000L)

        TTLog.i(TTLog.DATA) { "cleanup: ${orphanFiles.size} files, $removedRows rows" }
        orphanFiles.size to removedRows
    }
}
