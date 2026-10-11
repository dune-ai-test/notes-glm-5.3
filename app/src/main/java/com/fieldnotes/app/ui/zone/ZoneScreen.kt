package com.fieldnotes.app.ui.zone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fieldnotes.app.ui.components.SectionLabel
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT

/**
 * Zone — the personal tools hub. Each tool is a self-contained card; nothing
 * here is persisted yet (the mileage card computes live, the password card
 * keeps demo entries in memory).
 */
@Composable
fun ZoneScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .background(FN.bg)
            .statusBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionLabel("Your tools")
            Text("Zone.", style = FT.heroTitle, color = FN.text)
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ToolHeader(icon = Icons.Outlined.Calculate, label = "CALCULATOR")
            MileageToolCard()

            Spacer(Modifier.size(6.dp))
            ToolHeader(icon = Icons.Outlined.Password, label = "VAULT")
            PasswordToolCard()

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(FN.dotGray)
                )
                Text(
                    "More tools coming soon",
                    style = FT.monoTiny,
                    color = FN.muted,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = FN.textSoft,
            modifier = Modifier.size(15.dp)
        )
        SectionLabel(label, color = FN.muted)
    }
}
