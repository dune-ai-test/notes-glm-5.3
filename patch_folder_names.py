import re

f = 'app/src/main/java/com/fieldnotes/app/ui/components/NoteCards.kt'
src = open(f, encoding='utf-8').read()

# 1. Dispatch passes folderName to the three cards
old_disp = '''        NoteCardKind.HERO -> HeroCard(entry, blocks, folderName, onOpen, onLongPress)
        NoteCardKind.AUDIO -> AudioCard(entry, blocks, playbackState, onOpen, onLongPress, onTogglePlay)
        NoteCardKind.CHECKLIST -> ChecklistCard(entry, blocks, onOpen, onLongPress, onToggleChecklistItem)
        NoteCardKind.SKETCH -> SketchCard(entry, onOpen, onLongPress)
        NoteCardKind.QUOTE -> QuoteCard(entry, blocks, folderName, onOpen, onLongPress)
        NoteCardKind.TEXT -> TextCard(entry, blocks, folderName, dotColor, onOpen, onLongPress)'''
new_disp = '''        NoteCardKind.HERO -> HeroCard(entry, blocks, folderName, onOpen, onLongPress)
        NoteCardKind.AUDIO -> AudioCard(entry, blocks, folderName, playbackState, onOpen, onLongPress, onTogglePlay)
        NoteCardKind.CHECKLIST -> ChecklistCard(entry, blocks, folderName, onOpen, onLongPress, onToggleChecklistItem)
        NoteCardKind.SKETCH -> SketchCard(entry, folderName, onOpen, onLongPress)
        NoteCardKind.QUOTE -> QuoteCard(entry, blocks, folderName, onOpen, onLongPress)
        NoteCardKind.TEXT -> TextCard(entry, blocks, folderName, dotColor, onOpen, onLongPress)'''
assert old_disp in src
src = src.replace(old_disp, new_disp, 1)

# 2. ChecklistCard signature + date line gains folder
old_ck = '''private fun ChecklistCard(
    entry: NoteWithTags,
    blocks: List<Block>,
    onOpen: () -> Unit,
    onLongPress: (() -> Unit)?,
    onToggleChecklistItem: (Int, Int) -> Unit
) {'''
new_ck = '''private fun ChecklistCard(
    entry: NoteWithTags,
    blocks: List<Block>,
    folderName: String,
    onOpen: () -> Unit,
    onLongPress: (() -> Unit)?,
    onToggleChecklistItem: (Int, Int) -> Unit
) {'''
assert old_ck in src
src = src.replace(old_ck, new_ck, 1)

old_ck_date = '''            Text(
                text = TimeFormat.smartDate(entry.note.updatedAt),
                style = FT.monoTiny,
                color = soft
            )
        }
        Text(
            text = entry.note.title.ifBlank { "Untitled" },
            style = FT.cardTitle,
            color = ink
        )
        checklist.items.take(4)'''
new_ck_date = '''            Text(
                text = "$folderName · ${TimeFormat.smartDate(entry.note.updatedAt)}",
                style = FT.monoTiny,
                color = soft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = entry.note.title.ifBlank { "Untitled" },
            style = FT.cardTitle,
            color = ink
        )
        checklist.items.take(4)'''
assert old_ck_date in src
src = src.replace(old_ck_date, new_ck_date, 1)

# 3. SketchCard signature + date line
old_sk = '''private fun SketchCard(
    entry: NoteWithTags,
    onOpen: () -> Unit,
    onLongPress: () -> Unit
) {'''
new_sk = '''private fun SketchCard(
    entry: NoteWithTags,
    folderName: String,
    onOpen: () -> Unit,
    onLongPress: () -> Unit
) {'''
assert old_sk in src
src = src.replace(old_sk, new_sk, 1)

old_sk_date = '''            Text(
                text = TimeFormat.smartDate(entry.note.updatedAt),
                style = FT.monoTiny,
                color = FN.inkFixed.copy(alpha = 0.5f)
            )
        }
        Text(
            text = entry.note.title.ifBlank { "Untitled" },
            style = FT.cardTitle,
            color = FN.inkFixed
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = FN.onStrong,'''
new_sk_date = '''            Text(
                text = "$folderName · ${TimeFormat.smartDate(entry.note.updatedAt)}",
                style = FT.monoTiny,
                color = FN.inkFixed.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = entry.note.title.ifBlank { "Untitled" },
            style = FT.cardTitle,
            color = FN.inkFixed
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = FN.onStrong,'''
assert old_sk_date in src
src = src.replace(old_sk_date, new_sk_date, 1)

# 4. AudioCard signature + bottom meta gains folder
old_au = '''private fun AudioCard(
    entry: NoteWithTags,
    blocks: List<Block>,
    playbackState: AudioPlayer.State,
    onOpen: () -> Unit,
    onLongPress: (() -> Unit)?,
    onTogglePlay: (String, String) -> Unit
) {'''
new_au = '''private fun AudioCard(
    entry: NoteWithTags,
    blocks: List<Block>,
    folderName: String,
    playbackState: AudioPlayer.State,
    onOpen: () -> Unit,
    onLongPress: (() -> Unit)?,
    onTogglePlay: (String, String) -> Unit
) {'''
assert old_au in src
src = src.replace(old_au, new_au, 1)

old_au_meta = '''            Text(
                text = TimeFormat.smartDate(entry.note.updatedAt),
                style = FT.monoTiny,
                color = FN.muted,
                maxLines = 1
            )'''
new_au_meta = '''            Text(
                text = "$folderName · ${TimeFormat.smartDate(entry.note.updatedAt)}",
                style = FT.monoTiny,
                color = FN.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )'''
assert old_au_meta in src
src = src.replace(old_au_meta, new_au_meta, 1)

open(f, 'w', encoding='utf-8', newline='').write(src)
print('patched')
