package com.fieldnotes.app.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fieldnotes.app.FieldNotesApp
import com.fieldnotes.app.data.export.NoteExporter
import com.fieldnotes.app.data.repo.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

object AutoBackup {

    const val KEEP_COUNT = 4
    private const val FILE_PREFIX = "field-notes-auto-"

    fun backupDir(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, "backups")
    }

    /**
     * Writes a full ZIP backup (same format as manual export) and prunes old
     * auto-backups, keeping the newest [AutoBackup.KEEP_COUNT]. Returns the
     * written file, or null on failure.
     */
    suspend fun run(context: Context, repo: NoteRepository): File? = withContext(Dispatchers.IO) {
        try {
            val notes = repo.allNotes.first()
            val folders = repo.folders.first()
            val memos = repo.memos.first()
            val dir = backupDir(context).apply { mkdirs() }
            val file = File(dir, "$FILE_PREFIX${LocalDate.now()}.zip")
            file.outputStream().use { out -> NoteExporter.exportZip(notes, folders, memos, out) }
            prune(dir)
            file
        } catch (_: Exception) {
            null
        }
    }

    /** Deletes the oldest auto-backups, keeping the newest [AutoBackup.KEEP_COUNT]. */
    private fun prune(dir: File) {
        dir.listFiles { f -> f.isFile && f.name.startsWith(FILE_PREFIX) }
            ?.sortedByDescending { it.name }
            ?.drop(KEEP_COUNT)
            ?.forEach { it.delete() }
    }
}

/** Weekly scheduled backup. Runs via WorkManager even when the app is closed. */
class AutoBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val container = (applicationContext as? FieldNotesApp)?.container
            ?: return@withContext Result.failure()
        val file = AutoBackup.run(applicationContext, container.noteRepository)
        if (file != null && file.exists()) Result.success() else Result.retry()
    }
}
