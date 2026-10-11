package com.fieldnotes.app.ui.zone

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fieldnotes.app.ui.components.BiometricUnlock
import com.fieldnotes.app.ui.components.CircleIconButton
import com.fieldnotes.app.util.plural
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT

@Composable
fun PasswordScreen(navController: NavHostController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var unlocked by remember { mutableStateOf(false) }
    var showUnlock by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    val entries = ZoneData.vault
    val revealed = remember { mutableStateMapOf<Int, Boolean>() }

    Column(
        Modifier
            .fillMaxSize()
            .background(FN.bg)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircleIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back") {
                navController.popBackStack()
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text("Passwords", style = FT.sectionTitle.copy(fontSize = 20.sp), color = FN.text)
                Text(
                    "${plural(entries.size, "entry", "entries")} • demo, not saved yet",
                    style = FT.monoTiny,
                    color = FN.muted
                )
            }
            CircleIconButton(
                icon = if (unlocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                contentDescription = if (unlocked) "Lock vault" else "Unlock vault",
                background = if (unlocked) FN.accentSoft else FN.strong,
                tint = if (unlocked) FN.accent else FN.surface
            ) {
                unlocked = !unlocked
            }
        }

        if (!unlocked) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(shape = CircleShape, color = FN.inkCard, modifier = Modifier.size(84.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = FN.onStrong,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Text(
                    "Vault locked",
                    style = FT.sectionTitle,
                    color = FN.text,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    "Demo — entries are kept in memory only.",
                    style = FT.bodySmall,
                    color = FN.muted,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = FN.strong,
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text(
                        "Unlock with biometrics",
                        style = FT.button,
                        color = FN.onStrong,
                        modifier = Modifier
                            .clickable { showUnlock = true }
                            .padding(horizontal = 22.dp, vertical = 10.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 132.dp, top = 6.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = FN.strong
                        ) {
                            Row(
                                Modifier
                                    .clickable { showAdd = true }
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = FN.onStrong,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text("Add entry", style = FT.button, color = FN.onStrong)
                            }
                        }
                    }
                }
                if (entries.isEmpty()) {
                    item {
                        Text(
                            "Nothing in the vault yet — add your first entry.",
                            style = FT.bodySmall,
                            color = FN.muted
                        )
                    }
                }
                items(entries.size) { index ->
                    val entry = entries[index]
                    val revealedThis = revealed[index] == true
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = FN.surface,
                        border = BorderStroke(1.dp, FN.line),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    entry.title,
                                    style = FT.cardTitleSmall.copy(fontSize = 12.5.sp),
                                    color = FN.text
                                )
                                Text(entry.username, style = FT.monoTiny, color = FN.muted)
                                Text(
                                    if (revealedThis) entry.password else "••••••••",
                                    style = FT.monoChip,
                                    color = FN.textSoft
                                )
                            }
                            Icon(
                                imageVector = if (revealedThis) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Reveal",
                                tint = FN.dotGray,
                                modifier = Modifier
                                    .size(17.dp)
                                    .clickable { revealed[index] = !revealedThis }
                            )
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Remove",
                                tint = FN.accent,
                                modifier = Modifier
                                    .size(17.dp)
                                    .clickable {
                                        entries.removeAt(index)
                                        Toast.makeText(context, "Entry removed (demo)", Toast.LENGTH_SHORT).show()
                                    }
                            )
                        }
                    }
                }
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
