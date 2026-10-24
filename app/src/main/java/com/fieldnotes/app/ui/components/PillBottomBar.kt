package com.fieldnotes.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.fieldnotes.app.ui.theme.FN

enum class TopLevelTab(val route: String, val label: String, val icon: ImageVector) {
    LIBRARY("library", "Library", Icons.Outlined.Folder),
    HOME("home", "Home", Icons.Outlined.Home),
    REMINDER("quick", "Reminder", Icons.Outlined.Event),
    SETTINGS("settings", "Settings", Icons.Outlined.Settings)
}

val topLevelRoutes: Set<String> = TopLevelTab.entries.map { it.route }.toSet()

@Composable
fun PillBottomBar(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
    onFab: () -> Unit,
    hideTabs: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (hideTabs) Arrangement.End else Arrangement.Start
    ) {
        if (hideTabs) {
            FabButton(onFab)
            return@Row
        }
        Surface(
            shape = CircleShape,
            color = FN.strong,
            border = BorderStroke(1.dp, FN.line),
            shadowElevation = 10.dp,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                TopLevelTab.entries.forEachIndexed { index, tab ->
                    val selected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (selected) FN.onStrong else Color.Transparent)
                            .clickable { onSelect(index) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) FN.inkFixed else FN.onStrong.copy(alpha = 0.6f),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        FabButton(onFab)
    }
}

@Composable
private fun FabButton(onFab: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = FN.strong,
        border = BorderStroke(1.dp, FN.line),
        shadowElevation = 10.dp,
        modifier = Modifier.size(56.dp)
    ) {
        Box(
            Modifier.clickable(onClick = onFab),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "New note",
                tint = FN.onStrong,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
