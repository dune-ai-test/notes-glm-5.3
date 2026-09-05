package com.fieldnotes.app

import com.fieldnotes.app.data.db.FolderEntity
import com.fieldnotes.app.data.db.MemoEntity
import com.fieldnotes.app.data.db.NoteEntity
import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.db.TagEntity
import com.fieldnotes.app.data.export.NoteExporter
import com.fieldnotes.app.data.model.Block
import com.fieldnotes.app.data.model.ChecklistItem
import com.fieldnotes.app.data.model.encodeBlocks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteExporterTest {

    @Test
    fun `markdown contains all block types`() {
        val entry = NoteWithTags(
            note = NoteEntity(
                id = 1,
                title = "The warmth of linen light",
                blocksJson = encodeBlocks(
                    listOf(
                        Block.Paragraph("Morning in Kyoto arrives softly."),
                        Block.Heading("Why it stays with you"),
                        Block.Checklist(
                            listOf(
                                ChecklistItem("Linen curtains", done = true),
                                ChecklistItem("Paper lanterns")
                            )
                        ),
                        Block.Highlight("“Light is not seen.”")
                    )
                ),
                createdAt = 0,
                updatedAt = 0
            ),
            tags = listOf(TagEntity(id = 1, name = "travel", colorIndex = 5))
        )

        val markdown = NoteExporter.noteToMarkdown(entry, folderLabel = "Personal")

        assertTrue(markdown.contains("# The warmth of linen light"))
        assertTrue(markdown.contains("- Folder: Personal"))
        assertTrue(markdown.contains("#travel"))
        assertTrue(markdown.contains("## Why it stays with you"))
        assertTrue(markdown.contains("- [x] Linen curtains"))
        assertTrue(markdown.contains("- [ ] Paper lanterns"))
        assertTrue(markdown.contains("> “Light is not seen.”"))
    }

    @Test
    fun `folder name lookup falls back to Unfiled`() {
        val folders = listOf(FolderEntity(id = 1, name = "Work", iconKey = "work", colorIndex = 5))
        assertEquals("Work", NoteExporter.folderName(folders, 1))
        assertEquals("Unfiled", NoteExporter.folderName(folders, 99))
    }

    @Test
    fun `zip export produces readable archive`() {
        val entry = NoteWithTags(
            note = NoteEntity(
                id = 1,
                title = "Market list",
                blocksJson = encodeBlocks(listOf(Block.Paragraph("tomatoes"))),
                createdAt = 0,
                updatedAt = 0
            ),
            tags = emptyList()
        )
        val out = java.io.ByteArrayOutputStream()
        NoteExporter.exportZip(
            notes = listOf(entry),
            folders = emptyList(),
            memos = listOf(MemoEntity(id = 1, title = "Morning idea", createdAt = 0)),
            output = out
        )

        val zip = java.util.zip.ZipInputStream(out.toByteArray().inputStream())
        val names = generateSequence { zip.nextEntry }.map { it.name }.toList()
        assertTrue(names.any { it.startsWith("notes/") && it.endsWith(".md") })
        assertTrue(names.contains("backup.json"))
    }

    @Test
    fun `unsafe filename characters are stripped`() {
        val entry = NoteWithTags(
            note = NoteEntity(
                id = 1,
                title = "Ideas: big/secret?",
                blocksJson = "[]",
                createdAt = 0,
                updatedAt = 0
            ),
            tags = emptyList()
        )
        val markdown = NoteExporter.noteToMarkdown(entry, folderLabel = "Ideas")
        assertTrue(markdown.contains("# Ideas: big/secret?"))
    }

    @Test
    fun `exporter is pure and deterministic`() {
        val entry = NoteWithTags(
            note = NoteEntity(id = 1, title = "A", blocksJson = "[]", createdAt = 0, updatedAt = 0),
            tags = emptyList()
        )
        val folders = listOf(FolderEntity(id = 1, name = "Work", iconKey = "work", colorIndex = 5))
        assertEquals(
            NoteExporter.noteToMarkdown(entry, "Work"),
            NoteExporter.noteToMarkdown(entry, "Work")
        )
    }
}
