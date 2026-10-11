import re

f = 'app/src/main/java/com/fieldnotes/app/ui/home/HomeScreen.kt'
src = open(f, encoding='utf-8').read()

old_sig = '''private fun NoteActionSheet(
    entry: NoteWithTags,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }'''
new_sig = '''private fun NoteActionSheet(
    entry: NoteWithTags,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onShare: () -> Unit,
    onColor: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var showColors by remember { mutableStateOf(false) }'''
assert old_sig in src
src = src.replace(old_sig, new_sig, 1)

old_actions = '''            SheetAction(
                icon = Icons.Outlined.PushPin,
                label = if (entry.note.pinned) "Unpin note" else "Pin note",
                tint = Ink
            ) {
                onPin()
                onDismiss()
            }
            SheetAction(icon = Icons.Outlined.Delete, label = "Delete note", tint = Accent) {
                confirmDelete = true
            }'''
new_actions = '''            SheetAction(
                icon = Icons.Outlined.PushPin,
                label = if (entry.note.pinned) "Unpin note" else "Pin note",
                tint = Ink
            ) {
                onPin()
                onDismiss()
            }
            SheetAction(icon = Icons.Outlined.Share, label = "Share as Markdown", tint = Ink) {
                onShare()
                onDismiss()
            }
            SheetAction(icon = Icons.Outlined.Palette, label = "Change colour", tint = Ink) {
                showColors = !showColors
            }
            if (showColors) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    (0..5).forEach { index ->
                        val selected = entry.note.colorIndex == index
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(noteColor(index))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) FN.accent else FN.line,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onColor(index)
                                    onDismiss()
                                }
                        )
                    }
                }
            }
            SheetAction(icon = Icons.Outlined.Delete, label = "Delete note", tint = Accent) {
                confirmDelete = true
            }'''
assert old_actions in src
src = src.replace(old_actions, new_actions, 1)

old_call = '''            NoteActionSheet(
                entry = entry,
                onDismiss = { actionNote = null },
                onPin = { vm.togglePin(entry) },
                onDelete = { vm.delete(entry) }
            )'''
new_call = '''            NoteActionSheet(
                entry = entry,
                onDismiss = { actionNote = null },
                onPin = { vm.togglePin(entry) },
                onShare = { shareNote(entry) },
                onColor = { index -> vm.setColor(entry, index) },
                onDelete = { vm.delete(entry) }
            )'''
assert old_call in src
src = src.replace(old_call, new_call, 1)

open(f, 'w', encoding='utf-8', newline='').write(src)
print('patched')
