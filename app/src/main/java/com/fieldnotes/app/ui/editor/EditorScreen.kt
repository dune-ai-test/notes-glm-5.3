@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.fieldnotes.app.ui.editor

import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.fieldnotes.app.data.db.NoteEntity
import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.export.NoteExporter
import com.fieldnotes.app.data.media.AudioPlayer
import com.fieldnotes.app.data.model.Block
import com.fieldnotes.app.data.model.ChecklistItem
import com.fieldnotes.app.data.model.encodeBlocks
import com.fieldnotes.app.data.model.withText
import com.fieldnotes.app.data.model.wordCount
import com.fieldnotes.app.di.LocalAppContainer
import com.fieldnotes.app.ui.components.ColoredDot
import com.fieldnotes.app.ui.components.MiniCheckbox
import com.fieldnotes.app.ui.components.MetaPill
import com.fieldnotes.app.ui.components.RecordSheet
import com.fieldnotes.app.ui.components.ReminderDialog
import com.fieldnotes.app.ui.components.SectionLabel
import com.fieldnotes.app.ui.components.TagChipView
import com.fieldnotes.app.ui.components.Waveform
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT
import com.fieldnotes.app.ui.theme.noteColor
import com.fieldnotes.app.util.TimeFormat
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    noteId: Long,
    folderIdHint: Long = -1L,
    navController: NavHostController
) {
    val container = LocalAppContainer.current
    val vm: EditorViewModel = viewModel(
        key = "editor-$noteId",
        factory = viewModelFactory { initializer { EditorViewModel(container.noteRepository, noteId, folderIdHint) } }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val allTags by vm.tags.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val playback by container.audioPlayer.state.collectAsStateWithLifecycle()
    val recState by container.audioRecorder.state.collectAsStateWithLifecycle()
    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = com.fieldnotes.app.data.repo.AppSettings())

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showMore by remember { mutableStateOf(false) }
    var showAddBlock by remember { mutableStateOf(false) }
    var showRecord by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }
    var pendingFocusIndex by remember { mutableStateOf(-1) }
    val focusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }

    val saveAndClose: () -> Unit = {
        scope.launch {
            vm.finish()
            navController.popBackStack()
        }
    }
    BackHandler { saveAndClose() }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val path = container.imageStore.copyFrom(uri)
                if (path != null) vm.addBlock(Block.Image(path = path))
            }
        }
    }

    val requestMicAndRecord = {
        showRecord = true
    }
    val micDeniedMessage = "Microphone permission is needed to record voice notes"
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            requestMicAndRecord()
        } else {
            Toast.makeText(context, micDeniedMessage, Toast.LENGTH_SHORT).show()
        }
    }
    val startMicFlow: () -> Unit = {
        if (
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            requestMicAndRecord()
        } else {
            micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    // Autofocus freshly added text blocks.
    LaunchedEffect(pendingFocusIndex) {
        if (pendingFocusIndex >= 0) {
            delay(120)
            runCatching { focusRequesters[pendingFocusIndex]?.requestFocus() }
            pendingFocusIndex = -1
        }
    }

    fun addAndFocus(block: Block) {
        vm.addBlock(block)
        pendingFocusIndex = vm.state.value.blocks.size - 1
    }

    fun shareNote() {
        val current = state
        val md = NoteExporter.noteToMarkdown(
            NoteWithTags(
                note = NoteEntity(
                    id = noteId,
                    title = current.title,
                    blocksJson = encodeBlocks(current.blocks),
                    folderId = current.folderId,
                    createdAt = current.createdAt,
                    updatedAt = current.updatedAt
                ),
                tags = allTags.filter { it.id in current.tagIds }
            ),
            NoteExporter.folderName(folders, current.folderId)
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, current.title.ifBlank { "Field note" })
            putExtra(Intent.EXTRA_TEXT, md)
        }
        context.startActivity(Intent.createChooser(intent, "Share note"))
    }

    Surface(Modifier.fillMaxSize(), color = FN.bg) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
            EditorTopBar(
                editedLabel = "edited ${TimeFormat.relative(state.updatedAt)}",
                onBack = saveAndClose,
                onShare = { if (!state.loading && !state.missing) shareNote() },
                onMore = { showMore = true }
            )

            when {
                state.loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FN.text)
                    }
                }
                state.missing || state.kind == NoteEntity.KIND_SKETCH -> {
                    SketchPlaceholder(onBack = saveAndClose)
                }
                else -> {
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SectionLabel(
                            text = "${NoteExporter.folderName(folders, state.folderId)} · edited ${TimeFormat.relative(state.updatedAt)}",
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        BasicTextField(
                            value = state.title,
                            onValueChange = vm::setTitle,
                            textStyle = FT.editorTitle.copy(color = FN.text),
                            cursorBrush = SolidColor(FN.accent),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                Box {
                                    if (state.title.isEmpty()) {
                                        Text("Title", style = FT.editorTitle, color = FN.dotGray)
                                    }
                                    inner()
                                }
                            }
                        )
                        MetaPill(
                            text = "${TimeFormat.smartDate(state.createdAt)} · ${state.blocks.wordCount() + wordCount(state.title)} words"
                        )
                        state.blocks.forEachIndexed { index, block ->
                            BlockEditor(
                                index = index,
                                block = block,
                                playback = playback,
                                focusRequester = focusRequesters[index] ?: FocusRequester().also {
                                    focusRequesters[index] = it
                                },
                                onFocused = { vm.setFocusedBlock(index) },
                                onUpdate = { vm.updateBlock(index, it) },
                                onRemove = {
                                    vm.removeBlock(index)
                                    // If that left a single fresh paragraph, put the
                                    // typing place back and focus it.
                                    val now = vm.state.value.blocks
                                    if (now.size == 1 && now[0] is Block.Paragraph && (now[0] as Block.Paragraph).text.isEmpty()) {
                                        pendingFocusIndex = 0
                                    }
                                },
                                onChecklistText = { itemIndex, text ->
                                    vm.updateChecklistItem(index, itemIndex, text = text)
                                },
                                onChecklistToggle = { itemIndex ->
                                    val checklist = block as? Block.Checklist ?: return@BlockEditor
                                    val item = checklist.items.getOrNull(itemIndex) ?: return@BlockEditor
                                    vm.updateChecklistItem(index, itemIndex, done = !item.done)
                                },
                                onChecklistAdd = { vm.addChecklistItem(index) },
                                onChecklistRemove = { itemIndex -> vm.removeChecklistItem(index, itemIndex) },
                                onTogglePlay = { audioId, path -> container.audioPlayer.toggle(audioId, path) }
                            )
                        }
                        TagsCard(
                            assignedIds = state.tagIds,
                            allTags = allTags,
                            onToggleTag = { id ->
                                vm.setTags(
                                    if (id in state.tagIds) state.tagIds - id else state.tagIds + id
                                )
                            },
                            onCreateTag = { name -> vm.createTag(name) { vm.selectTag(it) } }
                        )
                        Spacer(Modifier.height(90.dp))
                    }

                    val boldTarget = if (state.focusedBlock in state.blocks.indices) {
                        state.focusedBlock
                    } else {
                        state.blocks.indexOfLast { it is Block.Paragraph }
                    }

                    EditorToolbar(
                        emphasized = (state.blocks.getOrNull(boldTarget) as? Block.Paragraph)?.emphasized == true,
                        boldApplicable = state.blocks.getOrNull(boldTarget) is Block.Paragraph,
                        onBold = { vm.toggleEmphasis() },
                        onChecklist = { vm.turnIntoChecklist() },
                        onImage = {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onAddBlock = { showAddBlock = true },
                        onMic = startMicFlow,
                        onMore = { showMore = true }
                    )
                }
            }
        }
    }

    if (showAddBlock) {
        AddBlockSheet(
            onDismiss = { showAddBlock = false },
            onAdd = { block ->
                showAddBlock = false
                addAndFocus(block)
            },
            onPickImage = {
                showAddBlock = false
                pickImage.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRecord = {
                showAddBlock = false
                startMicFlow()
            }
        )
    }

    if (showMore) {
        MoreSheet(
            state = state,
            folders = folders,
            allTags = allTags,
            locked = state.locked,
            biometricAvailable = container.biometricAvailable,
            onDismiss = { showMore = false },
            onRemind = {
                showMore = false
                showReminder = true
            },
            onPin = { vm.setPinned(!state.pinned) },
            onToggleLock = { lock ->
                if (lock && !container.biometricAvailable) {
                    Toast.makeText(
                        context,
                        "Set up biometrics or a screen lock first",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    vm.setLocked(lock)
                }
            },
            onColor = { vm.setColor(it) },
            onFolder = { vm.setFolder(it) },
            onToggleTag = { id ->
                vm.setTags(if (id in state.tagIds) state.tagIds - id else state.tagIds + id)
            },
            onDelete = {
                showMore = false
                vm.deleteNote { navController.popBackStack() }
            }
        )
    }

    if (showReminder) {
        ReminderDialog(
            initialDate = java.time.LocalDate.now(),
            initialTitle = state.title,
            titleHint = "Reminder label",
            onDismiss = { showReminder = false },
            onCreate = { title, dueAt, repeat ->
                showReminder = false
                scope.launch {
                    val savedId = vm.ensureSaved()
                    if (savedId == null) {
                        Toast.makeText(context, "Open the note first", Toast.LENGTH_SHORT).show()
                    } else {
                        val id = container.noteRepository.createReminder(
                            title = title,
                            dueAt = dueAt,
                            repeat = repeat.key,
                            noteId = savedId
                        )
                        com.fieldnotes.app.data.reminder.ReminderScheduler.schedule(context, id, dueAt)
                        container.playChime()
                        Toast.makeText(context, "Reminder set", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showRecord) {
        RecordSheet(
            recorderState = recState,
            onDismiss = {
                container.audioRecorder.cancel()
                showRecord = false
            },
            onStart = {
                scope.launch {
                    val started = container.audioRecorder.start(
                        scope = scope,
                        dir = container.recordingsDir,
                        bitRate = if (settings.audioQuality == "high") 256_000 else 128_000
                    )
                    if (!started) {
                        Toast.makeText(
                            context,
                            "Couldn't start recording — check the microphone permission",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onSave = {
                val result = container.audioRecorder.stop()
                showRecord = false
                if (result != null) {
                    vm.addBlock(
                        Block.Audio(
                            path = result.path,
                            durationMs = result.durationMs,
                            amplitudes = result.amplitudes
                        )
                    )
                } else {
                    Toast.makeText(
                        context,
                        "Recording failed or was too short — try again",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}

@Composable
private fun EditorTopBar(
    editedLabel: String,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleTool(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text("Field Notes", style = FT.cardTitle, color = FN.text, maxLines = 1)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ColoredDot(FN.accent, size = 5.dp)
                Text(editedLabel, style = FT.monoTiny, color = FN.muted, maxLines = 1)
            }
        }
        CircleTool(Icons.Outlined.Share, "Share", onShare)
        Spacer(Modifier.size(8.dp))
        CircleTool(Icons.Outlined.MoreHoriz, "More", onMore)
    }
}

@Composable
private fun CircleTool(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = FN.surface,
        border = BorderStroke(1.dp, FN.line),
        modifier = Modifier.size(42.dp)
    ) {
        Box(
            Modifier
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = FN.text, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SketchPlaceholder(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = FN.butter) {
            Box(Modifier.padding(22.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = FN.text,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Sketching is coming soon", style = FT.sectionTitle, color = FN.text)
        Spacer(Modifier.height(6.dp))
        Text(
            "This note is a sketch. Finger drawing arrives in a later update.",
            style = FT.bodySmall,
            color = FN.muted
        )
        Spacer(Modifier.height(20.dp))
        Text("Go back", style = FT.button, color = FN.accent, modifier = Modifier.clickable(onClick = onBack))
    }
}

@Composable
private fun BlockEditor(
    index: Int,
    block: Block,
    playback: AudioPlayer.State,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onUpdate: (Block) -> Unit,
    onRemove: () -> Unit,
    onChecklistText: (Int, String) -> Unit,
    onChecklistToggle: (Int) -> Unit,
    onChecklistAdd: () -> Unit,
    onChecklistRemove: (Int) -> Unit,
    onTogglePlay: (String, String) -> Unit
) {
    when (block) {
        is Block.Heading -> TextField(
            text = block.text,
            hint = "Heading",
            style = FT.heading,
            focusRequester = focusRequester,
            onFocused = onFocused,
            onText = { onUpdate(block.withText(it)) }
        )
        is Block.Paragraph -> TextField(
            text = block.text,
            hint = "Start writing…",
            style = if (block.emphasized) FT.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            else FT.body,
            focusRequester = focusRequester,
            onFocused = onFocused,
            onText = { onUpdate(block.withText(it)) }
        )
        is Block.Highlight -> Surface(
            shape = RoundedCornerShape(14.dp),
            color = FN.butter,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = FN.text,
                            modifier = Modifier.size(12.dp)
                        )
                        Text("HIGHLIGHT", style = FT.monoBadge, color = FN.text)
                    }
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Remove",
                        tint = FN.inkFixed.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable(onClick = onRemove)
                    )
                }
                TextField(
                    text = block.text,
                    hint = "Worth remembering…",
                    style = FT.bodySmall.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    ),
                    focusRequester = focusRequester,
                    onFocused = onFocused,
                    onText = { onUpdate(block.withText(it)) }
                )
            }
        }
        is Block.Checklist -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            block.items.forEachIndexed { itemIndex, item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniCheckbox(done = item.done) { onChecklistToggle(itemIndex) }
                    BasicTextField(
                        value = item.text,
                        onValueChange = { onChecklistText(itemIndex, it) },
                        textStyle = FT.bodySmall.copy(
                            color = if (item.done) FN.muted else FN.text
                        ),
                        cursorBrush = SolidColor(FN.accent),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { if (it.isFocused) onFocused() },
                        decorationBox = { inner ->
                            Box {
                                if (item.text.isEmpty()) {
                                    Text("List item", style = FT.bodySmall, color = FN.dotGray)
                                }
                                inner()
                            }
                        }
                    )
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Remove item",
                        tint = FN.dotGray,
                        modifier = Modifier
                            .size(13.dp)
                            .clickable { onChecklistRemove(itemIndex) }
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.clickable(onClick = onChecklistAdd)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = FN.surface,
                    border = BorderStroke(1.dp, FN.line)
                ) {
                    Text(
                        "+",
                        style = FT.bodySmall,
                        color = FN.muted,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Text("Add item", style = FT.bodySmall, color = FN.muted)
            }
            if (block.items.all { it.text.isBlank() }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable(onClick = onRemove)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = FN.accent,
                        modifier = Modifier.size(13.dp)
                    )
                    Text("Delete empty list", style = FT.bodySmall, color = FN.accent)
                }
            }
        }
        is Block.Image -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = FN.surface,
                border = BorderStroke(1.dp, FN.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box {
                        if (block.path.isNotBlank() && File(block.path).exists()) {
                            AsyncImage(
                                model = File(block.path),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                            )
                        } else {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(FN.bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = null,
                                    tint = FN.dotGray,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Remove",
                            tint = FN.onStrong,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .size(16.dp)
                                .clickable(onClick = onRemove)
                        )
                    }
                    BasicTextField(
                        value = block.caption,
                        onValueChange = { onUpdate(block.copy(caption = it)) },
                        textStyle = FT.monoTiny.copy(color = FN.muted),
                        cursorBrush = SolidColor(FN.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { if (it.isFocused) onFocused() }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        decorationBox = { inner ->
                            Box {
                                if (block.caption.isEmpty()) {
                                    Text(
                                        "Add a caption…",
                                        style = FT.monoTiny,
                                        color = FN.dotGray,
                                        modifier = Modifier.padding(horizontal = 0.dp, vertical = 10.dp)
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
            }
        }
        is Block.Audio -> {
            val audioId = "block-$index"
            val active = playback.activeId == audioId
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = FN.surface,
                border = BorderStroke(1.dp, FN.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(FN.strong)
                            .clickable { onTogglePlay(audioId, block.path) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (active && playback.isPlaying) {
                                Icons.Outlined.Pause
                            } else {
                                Icons.Outlined.PlayArrow
                            },
                            contentDescription = if (active && playback.isPlaying) "Pause" else "Play",
                            tint = FN.onStrong,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = block.title,
                            style = FT.cardTitleSmall.copy(fontSize = 12.sp),
                            color = FN.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Waveform(
                            amplitudes = block.amplitudes,
                            progress = if (active) playback.progress else 0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        text = TimeFormat.duration(block.durationMs),
                        style = FT.monoTiny,
                        color = FN.muted
                    )
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Remove",
                        tint = FN.dotGray,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable(onClick = onRemove)
                    )
                }
            }
        }
    }
}

@Composable
private fun TextField(
    text: String,
    hint: String,
    style: TextStyle,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onText: (String) -> Unit
) {
    BasicTextField(
        value = text,
        onValueChange = onText,
        textStyle = style.copy(color = FN.text),
        cursorBrush = SolidColor(FN.accent),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onFocused() },
        decorationBox = { inner ->
            Box {
                if (text.isEmpty()) {
                    Text(hint, style = style, color = FN.dotGray)
                }
                inner()
            }
        }
    )
}

@Composable
private fun TagsCard(
    assignedIds: Set<Long>,
    allTags: List<com.fieldnotes.app.data.db.TagEntity>,
    onToggleTag: (Long) -> Unit,
    onCreateTag: (String) -> Unit
) {
    var showNewTag by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = FN.surface,
        border = BorderStroke(1.dp, FN.line),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Tags", style = FT.sectionTitle, color = FN.text)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                allTags.forEach { tag ->
                    TagChipView(
                        name = tag.name,
                        colorIndex = tag.colorIndex,
                        selected = tag.id in assignedIds,
                        onClick = { onToggleTag(tag.id) }
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, FN.line)
                ) {
                    Row(
                        Modifier
                            .clickable { showNewTag = true }
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = FN.text,
                            modifier = Modifier.size(12.dp)
                        )
                        Text("New tag", style = FT.chip, color = FN.text)
                    }
                }
            }
        }
    }
    if (showNewTag) {
        NewTagDialog(
            onDismiss = { showNewTag = false },
            onCreate = { name ->
                showNewTag = false
                onCreateTag(name)
            }
        )
    }
}

@Composable
private fun NewTagDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New tag", style = FT.sectionTitle, color = FN.text) },
        text = {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = FT.body.copy(color = FN.text),
                cursorBrush = SolidColor(FN.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FN.surfaceAlt, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                decorationBox = { inner ->
                    Box {
                        if (name.isEmpty()) Text("e.g. recipes", style = FT.body, color = FN.muted)
                        inner()
                    }
                }
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name) }) {
                Text("Create", color = FN.text)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = FN.muted) }
        }
    )
}

@Composable
private fun EditorToolbar(
    emphasized: Boolean,
    boldApplicable: Boolean,
    onBold: () -> Unit,
    onChecklist: () -> Unit,
    onImage: () -> Unit,
    onAddBlock: () -> Unit,
    onMic: () -> Unit,
    onMore: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = FN.surface,
        border = BorderStroke(1.dp, FN.line),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
    ) {
        Row(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            EditorTool(Icons.Outlined.FormatBold, active = emphasized, enabled = boldApplicable, "Bold", onBold)
            EditorTool(
                Icons.AutoMirrored.Outlined.FormatListBulleted,
                active = false,
                "Checklist",
                onChecklist
            )
            EditorTool(Icons.Outlined.Image, active = false, "Image", onImage)
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(FN.accent)
                    .clickable(onClick = onAddBlock),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Add block",
                    tint = FN.onStrong,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            EditorTool(Icons.Outlined.Mic, active = false, "Record", onMic)
            EditorTool(Icons.Outlined.MoreHoriz, active = false, "More", onMore)
        }
    }
}

@Composable
private fun EditorTool(
    icon: ImageVector,
    active: Boolean,
    label: String,
    onClick: () -> Unit
) {
    EditorTool(icon = icon, active = active, enabled = true, label = label, onClick = onClick)
}

@Composable
private fun EditorTool(
    icon: ImageVector,
    active: Boolean,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.35f
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (active) FN.accent.copy(alpha = 0.16f * alpha) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (active) FN.accent else FN.textSoft.copy(alpha = alpha),
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun AddBlockSheet(
    onDismiss: () -> Unit,
    onAdd: (Block) -> Unit,
    onPickImage: () -> Unit,
    onRecord: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Add block", style = FT.sectionTitle, color = FN.text)
            Spacer(Modifier.height(6.dp))
            BlockAction(Icons.Outlined.TextFields, "Paragraph") { onAdd(Block.Paragraph()) }
            BlockAction(Icons.Outlined.MenuBook, "Heading") { onAdd(Block.Heading()) }
            BlockAction(
                Icons.AutoMirrored.Outlined.FormatListBulleted,
                "Checklist"
            ) { onAdd(Block.Checklist(emptyList())) }
            BlockAction(Icons.Outlined.MenuBook, "Highlight") { onAdd(Block.Highlight()) }
            BlockAction(Icons.Outlined.Image, "Image") { onPickImage() }
            BlockAction(Icons.Outlined.Mic, "Voice memo") { onRecord() }
        }
    }
}

@Composable
private fun BlockAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = FN.text, modifier = Modifier.size(19.dp))
        Text(label, style = FT.button, color = FN.text)
    }
}

@Composable
private fun MoreSheet(
    state: EditorViewModel.EditorState,
    folders: List<com.fieldnotes.app.data.db.FolderEntity>,
    allTags: List<com.fieldnotes.app.data.db.TagEntity>,
    locked: Boolean,
    biometricAvailable: Boolean,
    onDismiss: () -> Unit,
    onRemind: () -> Unit,
    onPin: () -> Unit,
    onToggleLock: (Boolean) -> Unit,
    onColor: (Int) -> Unit,
    onFolder: (Long) -> Unit,
    onToggleTag: (Long) -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Note options", style = FT.sectionTitle, color = FN.text)
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = "Pin",
                    tint = if (state.pinned) FN.accent else FN.dotGray,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onPin)
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onRemind)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Alarm,
                    contentDescription = null,
                    tint = FN.textSoft,
                    modifier = Modifier.size(19.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text("Remind me", style = FT.button, color = FN.text)
                    Text(
                        "Notify at a date & time",
                        style = FT.bodySmall.copy(fontSize = 11.sp),
                        color = FN.muted
                    )
                }
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = FN.dotGray,
                    modifier = Modifier.size(16.dp)
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = if (locked) FN.accent else FN.textSoft,
                    modifier = Modifier.size(19.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text("Lock this note", style = FT.button, color = FN.text)
                    Text(
                        "Ask for biometrics before opening",
                        style = FT.bodySmall.copy(fontSize = 11.sp),
                        color = FN.muted
                    )
                }
                Switch(
                    checked = locked,
                    onCheckedChange = onToggleLock,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = FN.onStrong,
                        checkedTrackColor = FN.strong,
                        uncheckedThumbColor = FN.onStrong,
                        uncheckedTrackColor = FN.line,
                        uncheckedBorderColor = FN.line
                    )
                )
            }
            SectionLabel("Card color")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (0..5).map { noteColor(it) }
                    .forEachIndexed { index, color ->
                        val selected = state.colorIndex == index
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) FN.accent else FN.line,
                                    shape = CircleShape
                                )
                                .clickable { onColor(index) }
                        )
                    }
            }
            SectionLabel("Folder")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                folders.forEach { folder ->
                    TagChipView(
                        name = folder.name,
                        colorIndex = if (state.folderId == folder.id) 6 else folder.colorIndex,
                        selected = state.folderId == folder.id,
                        onClick = { onFolder(folder.id) }
                    )
                }
            }
            SectionLabel("Tags")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                allTags.forEach { tag ->
                    TagChipView(
                        name = tag.name,
                        colorIndex = tag.colorIndex,
                        selected = tag.id in state.tagIds,
                        onClick = { onToggleTag(tag.id) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onDelete)
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = FN.accent, modifier = Modifier.size(19.dp))
                Text("Delete note", style = FT.button, color = FN.accent)
            }
        }
    }
}
