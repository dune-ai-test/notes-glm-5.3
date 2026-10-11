f = 'app/src/main/java/com/fieldnotes/app/ui/components/NoteCards.kt'
src = open(f, encoding='utf-8').read()

# Checklist card (broken block from bad splice)
old_ck = '''            Text(
                text = "$folderName · ${TimeFormat.smartDate(entry.note.updatedAt)}",
                style = FT.monoTiny,
                color = soft
            )
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        }'''
new_ck = '''            Text(
                text = "$folderName · ${TimeFormat.smartDate(entry.note.updatedAt)}",
                style = FT.monoTiny,
                color = soft,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }'''
assert old_ck in src, 'checklist block mismatch'
src = src.replace(old_ck, new_ck, 1)

# Sketch card (same bad splice pattern)
old_sk = '''            Text(
                text = "$folderName · ${TimeFormat.smartDate(entry.note.updatedAt)}",
                style = FT.monoTiny,
                color = FN.inkFixed.copy(alpha = 0.5f)
            )
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        }'''
new_sk = '''            Text(
                text = "$folderName · ${TimeFormat.smartDate(entry.note.updatedAt)}",
                style = FT.monoTiny,
                color = FN.inkFixed.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }'''
assert old_sk in src, 'sketch block mismatch'
src = src.replace(old_sk, new_sk, 1)

open(f, 'w', encoding='utf-8', newline='').write(src)
print('fixed')
