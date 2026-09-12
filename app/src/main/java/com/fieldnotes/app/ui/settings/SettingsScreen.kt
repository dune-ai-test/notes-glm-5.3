package com.fieldnotes.app.ui.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Save
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
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fieldnotes.app.data.repo.AppSettings
import com.fieldnotes.app.di.LocalAppContainer
import com.fieldnotes.app.ui.components.CircleIconButton
import com.fieldnotes.app.ui.components.SectionLabel
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT
import com.fieldnotes.app.util.TimeFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavHostController) {
    val container = LocalAppContainer.current
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(container) } }
    )
    val settings by vm.settings.collectAsStateWithLifecycle()
    val noteCount by vm.noteCount.collectAsStateWithLifecycle()
    val memoCount by vm.memoCount.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showNameDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showEraseDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }
    var showAutoLockDialog by remember { mutableStateOf(false) }
    var backupRunning by remember { mutableStateOf(false) }
    var backupFileName by remember { mutableStateOf("") }
    var syncing by remember { mutableStateOf(false) }

    val cacheSize by produceState(initialValue = 0L) {
        value = vm.cacheSizeBytes()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            vm.exportAll(uri) { ok ->
                container.playChime()
                Toast.makeText(
                    context,
                    if (ok) "Backup exported" else "Export failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.importBackup(uri) { ok, message ->
                if (ok) container.playChime()
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
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
            .background(FN.bg)
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
                Text("Settings", style = FT.screenTitle.copy(fontSize = 30.sp), color = FN.text)
                Text(
                    "${settings.userName} • Local • $noteCount notes",
                    style = FT.monoTiny,
                    color = FN.muted
                )
            }
            CircleIconButton(Icons.Outlined.Search, "Search") {
                toast("Search lives on the Home tab")
            }
        }

        // Profile card
        Surface(shape = RoundedCornerShape(24.dp), color = FN.strong, modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(shape = RoundedCornerShape(14.dp), color = FN.peach) {
                        Box(Modifier.padding(12.dp)) {
                            Text(
                                settings.userName.take(1).uppercase(),
                                style = FT.cardTitleLarge,
                                color = FN.inkFixed
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(settings.userName, style = FT.sectionTitle, color = FN.onStrong)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = FN.sage,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                "All notes on this device • $noteCount notes",
                                style = FT.monoTiny,
                                color = FN.muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(50), color = FN.inkCardTile) {
                    Text(
                        if (syncing) "Checking…" else "Sync",
                        style = FT.button,
                        color = FN.onStrong,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!syncing) {
                                    syncing = true
                                    scope.launch {
                                        delay(600)
                                        syncing = false
                                        container.playChime()
                                        toast("$noteCount notes • $memoCount memos stored on this device")
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
                iconBg = FN.lilac,
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
                iconBg = FN.lilac,
                title = "Appearance",
                subtitle = when (settings.appearance) {
                    "dark" -> "Dark"
                    "light" -> "Light • Warm paper"
                    else -> "System"
                },
                onClick = { showAppearanceDialog = true }
            ) {
                Chevron()
            }
            SettingRow(
                icon = Icons.Outlined.Language,
                iconBg = FN.sky,
                title = "Language",
                subtitle = "English (US)",
                onClick = { showLanguageDialog = true }
            ) {
                Chevron()
            }
            SettingRow(
                icon = Icons.Outlined.Vibration,
                iconBg = FN.sage,
                title = "Haptics",
                subtitle = "Subtle feedback on capture"
            ) {
                Toggle(settings.haptics) { vm.setHaptics(it) }
            }
        }

        Section("Audio") {
            SettingRow(
                icon = Icons.Outlined.GraphicEq,
                iconBg = FN.butter,
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
                iconBg = FN.lilac,
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
                iconBg = FN.butter,
                title = "Sounds",
                subtitle = "Playback ticks & success chimes"
            ) {
                Toggle(settings.sounds) { vm.setSounds(it) }
            }
        }

        Section("Privacy & Data") {
            SettingRow(
                icon = Icons.Outlined.Fingerprint,
                iconBg = FN.lilac,
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
                icon = Icons.Outlined.LockClock,
                iconBg = FN.sky,
                title = "Auto-lock",
                subtitle = when (settings.autoLockMinutes) {
                    0 -> "Immediately on background"
                    5 -> "After 5 minutes in background"
                    15 -> "After 15 minutes in background"
                    else -> "After 1 minute in background"
                },
                onClick = { showAutoLockDialog = true }
            ) {
                Chevron()
            }
            SettingRow(
                icon = Icons.Outlined.Backup,
                iconBg = FN.sage,
                title = "Scheduled backup",
                subtitle = "Weekly ZIP • keeps last 4 • device only"
            ) {
                Toggle(settings.autoBackup) { vm.setAutoBackup(it) }
            }
            SettingRow(
                icon = Icons.Outlined.Save,
                iconBg = FN.butter,
                title = "Back up now",
                subtitle = if (backupRunning) "Writing backup…" else if (backupFileName.isBlank()) "Write this week's ZIP now" else "Last: $backupFileName"
            ) {
                ValueChip(
                    text = if (backupRunning) "…" else "Run",
                    onClick = {
                        if (!backupRunning) {
                            backupRunning = true
                            vm.runAutoBackupNow { ok, name ->
                                backupRunning = false
                                if (ok) {
                                    backupFileName = name
                                    container.playChime()
                                }
                                toast(if (ok) "Backup saved to the device" else "Backup failed")
                            }
                        }
                    }
                )
            }
            SettingRow(
                icon = Icons.Outlined.AutoDelete,
                iconBg = FN.peach,
                title = "Trash",
                subtitle = "Deleted notes • kept 30 days",
                onClick = { navController.navigate("trash") }
            ) {
                Chevron()
            }
            SettingRow(
                icon = Icons.Outlined.DeleteSweep,
                iconBg = FN.sky,
                title = "Clear cache",
                subtitle = "${formatBytes(cacheSize)} • Previews & temp files"
            ) {
                ValueChip(text = "Clear", accent = true) { vm.clearCache { toast("Cache cleared") } }
            }
            SettingRow(
                icon = Icons.Outlined.UploadFile,
                iconBg = FN.sage,
                title = "Export all notes",
                subtitle = "ZIP • Markdown + media"
            ) {
                ValueChip(text = "Export") { exportLauncher.launch("field-notes-backup.zip") }
            }
            SettingRow(
                icon = Icons.Outlined.Download,
                iconBg = FN.sky,
                title = "Import backup",
                subtitle = "Restore from a Field Notes ZIP"
            ) {
                ValueChip(text = "Import") {
                    importLauncher.launch(arrayOf("application/zip"))
                }
            }
        }

        Section("More") {
            SettingRow(
                icon = Icons.Outlined.HelpOutline,
                iconBg = FN.sky,
                title = "Help & guides",
                subtitle = "Tips, shortcuts, how it works",
                onClick = { showHelpDialog = true }
            ) {
                Chevron()
            }
            SettingRow(
                icon = Icons.Outlined.CardGiftcard,
                iconBg = FN.peach,
                title = "What's new",
                subtitle = "v1.1 • Dark mode & reminders",
                onClick = { showWhatsNewDialog = true }
            ) {
                Chevron()
            }
            SettingRow(
                icon = Icons.Outlined.StarBorder,
                iconBg = FN.butter,
                title = "Rate the app",
                subtitle = "Loving the capture flow?",
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=com.fieldnotes.app")
                    )
                    runCatching { context.startActivity(intent) }
                        .onFailure { toast("Thanks for the love!") }
                }
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
                    tint = FN.accent,
                    modifier = Modifier.size(17.dp)
                )
                Text("Log out", style = FT.button, color = FN.accent)
            }
            Spacer(Modifier.size(18.dp))
            CircleIconButton(
                icon = Icons.Outlined.Delete,
                contentDescription = "Erase all data",
                background = FN.accentSoft,
                tint = FN.accent,
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
            Text("Version 1.0 • Build 1", style = FT.monoTiny, color = FN.dotGray)
            Text("Privacy • Terms • Acknowledgements", style = FT.monoTiny, color = FN.dotGray)
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

    if (showAppearanceDialog) {
        ChoiceDialog(
            title = "Appearance",
            options = listOf(
                "system" to "System",
                "light" to "Light • Warm paper",
                "dark" to "Dark"
            ),
            selected = settings.appearance,
            onSelect = {
                showAppearanceDialog = false
                vm.setAppearance(it)
            },
            onDismiss = { showAppearanceDialog = false }
        )
    }

    if (showAutoLockDialog) {
        ChoiceDialog(
            title = "Auto-lock",
            options = listOf(
                "0" to "Immediately on background",
                "1" to "After 1 minute",
                "5" to "After 5 minutes",
                "15" to "After 15 minutes"
            ),
            selected = settings.autoLockMinutes.toString(),
            onSelect = {
                showAutoLockDialog = false
                vm.setAutoLockMinutes(it.toInt())
            },
            onDismiss = { showAutoLockDialog = false }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Language", style = FT.sectionTitle, color = FN.text) },
            text = {
                Text(
                    "English (US) is the only language available right now. More languages are planned.",
                    style = FT.body,
                    color = FN.textSoft
                )
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("OK", color = FN.text) }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("How Field Notes works", style = FT.sectionTitle, color = FN.text) },
            text = {
                Text(
                    "• Tap + on any screen to start a note.\n" +
                        "• Use the editor toolbar to add checklists, images and voice memos.\n" +
                        "• Long-press a card on Home to pin or delete it.\n" +
                        "• Export creates a ZIP you can re-import anytime from this screen.",
                    style = FT.body,
                    color = FN.textSoft
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Got it", color = FN.text) }
            }
        )
    }

    if (showWhatsNewDialog) {
        AlertDialog(
            onDismissRequest = { showWhatsNewDialog = false },
            title = { Text("What's new", style = FT.sectionTitle, color = FN.text) },
            text = {
                Text(
                    "v1.1\n• Dark mode (Settings → Appearance)\n" +
                        "• Reminders with a mini calendar\n" +
                        "• Import & export full backups\n" +
                        "• Faster, cleaner note editor",
                    style = FT.body,
                    color = FN.textSoft
                )
            },
            confirmButton = {
                TextButton(onClick = { showWhatsNewDialog = false }) { Text("Nice", color = FN.text) }
            }
        )
    }

    if (showEraseDialog) {
        AlertDialog(
            onDismissRequest = { showEraseDialog = false },
            title = { Text("Erase everything?", style = FT.sectionTitle, color = FN.text) },
            text = {
                Text(
                    "All notes, memos and settings will be wiped permanently. This can't be undone.",
                    style = FT.body,
                    color = FN.textSoft
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showEraseDialog = false
                    container.playChime()
                    vm.wipeAll { toast("Everything erased") }
                }) {
                    Text("Erase", color = FN.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEraseDialog = false }) { Text("Cancel", color = FN.textSoft) }
            }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title, modifier = Modifier.padding(start = 6.dp), color = FN.dotGray)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = FN.onStrong,
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
                Icon(icon, contentDescription = null, tint = FN.inkFixed, modifier = Modifier.size(17.dp))
            }
        }
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = FT.chip.copy(fontSize = 13.sp), color = FN.text)
            Text(
                subtitle,
                style = FT.bodySmall.copy(fontSize = 11.sp),
                color = FN.muted,
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
        tint = FN.dotGray,
        modifier = Modifier.size(16.dp)
    )
}

@Composable
private fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = FN.onStrong,
            checkedTrackColor = FN.strong,
            uncheckedThumbColor = FN.onStrong,
            uncheckedTrackColor = FN.line,
            uncheckedBorderColor = FN.line
        )
    )
}

@Composable
private fun ValueChip(text: String, accent: Boolean = false, onClick: (() -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (accent) FN.accentSoft else FN.bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (accent) FN.accent else FN.line)
    ) {
        Text(
            text,
            style = FT.monoChip,
            color = if (accent) FN.accent else FN.text,
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
        title = { Text("Your name", style = FT.sectionTitle, color = FN.text) },
        text = {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = FT.body.copy(color = FN.text),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(FN.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FN.bg, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) { Text("Save", color = FN.text) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = FN.muted) }
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
        title = { Text(title, style = FT.sectionTitle, color = FN.text) },
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
                                .background(if (value == selected) FN.strong else FN.bg)
                                .border(
                                    1.dp,
                                    if (value == selected) FN.strong else FN.line,
                                    CircleShape
                                )
                        )
                        Text(label, style = FT.body, color = if (value == selected) FN.text else FN.textSoft)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = FN.muted) }
        }
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%d KB".format(bytes / 1_000)
    else -> "$bytes B"
}
