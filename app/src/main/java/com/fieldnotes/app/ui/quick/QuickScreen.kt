@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.fieldnotes.app.ui.quick

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.fieldnotes.app.data.db.MemoEntity
import com.fieldnotes.app.data.model.Block
import com.fieldnotes.app.data.repo.AppSettings
import com.fieldnotes.app.di.LocalAppContainer
import com.fieldnotes.app.navigation.navigateTopLevel
import com.fieldnotes.app.ui.components.ColoredDot
import com.fieldnotes.app.ui.components.RecordSheet
import com.fieldnotes.app.ui.components.SectionLabel
import com.fieldnotes.app.ui.components.Waveform
import com.fieldnotes.app.ui.theme.Accent
import com.fieldnotes.app.ui.theme.Butter
import com.fieldnotes.app.ui.theme.CardWhite
import com.fieldnotes.app.ui.theme.DotGray
import com.fieldnotes.app.ui.theme.FT
import com.fieldnotes.app.ui.theme.Ink
import com.fieldnotes.app.ui.theme.InkCard
import com.fieldnotes.app.ui.theme.Line
import com.fieldnotes.app.ui.theme.Lilac
import com.fieldnotes.app.ui.theme.Muted
import com.fieldnotes.app.ui.theme.Sage
import com.fieldnotes.app.ui.theme.WarmPaper
import com.fieldnotes.app.ui.theme.noteColor
import com.fieldnotes.app.ui.theme.tagColor
import com.fieldnotes.app.util.TimeFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val QuickTabs = listOf("Today", "Capture", "Play")

@Composable
fun QuickScreen(navController: NavHostController, onCreateNote: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: QuickViewModel = viewModel(
        factory = viewModelFactory {
            initializer { QuickViewModel(container.noteRepository, container.settingsRepository) }
        }
    )
    val memos by vm.memos.collectAsStateWithLifecycle()
    val newMemos by vm.newMemosCount.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val focus by vm.focus.collectAsStateWithLifecycle()
    val settings by container.settingsRepository.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings())
    val playback by container.audioPlayer.state.collectAsStateWithLifecycle()
    val recState by container.audioRecorder.state.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(0) }
    var showRecord by remember { mutableStateOf(false) }
    var showNewTag by remember { mutableStateOf(false) }
    var deleteMemo by remember { mutableStateOf<MemoEntity?>(null) }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showRecord = true
        else Toast.makeText(context, "Microphone permission is needed for voice memos", Toast.LENGTH_SHORT).show()
    }
    val startRecording: () -> Unit = {
        scope.launch {
            val ok = container.audioRecorder.start(
                scope = scope,
                dir = container.recordingsDir,
                bitRate = if (settings.audioQuality == "high") 256_000 else 128_000
            )
            if (!ok) Toast.makeText(context, "Couldn't start recording", Toast.LENGTH_SHORT).show()
        }
    }
    val requestMic: () -> Unit = {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            startRecording()
        } else {
            micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val path = container.imageStore.copyFrom(uri)
                val id = container.noteRepository.createNote()
                val created = container.noteRepository.getNote(id)
                if (created != null && path != null) {
                    container.noteRepository.saveBlocks(created.note, listOf(Block.Image(path = path)))
                }
                vm.touchStreak()
                navController.navigate("editor/$id")
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(WarmPaper)
            .statusBarsPadding()
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                Modifier.padding(top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SectionLabel("Capture & Play · ${TimeFormat.smartDate(System.currentTimeMillis())}")
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Quick minds.", style = FT.heroTitle, color = Ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AvatarButton(initial = settings.userName.take(1).uppercase()) {
                            navController.navigateTopLevel("settings")
                        }
                        AddButton(onClick = onCreateNote)
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(CardWhite)
                    .border(1.dp, Line, RoundedCornerShape(50))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                QuickTabs.forEachIndexed { index, label ->
                    val selected = tab == index
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) Ink else Color.Transparent)
                            .clickable { tab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = FT.chip,
                            color = if (selected) CardWhite else Muted
                        )
                    }
                }
            }

            when (tab) {
                0 -> TodayTab(
                    streak = streak,
                    focus = focus,
                    memos = memos.take(3),
                    newMemos = newMemos,
                    tags = tags.take(4),
                    playback = playback,
                    onText = onCreateNote,
                    onVoice = { requestMic() },
                    onSketch = {
                        Toast.makeText(context, "Sketching is coming soon", Toast.LENGTH_SHORT).show()
                    },
                    onToggleFocus = vm::toggleFocus,
                    onResetFocus = vm::resetFocus,
                    onPlayMemo = { id, path -> container.audioPlayer.toggle(id, path) },
                    onOpenPlayTab = { tab = 2 },
                    onNewTag = { showNewTag = true }
                )
                1 -> CaptureTab(
                    onText = onCreateNote,
                    onVoice = { requestMic() },
                    onPhoto = {
                        pickPhoto.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSketch = {
                        Toast.makeText(context, "Sketching is coming soon", Toast.LENGTH_SHORT).show()
                    }
                )
                else -> PlayTab(
                    focus = focus,
                    memos = memos,
                    playback = playback,
                    onToggleFocus = vm::toggleFocus,
                    onResetFocus = vm::resetFocus,
                    onPlayMemo = { id, path -> container.audioPlayer.toggle(id, path) },
                    onDeleteMemo = { deleteMemo = it }
                )
            }
        }
    }

    if (showRecord) {
        RecordSheet(
            recorderState = recState,
            onDismiss = {
                container.audioRecorder.cancel()
                showRecord = false
            },
            onStart = startRecording,
            onSave = {
                val result = container.audioRecorder.stop()
                showRecord = false
                if (result != null) {
                    vm.saveMemo(result) {
                        Toast.makeText(context, "Voice memo saved", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showNewTag) {
        NewTagDialog(
            onDismiss = { showNewTag = false },
            onCreate = { name ->
                showNewTag = false
                vm.createTag(name)
            }
        )
    }

    deleteMemo?.let { memo ->
        AlertDialog(
            onDismissRequest = { deleteMemo = null },
            title = { Text("Delete memo?", style = FT.sectionTitle, color = Ink) },
            text = { Text("“${memo.title}” will be removed.", style = FT.body, color = com.fieldnotes.app.ui.theme.InkSoft) },
            confirmButton = {
                TextButton(onClick = {
                    val target = deleteMemo
                    deleteMemo = null
                    if (target != null) vm.deleteMemo(target)
                }) {
                    Text("Delete", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteMemo = null }) { Text("Cancel", color = Ink) }
            }
        )
    }
}

@Composable
private fun TodayTab(
    streak: Int,
    focus: FocusState,
    memos: List<MemoEntity>,
    newMemos: Int,
    tags: List<QuickTag>,
    playback: com.fieldnotes.app.data.media.AudioPlayer.State,
    onText: () -> Unit,
    onVoice: () -> Unit,
    onSketch: () -> Unit,
    onToggleFocus: () -> Unit,
    onResetFocus: () -> Unit,
    onPlayMemo: (String, String) -> Unit,
    onOpenPlayTab: () -> Unit,
    onNewTag: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        QuickCaptureCard(onText = onText, onVoice = onVoice, onSketch = onSketch)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StreakCard(streak = streak, modifier = Modifier.weight(0.42f))
            FocusCard(
                focus = focus,
                onToggle = onToggleFocus,
                onReset = onResetFocus,
                modifier = Modifier.weight(0.58f)
            )
        }

        VoiceMemosCard(
            memos = memos,
            newCount = newMemos,
            playback = playback,
            onPlay = onPlayMemo,
            onViewAll = onOpenPlayTab
        )

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionLabel("Tags", color = Muted)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    tags.forEach { tag ->
                        Surface(shape = RoundedCornerShape(50), color = tagColor(tag.colorIndex)) {
                            Row(
                                Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text(
                                    "#${tag.name}",
                                    style = FT.chip,
                                    color = if (tag.colorIndex == 6) CardWhite else Ink
                                )
                                Text(
                                    tag.count.toString(),
                                    style = FT.monoTiny,
                                    color = if (tag.colorIndex == 6) DotGray else Muted
                                )
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CardWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Line)
                    ) {
                        Row(
                            Modifier
                                .clickable(onClick = onNewTag)
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Add,
                                contentDescription = null,
                                tint = Ink,
                                modifier = Modifier.size(12.dp)
                            )
                            Text("New tag", style = FT.chip, color = Ink)
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = InkCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E2E2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.PanTool,
                        contentDescription = null,
                        tint = CardWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "Swipe, hold & flick to organize",
                        style = FT.cardTitleSmall.copy(fontSize = 12.5.sp),
                        color = CardWhite
                    )
                    Text(
                        "Try dragging a card — it bounces like paper.",
                        style = FT.bodySmall.copy(fontSize = 10.5.sp),
                        color = Muted
                    )
                }
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Muted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CaptureTab(onText: () -> Unit, onVoice: () -> Unit, onPhoto: () -> Unit, onSketch: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("New capture", color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CaptureOption("Text note", "Plain or rich", Icons.Outlined.TextFields, Modifier.weight(1f), onText)
            CaptureOption("Voice memo", "Record & replay", Icons.Outlined.Mic, Modifier.weight(1f), onVoice)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CaptureOption("Photo note", "From your gallery", Icons.Outlined.Image, Modifier.weight(1f), onPhoto)
            CaptureOption("Sketch", "Coming soon", Icons.Outlined.Edit, Modifier.weight(1f), onSketch)
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SectionLabel("Tip", color = Muted)
                Text(
                    "Everything you capture lands on Home as a card — checklists, memos and photos each get their own look.",
                    style = FT.bodySmall,
                    color = com.fieldnotes.app.ui.theme.InkSoft
                )
            }
        }
    }
}

@Composable
private fun CaptureOption(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardWhite,
        modifier = modifier
    ) {
        Column(
            Modifier
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = WarmPaper) {
                Box(Modifier.padding(9.dp)) {
                    Icon(icon, contentDescription = title, tint = Ink, modifier = Modifier.size(20.dp))
                }
            }
            Text(title, style = FT.cardTitleSmall.copy(fontSize = 13.sp), color = Ink)
            Text(subtitle, style = FT.monoTiny, color = Muted)
        }
    }
}

@Composable
private fun PlayTab(
    focus: FocusState,
    memos: List<MemoEntity>,
    playback: com.fieldnotes.app.data.media.AudioPlayer.State,
    onToggleFocus: () -> Unit,
    onResetFocus: () -> Unit,
    onPlayMemo: (String, String) -> Unit,
    onDeleteMemo: (MemoEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FocusCard(focus = focus, onToggle = onToggleFocus, onReset = onResetFocus)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionLabel("All memos", color = Muted)
            Text(
                "${memos.size}",
                style = FT.monoTiny,
                color = Muted
            )
        }
        if (memos.isEmpty()) {
            Text("No memos yet — record one from Quick capture.", style = FT.bodySmall, color = Muted)
        }
        memos.forEach { memo ->
            MemoRow(
                memo = memo,
                active = playback.activeId == "memo-${memo.id}",
                playing = playback.activeId == "memo-${memo.id}" && playback.isPlaying,
                onPlay = { onPlayMemo("memo-${memo.id}", memo.path) },
                onDelete = { onDeleteMemo(memo) }
            )
        }
    }
}

@Composable
private fun QuickCaptureCard(onText: () -> Unit, onVoice: () -> Unit, onSketch: () -> Unit) {
    val decorative = remember { List(14) { 6 + ((it * 29) % 15) } }
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Ink,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(shape = RoundedCornerShape(50), color = Color(0x14FFFFFF)) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ColoredDot(Accent, size = 6.dp)
                        Text("QUICK CAPTURE", style = FT.monoBadge, color = CardWhite)
                    }
                }
                Box(
                    Modifier
                        .size(width = 26.dp, height = 3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x33FFFFFF))
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "What's on\nyour mind?",
                    style = FT.heroTitle.copy(fontSize = 24.sp, lineHeight = 29.sp),
                    color = CardWhite,
                    modifier = Modifier.weight(1f)
                )
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF2E2E2E)) {
                    Box(Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
                        Waveform(
                            amplitudes = decorative,
                            baseColor = Accent,
                            idleColor = Accent.copy(alpha = 0.45f),
                            playedColor = Accent,
                            maxHeight = 20.dp,
                            barWidth = 4.dp,
                            gap = 3.dp
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CaptureChip("Text", Icons.Outlined.TextFields, selected = false, modifier = Modifier.weight(1f), onClick = onText)
                CaptureChip("Voice", Icons.Outlined.Mic, selected = true, modifier = Modifier.weight(1f), onClick = onVoice)
                CaptureChip("Sketch", Icons.Outlined.Edit, selected = false, modifier = Modifier.weight(1f), onClick = onSketch)
            }
        }
    }
}

@Composable
private fun CaptureChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) Accent else CardWhite,
        modifier = modifier
    ) {
        Row(
            Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) CardWhite else Ink,
                modifier = Modifier.size(14.dp)
            )
            Text(
                label,
                style = FT.chip.copy(fontSize = 11.5.sp),
                color = if (selected) CardWhite else Ink
            )
        }
    }
}

@Composable
private fun StreakCard(streak: Int, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(24.dp), color = Butter, modifier = modifier) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Ink),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = CardWhite,
                    modifier = Modifier.size(17.dp)
                )
            }
            Text(streak.toString(), style = FT.statNumber.copy(fontSize = 27.sp), color = Ink)
            Text(
                "day streak\nkeep it up!",
                style = FT.monoTiny,
                color = Ink.copy(alpha = 0.6f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(6) { index ->
                    Box(
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (index < streak.coerceAtMost(6)) Ink else Color(0x33FFFFFF))
                    )
                }
            }
        }
    }
}

@Composable
fun FocusCard(
    focus: FocusState,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CardWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionLabel("Focus session", color = Muted)
                ColoredDot(if (focus.running) Sage else DotGray, size = 7.dp)
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(focus.clock, style = FT.statNumber, color = Ink)
                    Text("Deep work • Lo-fi", style = FT.bodySmall.copy(fontSize = 11.sp), color = Muted)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = CircleShape, color = Ink) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clickable(onClick = onToggle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (focus.running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = if (focus.running) "Pause" else "Start",
                                tint = CardWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        "reset",
                        style = FT.monoTiny,
                        color = Muted,
                        modifier = Modifier.clickable(onClick = onReset)
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(WarmPaper)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(focus.progress.coerceIn(0f, 1f))
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Accent)
                )
            }
        }
    }
}

@Composable
private fun VoiceMemosCard(
    memos: List<MemoEntity>,
    newCount: Int,
    playback: com.fieldnotes.app.data.media.AudioPlayer.State,
    onPlay: (String, String) -> Unit,
    onViewAll: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Lilac,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("VOICE MEMOS", style = FT.monoLabel, color = Ink)
                if (newCount > 0) {
                    Surface(shape = RoundedCornerShape(50), color = Ink) {
                        Text(
                            "$newCount new",
                            style = FT.monoBadge,
                            color = CardWhite,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            if (memos.isEmpty()) {
                Text("No memos yet.", style = FT.bodySmall.copy(fontSize = 11.5.sp), color = Ink.copy(alpha = 0.6f))
            }
            memos.forEach { memo ->
                val active = playback.activeId == "memo-${memo.id}"
                Surface(shape = RoundedCornerShape(50), color = CardWhite) {
                    Row(
                        Modifier
                            .combinedClickable(
                                onClick = { onPlay("memo-${memo.id}", memo.path) },
                                onLongClick = onViewAll
                            )
                            .padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Ink),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (active && playback.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = "Play",
                                tint = CardWhite,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            memo.title,
                            style = FT.cardTitleSmall.copy(fontSize = 12.5.sp),
                            color = Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            TimeFormat.duration(memo.durationMs),
                            style = FT.monoTiny,
                            color = Muted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoRow(
    memo: MemoEntity,
    active: Boolean,
    playing: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = CardWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .combinedClickable(onClick = onPlay, onLongClick = onDelete)
                .padding(start = 8.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Ink),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (active && playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = "Play",
                    tint = CardWhite,
                    modifier = Modifier.size(14.dp)
                )
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    memo.title,
                    style = FT.cardTitleSmall.copy(fontSize = 12.5.sp),
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    TimeFormat.smartDate(memo.createdAt),
                    style = FT.monoTiny,
                    color = Muted
                )
            }
            Text(TimeFormat.duration(memo.durationMs), style = FT.monoTiny, color = Muted)
        }
    }
}

@Composable
private fun AvatarButton(initial: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = tagColor(5),
        modifier = Modifier.size(38.dp)
    ) {
        Box(
            Modifier.clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, style = FT.cardTitle.copy(fontSize = 15.sp), color = Ink)
        }
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Butter,
        modifier = Modifier.size(38.dp)
    ) {
        Box(
            Modifier.clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = "New note",
                tint = Ink,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun NewTagDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New tag", style = FT.sectionTitle, color = Ink) },
        text = {
            androidx.compose.foundation.text.BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = FT.body.copy(color = Ink),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarmPaper, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                decorationBox = { inner ->
                    Box {
                        if (name.isEmpty()) Text("e.g. recipes", style = FT.body, color = Muted)
                        inner()
                    }
                }
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }) { Text("Create", color = Ink) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) }
        }
    )
}
