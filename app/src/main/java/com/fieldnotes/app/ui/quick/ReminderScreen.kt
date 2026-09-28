@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.fieldnotes.app.ui.quick

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.fieldnotes.app.data.db.ReminderEntity
import com.fieldnotes.app.data.db.RepeatMode
import com.fieldnotes.app.data.reminder.ReminderScheduler
import com.fieldnotes.app.di.LocalAppContainer
import com.fieldnotes.app.ui.components.ColoredDot
import com.fieldnotes.app.ui.components.ReminderDialog
import com.fieldnotes.app.ui.components.SectionLabel
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val ReminderTabs = listOf("Upcoming", "Calendar")

@Composable
fun ReminderScreen(navController: androidx.navigation.NavHostController, onCreateNote: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: ReminderViewModel = viewModel(
        key = "reminders",
        factory = viewModelFactory {
            initializer { ReminderViewModel(container.noteRepository, container.context) }
        }
    )
    val pending by vm.pending.collectAsStateWithLifecycle()
    val completed by vm.completed.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var tab by rememberSaveable { mutableStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf(LocalDate.now()) }
    var deleteTarget by remember { mutableStateOf<ReminderEntity?>(null) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val requestNotifications: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 &&
            !ReminderScheduler.hasNotificationPermission(context)
        ) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun addToCalendar(title: String, dueAt: Long) {
        val intent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_INSERT
            data = android.provider.CalendarContract.Events.CONTENT_URI
            putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, dueAt)
            putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, dueAt + 60 * 60_000L)
            putExtra(android.provider.CalendarContract.Events.TITLE, title)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { Toast.makeText(context, "No calendar app found", Toast.LENGTH_SHORT).show() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(FN.bg)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionLabel("Don't forget")
                Text("Reminders.", style = FT.heroTitle, color = FN.text)
            }
            Surface(
                shape = CircleShape,
                color = FN.strong,
                border = androidx.compose.foundation.BorderStroke(1.dp, FN.line),
                modifier = Modifier.size(44.dp)
            ) {
                Box(
                    Modifier.clickable {
                        requestNotifications()
                        pendingDate = LocalDate.now()
                        showAdd = true
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "New reminder",
                        tint = FN.onStrong,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(50))
                .background(FN.surface)
                .border(1.dp, FN.line, RoundedCornerShape(50))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ReminderTabs.forEachIndexed { index, label ->
                val selected = tab == index
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) FN.strong else Color.Transparent)
                        .clickable { tab = index }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, style = FT.chip, color = if (selected) FN.onStrong else FN.muted)
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (tab == 0) {
                UpcomingTab(
                    pending = pending,
                    completed = completed,
                    onComplete = vm::complete,
                    onRestore = vm::restore,
                    onSnooze1h = { vm.snooze(it, ReminderScheduler.snoozeUntil1h()) },
                    onSnoozeTomorrow = { vm.snooze(it, ReminderScheduler.tomorrowAt()) },
                    onOpenNote = { noteId -> navController.navigate("editor/$noteId") },
                    onDelete = { deleteTarget = it },
                    onAddToCalendar = ::addToCalendar
                )
            } else {
                CalendarTab(
                    reminders = pending,
                    onAddForDate = { date ->
                        pendingDate = date
                        showAdd = true
                    },
                    onAddToCalendar = ::addToCalendar,
                    onDelete = { deleteTarget = it },
                    onComplete = vm::complete,
                    onOpenNote = { noteId -> navController.navigate("editor/$noteId") }
                )
            }
        }
    }

    if (showAdd) {
        ReminderDialog(
            initialDate = pendingDate,
            onDismiss = { showAdd = false },
            onCreate = { title, dueAt, repeat ->
                showAdd = false
                vm.add(title, dueAt, repeat)
            }
        )
    }

    deleteTarget?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete reminder?", style = FT.sectionTitle, color = FN.text) },
            text = { Text("“${target.title}” will be removed.", style = FT.body, color = FN.textSoft) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val t = deleteTarget
                    deleteTarget = null
                    if (t != null) vm.delete(t)
                }) {
                    Text("Delete", color = FN.accent)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = FN.muted)
                }
            }
        )
    }
}

@Composable
private fun UpcomingTab(
    pending: List<ReminderEntity>,
    completed: List<ReminderEntity>,
    onComplete: (Long) -> Unit,
    onRestore: (Long) -> Unit,
    onSnooze1h: (Long) -> Unit,
    onSnoozeTomorrow: (Long) -> Unit,
    onOpenNote: (Long) -> Unit,
    onDelete: (ReminderEntity) -> Unit,
    onAddToCalendar: (String, Long) -> Unit
) {
    if (pending.isEmpty() && completed.isEmpty()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Outlined.Event,
                contentDescription = null,
                tint = FN.dotGray,
                modifier = Modifier.size(30.dp)
            )
            Text("No reminders yet", style = FT.sectionTitle, color = FN.text)
            Text("Swipe right to complete · long-press for more.", style = FT.bodySmall, color = FN.muted)
        }
        return
    }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 130.dp)
    ) {
        items(pending, key = { it.id }) { reminder ->
            SwipeableReminderRow(
                reminder = reminder,
                onComplete = { onComplete(reminder.id) },
                onSnooze1h = { onSnooze1h(reminder.id) },
                onSnoozeTomorrow = { onSnoozeTomorrow(reminder.id) },
                onOpenNote = reminder.noteId?.let { noteId -> { onOpenNote(noteId) } },
                onAddToCalendar = { onAddToCalendar(reminder.title, reminder.dueAt) },
                onDelete = { onDelete(reminder) }
            )
        }
        if (completed.isNotEmpty()) {
            item(key = "completed-header") {
                SectionLabel("Completed", color = FN.muted)
            }
            items(completed, key = { "done-${it.id}" }) { reminder ->
                CompletedRow(
                    reminder = reminder,
                    onRestore = { onRestore(reminder.id) }
                )
            }
        }
    }
}

@Composable
private fun SwipeableReminderRow(
    reminder: ReminderEntity,
    onComplete: () -> Unit,
    onSnooze1h: () -> Unit,
    onSnoozeTomorrow: () -> Unit,
    onOpenNote: (() -> Unit)?,
    onAddToCalendar: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onComplete()
                true
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(FN.sage),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Event,
                        contentDescription = null,
                        tint = FN.inkFixed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Complete", style = FT.button, color = FN.inkFixed)
                }
            }
        }
    ) {
        ReminderRow(
            reminder = reminder,
            onSnooze1h = onSnooze1h,
            onSnoozeTomorrow = onSnoozeTomorrow,
            onOpenNote = onOpenNote,
            onAddToCalendar = onAddToCalendar,
            onDelete = onDelete
        )
    }
}

@Composable
private fun ReminderRow(
    reminder: ReminderEntity,
    onSnooze1h: () -> Unit,
    onSnoozeTomorrow: () -> Unit,
    onOpenNote: (() -> Unit)?,
    onAddToCalendar: () -> Unit,
    onDelete: () -> Unit
) {
    val date = remember(reminder.dueAt) { millisToDate(reminder.dueAt) }
    val time = remember(reminder.dueAt) { millisToTime(reminder.dueAt) }
    val repeat = RepeatMode.fromKey(reminder.repeat)
    var showSnoozeMenu by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = FN.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, FN.line),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { }, onLongClick = onDelete)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(FN.surfaceAlt)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    date.dayOfMonth.toString(),
                    style = FT.statNumber.copy(fontSize = 18.sp),
                    color = FN.text
                )
                Text(
                    date.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).uppercase(),
                    style = FT.monoBadge,
                    color = FN.muted
                )
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    reminder.title,
                    style = FT.cardTitle.copy(fontSize = 13.sp),
                    color = FN.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ColoredDot(FN.accent, size = 5.dp)
                    Text(
                        formatWhen(date, time),
                        style = FT.monoTiny,
                        color = FN.muted
                    )
                    if (repeat != RepeatMode.NONE) {
                        Icon(
                            Icons.Outlined.Replay,
                            contentDescription = repeat.label,
                            tint = FN.sage,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    if (reminder.noteId != null) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = "Linked note",
                            tint = FN.sky,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
            Box {
                Surface(
                    shape = CircleShape,
                    color = FN.surfaceAlt,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        Modifier.clickable { showSnoozeMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Alarm,
                            contentDescription = "Snooze",
                            tint = FN.text,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = showSnoozeMenu,
                    onDismissRequest = { showSnoozeMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Snooze 1 hour", style = FT.bodySmall, color = FN.text) },
                        onClick = {
                            showSnoozeMenu = false
                            onSnooze1h()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Tomorrow 9:00", style = FT.bodySmall, color = FN.text) },
                        onClick = {
                            showSnoozeMenu = false
                            onSnoozeTomorrow()
                        }
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = FN.surfaceAlt,
                modifier = Modifier.size(36.dp)
            ) {
                Box(
                    Modifier.clickable(onClick = onAddToCalendar),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Event,
                        contentDescription = "Add to calendar",
                        tint = FN.text,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            if (onOpenNote != null) {
                Surface(
                    shape = CircleShape,
                    color = FN.sky,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        Modifier.clickable(onClick = onOpenNote),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = "Open note",
                            tint = FN.inkFixed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedRow(reminder: ReminderEntity, onRestore: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = FN.surfaceAlt,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ColoredDot(FN.sage, size = 8.dp)
            Text(
                reminder.title,
                style = FT.bodySmall.copy(fontSize = 12.5.sp),
                color = FN.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                "Restore",
                style = FT.button,
                color = FN.text,
                modifier = Modifier.clickable(onClick = onRestore)
            )
        }
    }
}

@Composable
private fun CalendarTab(
    reminders: List<ReminderEntity>,
    onAddForDate: (LocalDate) -> Unit,
    onAddToCalendar: (String, Long) -> Unit,
    onDelete: (ReminderEntity) -> Unit,
    onComplete: (Long) -> Unit,
    onOpenNote: (Long) -> Unit
) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    val zone = ZoneId.systemDefault()
    val byDate = remember(reminders) {
        reminders.groupBy { Instant.ofEpochMilli(it.dueAt).atZone(zone).toLocalDate() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = FN.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, FN.line),
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
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { month = month.minusMonths(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ChevronLeft, "Previous month", tint = FN.text)
                    }
                    Text(
                        month.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault()) + " " + month.year,
                        style = FT.sectionTitle,
                        color = FN.text
                    )
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { month = month.plusMonths(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ChevronRight, "Next month", tint = FN.text)
                    }
                }
                MonthGrid(
                    month = month,
                    markedDates = byDate.keys,
                    selected = selected,
                    onSelect = { selected = it }
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionLabel(
                selected.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))
            )
            Text(
                "+ New reminder",
                style = FT.button,
                color = FN.accent,
                modifier = Modifier.clickable { onAddForDate(selected) }
            )
        }

        val dayReminders = byDate[selected].orEmpty()
        if (dayReminders.isEmpty()) {
            Text("Nothing planned for this day.", style = FT.bodySmall, color = FN.muted)
        }
        dayReminders.forEach { reminder ->
            SwipeableReminderRow(
                reminder = reminder,
                onComplete = { onComplete(reminder.id) },
                onSnooze1h = { },
                onSnoozeTomorrow = { },
                onOpenNote = reminder.noteId?.let { noteId -> { onOpenNote(noteId) } },
                onAddToCalendar = { onAddToCalendar(reminder.title, reminder.dueAt) },
                onDelete = { onDelete(reminder) }
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    markedDates: Set<LocalDate>,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val firstDay = month.atDay(1)
    val offset = (firstDay.dayOfWeek.value + 6) % 7 // Monday-start week
    val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(label, style = FT.monoTiny, color = FN.muted)
                }
            }
        }
        val totalCells = offset + month.lengthOfMonth()
        val rows = (totalCells + 6) / 7
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val day = cellIndex - offset + 1
                    Box(
                        Modifier
                            .weight(1f)
                            .height(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..month.lengthOfMonth()) {
                            val date = month.atDay(day)
                            val isSelected = date == selected
                            val isToday = date == today
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) FN.strong else Color.Transparent)
                                    .then(
                                        if (isToday && !isSelected) {
                                            Modifier.border(1.dp, FN.accent, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { onSelect(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        day.toString(),
                                        style = FT.bodySmall.copy(fontSize = 12.sp),
                                        color = if (isSelected) FN.onStrong else FN.text
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    if (date in markedDates) {
                                        ColoredDot(FN.accent, size = 4.dp)
                                    } else {
                                        Spacer(Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun millisToDate(ms: Long): LocalDate =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

private fun millisToTime(ms: Long): LocalTime =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime()

private fun formatWhen(date: LocalDate, time: LocalTime): String =
    "${date.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth} · " +
        time.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
