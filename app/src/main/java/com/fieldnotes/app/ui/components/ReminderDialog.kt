@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fieldnotes.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.fieldnotes.app.data.db.RepeatMode
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Reminder creation dialog shared by the Reminder screen and the editor's
 * "Remind me" action. Calls [onCreate] with the entered values.
 */
@Composable
fun ReminderDialog(
    initialDate: LocalDate,
    initialTitle: String = "",
    titleHint: String = "What should I remind you about?",
    showTitleField: Boolean = true,
    onDismiss: () -> Unit,
    onCreate: (title: String, dueAt: Long, repeat: RepeatMode) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var repeat by remember { mutableStateOf(RepeatMode.NONE) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New reminder", style = FT.sectionTitle, color = FN.text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (showTitleField) {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = FT.body.copy(color = FN.text),
                        cursorBrush = SolidColor(FN.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FN.surfaceAlt, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        decorationBox = { inner ->
                            Box {
                                if (title.isEmpty()) {
                                    Text(titleHint, style = FT.body, color = FN.muted)
                                }
                                inner()
                            }
                        }
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FN.surfaceAlt)
                        .clickable { showDatePicker = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Date", style = FT.chip, color = FN.muted)
                    Text(
                        date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d yyyy")),
                        style = FT.bodySmall,
                        color = FN.text
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FN.surfaceAlt)
                        .clickable { showTimePicker = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Time", style = FT.chip, color = FN.muted)
                    Text(
                        time.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a")),
                        style = FT.bodySmall,
                        color = FN.text
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    RepeatMode.entries.forEach { mode ->
                        val selected = repeat == mode
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) FN.strong else FN.surfaceAlt)
                                .clickable { repeat = mode }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                mode.label,
                                style = FT.chipSmall,
                                color = if (selected) FN.onStrong else FN.textSoft
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val dueAt = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val finalTitle = title.ifBlank { "Reminder" }
                    onCreate(finalTitle, dueAt, repeat)
                }
            ) {
                Text("Create", color = FN.text)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = FN.muted) }
        }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK", color = FN.text) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = FN.muted) }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pick a time", style = FT.sectionTitle, color = FN.text) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(state.hour, state.minute)
                    showTimePicker = false
                }) { Text("OK", color = FN.text) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel", color = FN.muted) }
            }
        )
    }
}
