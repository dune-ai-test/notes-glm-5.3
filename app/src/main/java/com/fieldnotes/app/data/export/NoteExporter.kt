package com.fieldnotes.app.data.export

import com.fieldnotes.app.data.db.FolderEntity
import com.fieldnotes.app.data.db.MemoEntity
import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.model.Block
import com.fieldnotes.app.data.model.decodeBlocks
import com.fieldnotes.app.data.model.encodeBlocks
import com.fieldnotes.app.util.TimeFormat
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Pure Kotlin export helpers: notes to Markdown, and a full ZIP backup
 * (Markdown + structured JSON + media files).
 */
object NoteExporter {

    fun folderName(folders: List<FolderEntity>, folderId: Long): String =
        folders.firstOrNull { it.id == folderId }?.name ?: "Unfiled"

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
            val summary = buildString {
                appendLine("{")
                appendLine("  \"app\": \"Field Notes\",")
                appendLine("  \"notes\": ${notes.size},")
                appendLine("  \"memos\": ${memos.size},")
                appendLine("  \"folders\": ${folders.size}")
                appendLine("}")
            }
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(summary.toByteArray())
            zip.closeEntry()
        }
    }
}
