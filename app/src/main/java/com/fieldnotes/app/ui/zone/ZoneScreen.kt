package com.fieldnotes.app.ui.zone

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fieldnotes.app.ui.components.ColoredDot
import com.fieldnotes.app.ui.components.SectionLabel
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT

/** Zone — the tools hub. Each card opens a dedicated tool screen. */
@Composable
fun ZoneScreen(navController: NavHostController) {
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
            SectionLabel("Trackers", color = FN.muted)
            ZoneCard(
                title = "Mileage",
                subtitle = "Fuel history per vehicle",
                count = ZoneData.mileage.size,
                countLabel = "fill-ups",
                accent = FN.butter,
                onClick = { navController.navigate("zone/mileage") }
            )

            SectionLabel("Private", color = FN.muted)
            ZoneCard(
                title = "Passwords",
                subtitle = "Fingerprint-locked vault",
                count = ZoneData.vault.size,
                countLabel = "entries",
                accent = FN.lilac,
                onClick = { navController.navigate("zone/passwords") }
            )

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
private fun ZoneCard(
    title: String,
    subtitle: String,
    count: Int,
    countLabel: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = FN.surface,
        border = BorderStroke(1.dp, FN.line),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(shape = RoundedCornerShape(14.dp), color = accent) {
                    Box(Modifier.padding(9.dp)) {
                        Icon(
                            imageVector = if (title == "Mileage") {
                                Icons.Outlined.Calculate
                            } else {
                                Icons.Outlined.Password
                            },
                            contentDescription = null,
                            tint = FN.inkFixed,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                ColoredDot(
                    if (count > 0) FN.accent else FN.dotGray,
                    size = 8.dp,
                    modifier = Modifier.padding(top = 4.dp)
                    )
            }
            Text(title, style = FT.sectionTitle.copy(fontSize = 19.sp), color = FN.text)
            Text(
                subtitle,
                style = FT.bodySmall.copy(fontSize = 11.5.sp),
                color = FN.muted
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        count.toString(),
                        style = FT.statNumber.copy(fontSize = 26.sp),
                        color = FN.text
                    )
                    Text(
                        countLabel,
                        style = FT.monoTiny,
                        color = FN.muted,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
                Surface(shape = CircleShape, color = FN.surfaceAlt) {
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = "Open",
                        tint = FN.text,
                        modifier = Modifier.padding(6.dp).size(14.dp)
                    )
                }
            }
        }
    }
}

