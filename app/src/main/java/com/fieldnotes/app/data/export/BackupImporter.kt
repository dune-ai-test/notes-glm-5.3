package com.fieldnotes.app.data.export

import android.content.Context
import com.fieldnotes.app.data.db.AppDatabase
import com.fieldnotes.app.data.db.FolderEntity
import com.fieldnotes.app.data.db.MemoEntity
import com.fieldnotes.app.data.db.NoteEntity
import com.fieldnotes.app.data.db.TagEntity
import com.fieldnotes.app.data.model.Block
import com.fieldnotes.app.data.model.BlockJson
import com.fieldnotes.app.data.model.decodeBlocks
import com.fieldnotes.app.data.model.decodeStringList
import com.fieldnotes.app.data.model.encodeBlocks
import com.fieldnotes.app.data.model.encodeToStringList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

data class ImportSummary(
    val notes: Int,
    val memos: Int,
    val newFolders: Int,
    val newTags: Int = 0,
    val legacy: Boolean = false,
    val skipped: Boolean = false
)

/**
 * Restores a Field Notes ZIP backup.
 *
 * Two formats are supported:
 *  - **Structured** (v1.1.7+): `backup.json` describing folders, notes, memos.
 *  - **Legacy** (older exports): only `notes/NN-title.md` + `notes/NN-title.json`
 *    (a bare JSON array of blocks) per note, plus media files. Titles come from
 *    the Markdown heading; everything lands in Unfiled.
 *
 * Media files are copied into the app's private storage and their paths are
 * rewritten by file name. Folders are merged by name; notes and memos are
 * always added as new entries.
 */
object BackupImporter {

    suspend fun import(context: Context, db: AppDatabase, stream: java.io.InputStream): ImportSummary =
        withContext(Dispatchers.IO) {
            var backup: BackupData? = null
            val mdFiles = mutableMapOf<String, String>()   // base name -> markdown text
            val jsonFiles = mutableMapOf<String, String>() // base name -> block-array JSON
            val copiedMedia = mutableMapOf<String, String>() // original name -> new absolute path

            ZipInputStream(stream.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == "backup.json" -> {
                            backup = try {
                                BlockJson.decodeFromString<BackupData>(zip.readBytes().decodeToString())
                            } catch (_: Exception) {
                                null
                            }
                        }
                        name.startsWith("media/") -> {
                            val fileName = name.removePrefix("media/")
                            if (fileName.isNotBlank()) {
                                val target = File(
                                    File(context.filesDir, "imports"),
                                    "${System.currentTimeMillis()}_$fileName"
                                )
                                runCatching {
                                    target.outputStream().use { zip.copyTo(it) }
                                    copiedMedia[fileName] = target.absolutePath
                                }
                            }
                        }
                        name.startsWith("notes/") && name.endsWith(".md") -> {
                            runCatching {
                                mdFiles[name.removeSuffix(".md")] = zip.readBytes().decodeToString()
                            }
                        }
                        name.startsWith("notes/") && name.endsWith(".json") -> {
                            runCatching {
                                jsonFiles[name.removeSuffix(".json")] = zip.readBytes().decodeToString()
                            }
                        }
                    }
                    entry = zip.nextEntry
                }
            }

            val data = backup
            if (data != null) {
                importStructured(context, db, data, copiedMedia)
            } else if (jsonFiles.isNotEmpty()) {
                importLegacy(context, db, jsonFiles, mdFiles, copiedMedia)
            } else {
                ImportSummary(notes = 0, memos = 0, newFolders = 0, skipped = true)
            }
        }

    private suspend fun importStructured(
        context: Context,
        db: AppDatabase,
        data: BackupData,
        copiedMedia: Map<String, String>
    ): ImportSummary {
        // Folders (merge by name)
        val folderIdMap = mutableMapOf<Long, Long>()
        var newFolders = 0
        data.folders.forEach { backupFolder ->
            val existing = db.folderDao().getFolderByName(backupFolder.name)
            val newId = if (existing != null) {
                existing.id
            } else {
                newFolders++
                db.folderDao().insert(
                    FolderEntity(
                        name = backupFolder.name,
                        iconKey = backupFolder.iconKey,
                        colorIndex = backupFolder.colorIndex
                    )
                )
            }
            folderIdMap[backupFolder.id] = newId
        }

        // Tags from pre-tag-removal backups are ignored.

        fun remap(path: String): String {
            if (path.isBlank()) return path
            val name = path.substringAfterLast('/')
            return copiedMedia[name] ?: path
        }

        data.notes.forEach { backupNote ->
            val remappedBlocks: List<Block> = backupNote.blocks.map { block ->
                when (block) {
                    is Block.Image -> block.copy(path = remap(block.path))
                    is Block.Audio -> block.copy(path = remap(block.path))
                    else -> block
                }
            }
            val noteId = db.noteDao().insert(
                NoteEntity(
                    title = backupNote.title,
                    blocksJson = encodeBlocks(remappedBlocks),
                    folderId = folderIdMap[backupNote.folderId] ?: unfiledId(db),
                    colorIndex = backupNote.colorIndex,
                    kind = backupNote.kind,
                    pinned = backupNote.pinned,
                    createdAt = backupNote.createdAt,
                    updatedAt = backupNote.updatedAt
                )
            )
        }

        data.memos.forEach { backupMemo ->
            db.memoDao().insert(
                MemoEntity(
                    title = backupMemo.title,
                    path = remap(backupMemo.path),
                    durationMs = backupMemo.durationMs,
                    amplitudesJson = encodeToStringList(backupMemo.amplitudes),
                    createdAt = backupMemo.createdAt
                )
            )
        }

        return ImportSummary(
            notes = data.notes.size,
            memos = data.memos.size,
            newFolders = newFolders
        )
    }

    private suspend fun importLegacy(
        context: Context,
        db: AppDatabase,
        jsonFiles: Map<String, String>,
        mdFiles: Map<String, String>,
        copiedMedia: Map<String, String>
    ): ImportSummary {
        val folderId = unfiledId(db)
        var imported = 0

        jsonFiles.forEach { (baseName, blocksJson) ->
            val blocks: List<Block> = try {
                decodeBlocks(blocksJson)
            } catch (_: Exception) {
                emptyList()
            }
            val md = mdFiles[baseName]
            val title = md?.lineSequence()
                ?.firstOrNull { it.startsWith("# ") }
                ?.removePrefix("# ")?.trim()
                ?: baseName.substringAfter('-').replace('_', ' ')

            fun remap(path: String): String {
                if (path.isBlank()) return path
                val name = path.substringAfterLast('/')
                return copiedMedia[name] ?: path
            }

            val remappedBlocks: List<Block> = blocks.map { block ->
                when (block) {
                    is Block.Image -> block.copy(path = remap(block.path))
                    is Block.Audio -> block.copy(path = remap(block.path))
                    else -> block
                }
            }

            val now = System.currentTimeMillis()
            db.noteDao().insert(
                NoteEntity(
                    title = title.ifBlank { "Untitled" },
                    blocksJson = encodeBlocks(remappedBlocks),
                    folderId = folderId,
                    colorIndex = 0,
                    kind = NoteEntity.KIND_TEXT,
                    pinned = false,
                    createdAt = now,
                    updatedAt = now
                )
            )
            imported++
        }

        return ImportSummary(notes = imported, memos = 0, newFolders = 0, legacy = true)
    }

    private suspend fun unfiledId(db: AppDatabase): Long =
        db.folderDao().getFolderByName("Unfiled")?.id ?: NoteEntity.DEFAULT_FOLDER_ID
}
