package com.fieldnotes.app.data.export

import com.fieldnotes.app.data.db.FolderEntity
import com.fieldnotes.app.data.db.MemoEntity
import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.model.Block
import com.fieldnotes.app.data.model.BlockJson
import com.fieldnotes.app.data.model.decodeBlocks
import com.fieldnotes.app.data.model.decodeStringList
import com.fieldnotes.app.data.model.encodeBlocks
import com.fieldnotes.app.util.TimeFormat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Structured backup payload written to `backup.json` inside the ZIP. */
@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long,
    val folders: List<BackupFolder>,
    val tags: List<BackupTag>,
    val notes: List<BackupNote>,
    val memos: List<BackupMemo>
)

@Serializable
data class BackupFolder(val id: Long, val name: String, val iconKey: String, val colorIndex: Int)

@Serializable
data class BackupTag(val id: Long, val name: String, val colorIndex: Int)

@Serializable
data class BackupNote(
    val folderId: Long,
    val title: String,
    val blocks: List<Block>,
    val colorIndex: Int,
    val kind: Int,
    val pinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val tagIds: List<Long>
)

@Serializable
data class BackupMemo(
    val title: String,
    val path: String,
    val durationMs: Long,
    val amplitudes: List<Int>,
    val createdAt: Long
)

/**
 * Pure Kotlin export helpers: notes to Markdown, and a full ZIP backup
 * (Markdown + structured JSON + media files).
 */
object NoteExporter {

    fun folderName(folders: List<FolderEntity>, folderId: Long): String =
        folders.firstOrNull { it.id == folderId }?.name ?: "Unfiled"

    fun buildBackupData(
        notes: List<NoteWithTags>,
        folders: List<FolderEntity>,
        memos: List<MemoEntity>
    ): BackupData = BackupData(
        exportedAt = System.currentTimeMillis(),
        folders = folders.map { BackupFolder(it.id, it.name, it.iconKey, it.colorIndex) },
        tags = notes.flatMap { it.tags }.distinctBy { it.id }
            .map { BackupTag(it.id, it.name, it.colorIndex) },
        notes = notes.map {
            BackupNote(
                folderId = it.note.folderId,
                title = it.note.title,
                blocks = decodeBlocks(it.note.blocksJson),
                colorIndex = it.note.colorIndex,
                kind = it.note.kind,
                pinned = it.note.pinned,
                createdAt = it.note.createdAt,
                updatedAt = it.note.updatedAt,
                tagIds = it.tags.map { t -> t.id }
            )
        },
        memos = memos.map {
            BackupMemo(
                title = it.title,
                path = it.path,
                durationMs = it.durationMs,
                amplitudes = decodeStringList(it.amplitudesJson),
                createdAt = it.createdAt
            )
        }
    )

    fun noteToMarkdown(note: NoteWithTags, folderLabel: String): String {
        val sb = StringBuilder()
        sb.appendLine("# ${note.note.title.ifBlank { "Untitled" }}")
        sb.appendLine()
        sb.appendLine("- Folder: $folderLabel")
        sb.appendLine("- Tags: ${note.tags.joinToString(", ") { "#${it.name}".ifBlank { "#" } }}")
        sb.appendLine("- Created: ${TimeFormat.dateShort(note.note.createdAt)}")
        sb.appendLine("- Updated: ${TimeFormat.dateShort(note.note.updatedAt)}")
        sb.appendLine()
        for (block in decodeBlocks(note.note.blocksJson)) {
            when (block) {
                is Block.Heading -> {
                    sb.appendLine("## ${block.text}")
                    sb.appendLine()
                }
                is Block.Paragraph -> {
                    val text = if (block.emphasized) "**${block.text}**" else block.text
                    sb.appendLine(text)
                    sb.appendLine()
                }
                is Block.Checklist -> {
                    block.items.forEach { sb.appendLine("- [${if (it.done) "x" else " "}] ${it.text}") }
                    sb.appendLine()
                }
                is Block.Highlight -> {
                    sb.appendLine("> ${block.text}")
                    sb.appendLine()
                }
                is Block.Image -> {
                    val name = block.path.substringAfterLast('/').ifBlank { "image" }
                    sb.appendLine("![](media/$name)")
                    if (block.caption.isNotBlank()) sb.appendLine("*${block.caption}*")
                    sb.appendLine()
                }
                is Block.Audio -> {
                    val name = block.path.substringAfterLast('/').ifBlank { "audio" }
                    sb.appendLine("- 🎙 ${block.title} (${TimeFormat.duration(block.durationMs)}) — media/$name")
                    sb.appendLine()
                }
            }
        }
        return sb.toString().trimEnd() + "\n"
    }

    fun exportZip(
        notes: List<NoteWithTags>,
        folders: List<FolderEntity>,
        memos: List<MemoEntity>,
        output: OutputStream
    ) {
        val folderLabels = folders.associateBy({ it.id }, { it.name })
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(BlockJson.encodeToString(buildBackupData(notes, folders, memos)).toByteArray())
            zip.closeEntry()

            notes.forEachIndexed { index, note ->
                val safeTitle = note.note.title.ifBlank { "Untitled" }
                    .replace(Regex("[^a-zA-Z0-9 \\-_]"), "").trim().replace(' ', '_')
                    .ifBlank { "note_${note.note.id}" }
                val entryName = "notes/${"%02d".format(index + 1)}-$safeTitle.md"
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(noteToMarkdown(note, folderName(folders, note.note.folderId)).toByteArray())
                zip.closeEntry()

                // Include note media + the raw structured JSON.
                for (block in decodeBlocks(note.note.blocksJson)) {
                    val path = when (block) {
                        is Block.Image -> block.path
                        is Block.Audio -> block.path
                        else -> ""
                    }
                    if (path.isNotBlank()) {
                        val file = File(path)
                        if (file.exists()) {
                            zip.putNextEntry(ZipEntry("media/${file.name}"))
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
                val jsonEntry = "notes/${"%02d".format(index + 1)}-$safeTitle.json"
                zip.putNextEntry(ZipEntry(jsonEntry))
                zip.write(encodeBlocks(decodeBlocks(note.note.blocksJson)).toByteArray())
                zip.closeEntry()
            }
        }
    }
}
