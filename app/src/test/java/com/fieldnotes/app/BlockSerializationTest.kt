package com.fieldnotes.app

import com.fieldnotes.app.data.model.Block
import com.fieldnotes.app.data.model.ChecklistItem
import com.fieldnotes.app.data.model.decodeBlocks
import com.fieldnotes.app.data.model.encodeBlocks
import com.fieldnotes.app.data.model.plainText
import com.fieldnotes.app.data.model.withText
import com.fieldnotes.app.data.model.wordCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockSerializationTest {

    @Test
    fun `blocks survive a round trip`() {
        val blocks: List<Block> = listOf(
            Block.Paragraph("Morning in Kyoto arrives softly.", emphasized = true),
            Block.Heading("Why it stays with you"),
            Block.Checklist(
                listOf(
                    ChecklistItem("Heirloom tomatoes", done = true),
                    ChecklistItem("Sourdough loaf")
                )
            ),
            Block.Highlight("Light is not seen, it is felt."),
            Block.Image(path = "/data/images/img1.jpg", caption = "Ryoan-ji"),
            Block.Audio(
                path = "/data/recordings/rec1.m4a",
                durationMs = 42_000,
                amplitudes = listOf(4, 12, 18, 7)
            )
        )

        val json = encodeBlocks(blocks)
        val decoded = decodeBlocks(json)

        assertEquals(blocks.size, decoded.size)
        assertEquals(blocks, decoded)
    }

    @Test
    fun `decode falls back to empty list on corrupt json`() {
        assertEquals(emptyList<Block>(), decodeBlocks("not json at all {"))
        assertEquals(emptyList<Block>(), decodeBlocks(""))
    }

    @Test
    fun `withText only changes text blocks`() {
        val paragraph = Block.Paragraph("old")
        assertEquals("new", paragraph.withText("new")?.let { (it as Block.Paragraph).text })

        val audio = Block.Audio(durationMs = 1_000)
        assertTrue(audio.withText("new") === audio)
    }

    @Test
    fun `plain text and word count`() {
        val blocks: List<Block> = listOf(
            Block.Paragraph("one two three"),
            Block.Checklist(listOf(ChecklistItem("four five"))),
            Block.Heading("six")
        )
        assertEquals("one two three four five six", blocks.plainText())
        assertEquals(6, blocks.wordCount())
    }

    @Test
    fun `checklist progress`() {
        val checklist = Block.Checklist(
            listOf(
                ChecklistItem("a", done = true),
                ChecklistItem("b", done = true),
                ChecklistItem("c")
            )
        )
        assertEquals(2, checklist.doneCount)
        assertEquals(2f / 3f, checklist.progress, 0.001f)
    }
}
