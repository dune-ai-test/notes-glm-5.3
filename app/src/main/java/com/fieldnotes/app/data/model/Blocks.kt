package com.fieldnotes.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed class Block {

    @Serializable
    @SerialName("heading")
    data class Heading(var text: String = "") : Block()

    @Serializable
    @SerialName("paragraph")
    data class Paragraph(var text: String = "", var emphasized: Boolean = false) : Block()

    @Serializable
    @SerialName("checklist")
    data class Checklist(var items: List<ChecklistItem> = emptyList()) : Block() {
        val doneCount: Int get() = items.count { it.done }
        val progress: Float get() = if (items.isEmpty()) 0f else doneCount.toFloat() / items.size
    }

    @Serializable
    @SerialName("highlight")
    data class Highlight(var text: String = "") : Block()

    @Serializable
    @SerialName("image")
    data class Image(var path: String = "", var caption: String = "") : Block()

    @Serializable
    @SerialName("audio")
    data class Audio(
        var path: String = "",
        var durationMs: Long = 0L,
        var amplitudes: List<Int> = emptyList(),
        var title: String = "Voice memo"
    ) : Block()
}

@Serializable
data class ChecklistItem(var text: String = "", var done: Boolean = false)

val BlockJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun encodeBlocks(blocks: List<Block>): String = BlockJson.encodeToString(blocks)

fun encodeToStringList(values: List<Int>): String = BlockJson.encodeToString(values)

fun decodeStringList(json: String): List<Int> = try {
    BlockJson.decodeFromString<List<Int>>(json)
} catch (_: Exception) {
    emptyList()
}

fun decodeBlocks(json: String): List<Block> = try {
    BlockJson.decodeFromString<List<Block>>(json)
} catch (_: Exception) {
    emptyList()
}

fun List<Block>.plainText(): String = joinToString(" ") { block ->
    when (block) {
        is Block.Heading -> block.text
        is Block.Paragraph -> block.text
        is Block.Highlight -> block.text
        is Block.Checklist -> block.items.joinToString(" ") { it.text }
        is Block.Image -> block.caption
        is Block.Audio -> block.title
    }
}

fun wordCount(text: String): Int = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

fun List<Block>.wordCount(): Int = wordCount(plainText())

/** Replaces the text of a text-bearing block, used by the editor's BasicTextFields. */
fun Block.withText(text: String): Block = when (this) {
    is Block.Heading -> copy(text = text)
    is Block.Paragraph -> copy(text = text)
    is Block.Highlight -> copy(text = text)
    is Block.Checklist -> this
    is Block.Image -> this
    is Block.Audio -> this
}
