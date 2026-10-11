@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.fieldnotes.app.ui.zone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.fieldnotes.app.ui.components.BiometricUnlock
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT

private data class VaultEntry(
    val title: String,
    val username: String,
    val password: String
)

/**
 * Password vault — UI preview only. Entries live in memory for this session
 * and are cleared when the app exits; persistence and encryption come later.
 */
@Composable
fun PasswordToolCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var unlocked by remember { mutableStateOf(false) }
    var showUnlock by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    val entries = remember {
        mutableStateListOf(
            VaultEntry("Gmail", "me@gmail.com", "hb#2026!x"),
            VaultEntry("Home Wi-Fi", "TP-Link_5G", "butterfly-88"),
            VaultEntry("Bank portal", "maya.c", "S3cure!pass")
        )
    }
    val revealed = remember { mutableStateMapOf<Int, Boolean>() }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = FN.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, FN.line),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (unlocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = if (unlocked) FN.sage else FN.accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text("PASSWORDS", style = FT.monoBadge, color = FN.text)
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (unlocked) FN.sage.copy(alpha = 0.25f) else FN.accentSoft
                ) {
                    Text(
                        if (unlocked) "UNLOCKED" else "LOCKED",
                        style = FT.monoBadge,
                        color = if (unlocked) FN.text else FN.accent,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            if (!unlocked) {
                Text(
                    "Keep logins behind your fingerprint. Demo — entries are not saved yet.",
                    style = FT.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                    color = FN.muted
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = FN.strong,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        "Unlock vault",
                        style = FT.button,
                        color = FN.onStrong,
                        modifier = Modifier
                            .clickable { showUnlock = true }
                            .padding(horizontal = 22.dp, vertical = 10.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    entries.forEachIndexed { index, entry ->
                        val revealedThis = revealed[index] == true
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(FN.surfaceAlt)
                                .combinedClickable(
                                    onClick = { revealed[index] = !revealedThis },
                                    onLongClick = {
                                        entries.removeAt(index)
                                        Toast.makeText(context, "Entry removed (demo)", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.title,
                                    style = FT.cardTitleSmall.copy(fontSize = 12.5.sp),
                                    color = FN.text
                                )
                                Text(
                                    entry.username,
                                    style = FT.monoTiny,
                                    color = FN.muted
                                )
                            }
                            Text(
                                if (revealedThis) entry.password else "••••••••",
                                style = FT.monoChip,
                                color = FN.textSoft
                            )
                            Icon(
                                imageVector = if (revealedThis) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Reveal",
                                tint = FN.dotGray,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable { showAdd = true }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = FN.strong,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = "Add entry",
                                    tint = FN.onStrong,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Text("Add entry", style = FT.chipSmall, color = FN.text)
                    }
                }
                Text(
                    "Demo — entries live in memory and clear on exit. Lock again by leaving the app.",
                    style = FT.monoTiny,
                    color = FN.muted
                )
            }
        }
    }

    if (showUnlock) {
        BiometricUnlock(
            title = "Unlock vault",
            subtitle = "Demo vault — entries are not persisted",
            onSuccess = {
                showUnlock = false
                unlocked = true
            },
            onDismiss = { showUnlock = false },
            onUnavailable = {
                showUnlock = false
                unlocked = true
                Toast.makeText(context, "No biometrics set up — demo opens unlocked", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAdd) {
        AddVaultEntryDialog(
            onDismiss = { showAdd = false },
            onCreate = { title, user, pass ->
                showAdd = false
                entries.add(VaultEntry(title, user, pass))
            }
        )
    }
}

@Composable
private fun AddVaultEntryDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, username: String, password: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add entry", style = FT.sectionTitle, color = FN.text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VaultDialogField("Name (e.g. Gmail)", title, { title = it }, false)
                VaultDialogField("Username", username, { username = it }, false)
                VaultDialogField("Password", password, { password = it }, !showPassword)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable { showPassword = !showPassword }
                ) {
                    Icon(
                        imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = null,
                        tint = FN.muted,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        if (showPassword) "Hide password" else "Show password",
                        style = FT.chipSmall,
                        color = FN.muted
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank() && password.isNotBlank()) {
                    onCreate(title.trim(), username.trim(), password)
                }
            }) {
                Text("Add", color = FN.text)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = FN.muted) }
        }
    )
}

@Composable
private fun VaultDialogField(
    hint: String,
    value: String,
    onValue: (String) -> Unit,
    mask: Boolean
) {
    BasicTextField(
        value = value,
        onValueChange = onValue,
        textStyle = FT.bodySmall.copy(color = FN.text),
        cursorBrush = SolidColor(FN.accent),
        singleLine = true,
        visualTransformation = if (mask) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = if (mask && value.isNotEmpty()) {
                KeyboardType.Password
            } else {
                KeyboardType.Text
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FN.surfaceAlt)
            .padding(12.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(hint, style = FT.bodySmall, color = FN.muted)
                }
                inner()
            }
        }
    )
}
