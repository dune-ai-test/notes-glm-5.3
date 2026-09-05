@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fieldnotes.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fieldnotes.app.data.media.AudioRecorder
import com.fieldnotes.app.ui.theme.Accent
import com.fieldnotes.app.ui.theme.CardWhite
import com.fieldnotes.app.ui.theme.FT
import com.fieldnotes.app.ui.theme.Ink
import com.fieldnotes.app.ui.theme.Muted
import com.fieldnotes.app.ui.theme.WarmPaper
import com.fieldnotes.app.util.TimeFormat

@Composable
fun RecordSheet(
    recorderState: AudioRecorder.State,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onSave: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Voice memo", style = FT.sectionTitle, color = Ink)
            Text(
                TimeFormat.duration(recorderState.elapsedMs),
                style = FT.statNumber,
                color = if (recorderState.isRecording) Accent else Ink
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(WarmPaper),
                contentAlignment = Alignment.Center
            ) {
                if (recorderState.isRecording) {
                    Waveform(
                        amplitudes = recorderState.amplitudes,
                        live = true,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                } else {
                    Text("Tap the button to record", style = FT.bodySmall, color = Muted)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Text(
                    "Cancel",
                    style = FT.button,
                    color = Muted,
                    modifier = Modifier.clickable(onClick = onDismiss)
                )
                Surface(
                    shape = CircleShape,
                    color = if (recorderState.isRecording) Ink else Accent,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(
                        Modifier.clickable {
                            if (recorderState.isRecording) onSave() else onStart()
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = if (recorderState.isRecording) "Stop" else "Record",
                            tint = CardWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    "Save",
                    style = FT.button,
                    color = if (recorderState.isRecording) Ink else Muted,
                    modifier = Modifier.clickable(
                        enabled = recorderState.isRecording,
                        onClick = onSave
                    )
                )
            }
        }
    }
}
