package com.fieldnotes.app.ui.settings

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fieldnotes.app.data.repo.AppSettings
import com.fieldnotes.app.di.LocalAppContainer
import com.fieldnotes.app.ui.components.CircleIconButton
import com.fieldnotes.app.ui.components.SectionLabel
import com.fieldnotes.app.ui.theme.Accent
import com.fieldnotes.app.ui.theme.Butter
import com.fieldnotes.app.ui.theme.CardWhite
import com.fieldnotes.app.ui.theme.DotGray
import com.fieldnotes.app.ui.theme.FT
import com.fieldnotes.app.ui.theme.Ink
import com.fieldnotes.app.ui.theme.InkCard
import com.fieldnotes.app.ui.theme.InkSoft
import com.fieldnotes.app.ui.theme.Line
import com.fieldnotes.app.ui.theme.Lilac
import com.fieldnotes.app.ui.theme.Muted
import com.fieldnotes.app.ui.theme.Peach
import com.fieldnotes.app.ui.theme.Sage
import com.fieldnotes.app.ui.theme.Sky
import com.fieldnotes.app.ui.theme.WarmPaper
import com.fieldnotes.app.util.TimeFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val container = LocalAppContainer.current
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(container) } }
    )
    val settings by vm.settings.collectAsStateWithLifecycle()
    val noteCount by vm.noteCount.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showNameDialog by remember { mutableStateOf(false) }
    var showCaptureDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showEraseDialog by remember { mutableStateOf(false) }
    var appearanceMenu by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }

    val cacheSize by produceState(initialValue = 0L) {
        value = vm.cacheSizeBytes()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            vm.exportAll(uri) { ok ->
                Toast.makeText(
                    context,
                    if (ok) "Backup exported" else "Export failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        vm.setPushNotifications(granted)
        if (!granted) {
            Toast.makeText(context, "Notifications stay off without permission", Toast.LENGTH_SHORT).show()
        }
    }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    Column(
        Modifier
            .fillMaxSize()
            .background(WarmPaper)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Settings", style = FT.screenTitle.copy(fontSize = 30.sp), color = Ink)
                Text(
                    "${settings.userName} • Local • $noteCount notes",
                    style = FT.monoTiny,
                    color = Muted
                )
            }
            CircleIconButton(Icons.Outlined.Search, "Search") {
                toast("Search lives on the Home tab")
            }
        }

        // Profile card
        Surface(shape = RoundedCornerShape(24.dp), color = Ink, modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Peach) {
                        Box(Modifier.padding(12.dp)) {
                            Text(
                                settings.userName.take(1).uppercase(),
                                style = FT.cardTitleLarge,
                                color = Ink
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(settings.userName, style = FT.sectionTitle, color = CardWhite)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = Sage,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                "All notes on this device • $noteCount notes",
                                style = FT.monoTiny,
                                color = Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(50), color = Color(0xFF2E2E2E)) {
                    Text(
                        if (syncing) "Syncing…" else "Sync",
                        style = FT.button,
                        color = CardWhite,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!syncing) {
                                    syncing = true
                                    scope.launch {
                                        delay(900)
                                        syncing = false
                                        toast("You're up to date — everything is on this device")
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Section("Account") {
            SettingRow(
                icon = Icons.Outlined.Person,
                iconBg = Lilac,
                title = "Personal information",
                subtitle = "Name, profile photo",
                onClick = { showNameDialog = true }
            ) {
                Chevron()
            }
        }

        Section("Preferences") {
            SettingRow(
                icon = Icons.Outlined.Palette,
                iconBg = Lilac,
                title = "Appearance",
                subtitle = "Light • Warm paper"
            ) {
                Box {
                    ValueChip(
                        text = settings.appearance.replaceFirstChar { it.uppercase() },
                        onClick = { appearanceMenu = true }
                    )
                    DropdownMenu(expanded = appearanceMenu, onDismissRequest = { appearanceMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Light • Warm paper", style = FT.bodySmall, color = Ink) },
                            onClick = { appearanceMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Dark — coming soon", style = FT.bodySmall, color = Muted) },
                            onClick = { toast("Dark mode is coming soon") ; appearanceMenu = false }
                        )
                    }
                }
            }
            SettingRow(
                icon = Icons.Outlined.Language,
                iconBg = Sky,
                title = "Language",
                subtitle = "English (US)",
                onClick = { toast("English (US) is the only language in v1") }
            ) {
                Chevron()
            }
            SettingRow(
                icon = Icons.Outlined.Vibration,
                iconBg = Sage,
                title = "Haptics",
                subtitle = "Subtle feedback on capture"
            ) {
                Toggle(settings.haptics) { vm.setHaptics(it) }
            }
            SettingRow(
                icon = Icons.Outlined.Accessibility,
                iconBg = Sky,
                title = "Reduce motion",
                subtitle = "Minimize animations"
            ) {
                Toggle(settings.reduceMotion) { vm.setReduceMotion(it) }
            }
        }

        Section("Capture & Play") {
            SettingRow(
                icon = Icons.Outlined.PhotoCamera,
                iconBg = Peach,
                title = "Default capture",
                subtitle = "Used by the Quick tab"
            ) {
                ValueChip(
                    text = when (settings.defaultCapture) {
                        "text" -> "Text"
                        "photo" -> "Photo"
                        else -> "Voice"
                    },
                    onClick = { showCaptureDialog = true }
                )
            }
            SettingRow(
                icon = Icons.Outlined.Mic,
                iconBg = Sage,
                title = "Auto-transcribe",
                subtitle = "Turn voice into searchable notes — soon"
            ) {
                Toggle(settings.autoTranscribe) { vm.setAutoTranscribe(it) }
            }
            SettingRow(
                icon = Icons.Outlined.GraphicEq,
                iconBg = Butter,
                title = "Audio quality",
                subtitle = if (settings.audioQuality == "high") "High • 256 kbps" else "Standard • 128 kbps"
            ) {
                ValueChip(
                    text = if (settings.audioQuality == "high") "High" else "Standard",
                    onClick = { showQualityDialog = true }
                )
            }
        }

        Section("Notifications") {
            SettingRow(
                icon = Icons.Outlined.Notifications,
                iconBg = Lilac,
                title = "Push notifications",
                subtitle = "Reminders & mentions"
            ) {
                Toggle(settings.pushNotifications) { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= 33) {
                        notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        vm.setPushNotifications(enabled)
                    }
                }
            }
            SettingRow(
                icon = Icons.Outlined.VolumeUp,
                iconBg = Butter,
                title = "Sounds",
                subtitle = "Playback ticks & success chimes"
            ) {
                Toggle(settings.sounds) { vm.setSounds(it) }
            }
        }

        Section("Privacy & Data") {
            SettingRow(
                icon = Icons.Outlined.Fingerprint,
                iconBg = Lilac,
                title = "Biometric lock",
                subtitle = "Require unlock on open"
            ) {
                Toggle(settings.biometricLock) { enabled ->
                    if (enabled && !vm.biometricAvailable) {
                        toast("Biometric unlock isn't set up on this device")
                    } else {
                        vm.setBiometricLock(enabled)
                    }
                }
            }
            SettingRow(
                icon = Icons.Outlined.DeleteSweep,
                iconBg = Sky,
                title = "Clear cache",
                subtitle = "${formatBytes(cacheSize)} • Previews & temp files"
            ) {
                ValueChip(text = "Clear", accent = true) { vm.clearCache { toast("Cache cleared") } }
            }
            SettingRow(
                icon = Icons.Outlined.UploadFile,
                iconBg = Sage,
                title = "Export all notes",
                subtitle = "ZIP • Markdown + media"
            ) {
                ValueChip(text = "Export") { exportLauncher.launch("field-notes-backup.zip") }
            }
        }

        Section("More") {
            SettingRow(
                icon = Icons.Outlined.HelpOutline,
                iconBg = Sky,
                title = "Help & guides",
                subtitle = "Tutorials, shortcuts, FAQs",
                onClick = { toast("Help center is coming soon") }
            ) {
                Chevron()
            }
            SettingRow(
                icon = Icons.Outlined.CardGiftcard,
                iconBg = Peach,
                title = "What's new",
                subtitle = "v1.0 • Capture & Play",
                onClick = { toast("You're on the latest version") }
            ) {
                Chevron()
            }
            SettingRow(
                icon = Icons.Outlined.StarBorder,
                iconBg = Butter,
                title = "Rate the app",
                subtitle = "Loving the capture flow?",
                onClick = { toast("Thanks for the love!") }
            ) {
                Chevron()
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable { toast("Local profile — nothing to log out of") }
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(17.dp)
                )
                Text("Log out", style = FT.button, color = Accent)
            }
            Spacer(Modifier.size(18.dp))
            CircleIconButton(
                icon = Icons.Outlined.Delete,
                contentDescription = "Erase all data",
                background = Color(0xFFF7DCD3),
                tint = Accent,
                size = 38.dp
            ) {
                showEraseDialog = true
            }
        }

        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Version 1.0 • Build 1", style = FT.monoTiny, color = DotGray)
            Text("Privacy • Terms • Acknowledgements", style = FT.monoTiny, color = DotGray)
        }
    }

    if (showNameDialog) {
        NameDialog(
            current = settings.userName,
            onDismiss = { showNameDialog = false },
            onSave = {
                showNameDialog = false
                vm.setUserName(it)
            }
        )
    }

    if (showCaptureDialog) {
        ChoiceDialog(
            title = "Default capture",
            options = listOf("text" to "Text", "voice" to "Voice", "photo" to "Photo"),
            selected = settings.defaultCapture,
            onSelect = {
                showCaptureDialog = false
                vm.setDefaultCapture(it)
            },
            onDismiss = { showCaptureDialog = false }
        )
    }

    if (showQualityDialog) {
        ChoiceDialog(
            title = "Audio quality",
            options = listOf("high" to "High • 256 kbps", "standard" to "Standard • 128 kbps"),
            selected = settings.audioQuality,
            onSelect = {
                showQualityDialog = false
                vm.setAudioQuality(it)
            },
            onDismiss = { showQualityDialog = false }
        )
    }

    if (showEraseDialog) {
        AlertDialog(
            onDismissRequest = { showEraseDialog = false },
            title = { Text("Erase everything?", style = FT.sectionTitle, color = Ink) },
            text = {
                Text(
                    "All notes, memos and settings will be wiped and the demo library restored. This can't be undone.",
                    style = FT.body,
                    color = InkSoft
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showEraseDialog = false
                    vm.wipeAndReseed { toast("Erased — demo library restored") }
                }) {
                    Text("Erase", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEraseDialog = false }) { Text("Cancel", color = InkSoft) }
            }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title, modifier = Modifier.padding(start = 6.dp), color = DotGray)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = iconBg) {
            Box(Modifier.padding(8.dp)) {
                Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(17.dp))
            }
        }
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = FT.chip.copy(fontSize = 13.sp), color = Ink)
            Text(
                subtitle,
                style = FT.bodySmall.copy(fontSize = 11.sp),
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing()
    }
}

@Composable
private fun Chevron() {
    Icon(
        Icons.Outlined.ChevronRight,
        contentDescription = null,
        tint = DotGray,
        modifier = Modifier.size(16.dp)
    )
}

@Composable
private fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = CardWhite,
            checkedTrackColor = Ink,
            uncheckedThumbColor = CardWhite,
            uncheckedTrackColor = Line,
            uncheckedBorderColor = Line
        )
    )
}

@Composable
private fun ValueChip(text: String, accent: Boolean = false, onClick: (() -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (accent) Color(0xFFF7DCD3) else WarmPaper,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (accent) Accent else Line)
    ) {
        Text(
            text,
            style = FT.monoChip,
            color = if (accent) Accent else Ink,
            modifier = Modifier
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 11.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun NameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your name", style = FT.sectionTitle, color = Ink) },
        text = {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = FT.body.copy(color = Ink),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarmPaper, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) { Text("Save", color = Ink) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) }
        }
    )
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = FT.sectionTitle, color = Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(value) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(if (value == selected) Ink else WarmPaper)
                                .border(
                                    1.dp,
                                    if (value == selected) Ink else Line,
                                    CircleShape
                                )
                        )
                        Text(label, style = FT.body, color = if (value == selected) Ink else InkSoft)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) }
        }
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%d KB".format(bytes / 1_000)
    else -> "$bytes B"
}
