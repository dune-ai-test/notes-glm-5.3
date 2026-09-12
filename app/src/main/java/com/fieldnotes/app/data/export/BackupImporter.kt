package com.fieldnotes.app.data.export

import android.content.Context
import com.fieldnotes.app.data.db.AppDatabase
import com.fieldnotes.app.data.db.FolderEntity
import com.fieldnotes.app.data.db.MemoEntity
import com.fieldnotes.app.data.db.NoteEntity
import com.fieldnotes.app.data.db.NoteTagCrossRef
import com.fieldnotes.app.data.db.TagEntity
import com.fieldnotes.app.data.model.Block
import com.fieldnotes.app.data.model.BlockJson
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
    val newTags: Int,
    val skipped: Boolean = false
)

/**
 * Restores a Field Notes ZIP backup produced by [NoteExporter]. Folders and tags
 * are merged by name; notes and memos are always added as new entries; media
 * files are copied into the app's private storage with paths rewritten by file name.
 */
object BackupImporter {

    suspend fun import(context: Context, db: AppDatabase, stream: java.io.InputStream): ImportSummary =
        withContext(Dispatchers.IO) {
            var backup: BackupData? = null
            val copiedMedia = mutableMapOf<String, String>() // original file name -> new absolute path

            ZipInputStream(stream.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "backup.json" -> {
                            backup = try {
                                BlockJson.decodeFromString<BackupData>(zip.readBytes().decodeToString())
                            } catch (_: Exception) {
                                null
                            }
                        }
                        entry.name.startsWith("media/") -> {
                            val name = entry.name.removePrefix("media/")
                            if (name.isNotBlank()) {
                                val target = File(File(context.filesDir, "imports"), "${System.currentTimeMillis()}_$name")
                                runCatching {
                                    target.outputStream().use { zip.copyTo(it) }
                                    copiedMedia[name] = target.absolutePath
                                }
                            }
                        }
                    }
                    entry = zip.nextEntry
                }
            }

            val data = backup
                ?: return@withContext ImportSummary(notes = 0, memos = 0, newFolders = 0, newTags = 0, skipped = true)

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

            // Tags (merge by name)
            val tagIdMap = mutableMapOf<Long, Long>()
            var newTags = 0
            data.tags.forEach { backupTag ->
                val existing = db.tagDao().getTagByName(backupTag.name)
                val newId = if (existing != null) {
                    existing.id
                } else {
                    newTags++
                    db.tagDao().insert(TagEntity(name = backupTag.name, colorIndex = backupTag.colorIndex))
                }
                tagIdMap[backupTag.id] = newId
            }

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
                        folderId = folderIdMap[backupNote.folderId] ?: NoteEntity.DEFAULT_FOLDER_ID,
                        colorIndex = backupNote.colorIndex,
                        kind = backupNote.kind,
                        pinned = backupNote.pinned,
                        createdAt = backupNote.createdAt,
                        updatedAt = backupNote.updatedAt
                    )
                )
                val refs = backupNote.tagIds.mapNotNull { tagIdMap[it] }
                    .map { NoteTagCrossRef(noteId, it) }
                if (refs.isNotEmpty()) db.noteDao().insertNoteTags(refs)
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

            ImportSummary(
                notes = data.notes.size,
                memos = data.memos.size,
                newFolders = newFolders,
                newTags = newTags
            )
        }
}
