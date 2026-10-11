package com.fieldnotes.app.ui.zone

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fieldnotes.app.ui.components.CircleIconButton
import com.fieldnotes.app.ui.components.ColoredDot
import com.fieldnotes.app.ui.components.SectionLabel
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT
import com.fieldnotes.app.util.plural
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@Composable
fun MileageScreen(navController: NavHostController) {
    var showAdd by remember { mutableStateOf(false) }
    val entries = ZoneData.mileage.sortedByDescending { it.date }

    val totalDistance = entries.sumOf { it.distance }
    val totalLitres = entries.sumOf { it.litres }
    val totalSpent = entries.sumOf { it.totalPaid }
    val avgMileage = if (totalLitres > 0) totalDistance / totalLitres else 0.0

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
                Text("Mileage", style = FT.sectionTitle.copy(fontSize = 20.sp), color = FN.text)
                Text(
                    "${plural(entries.size, "entry", "entries")} • $totalDistance km logged",
                    style = FT.monoTiny,
                    color = FN.muted
                )
            }
            CircleIconButton(Icons.Outlined.Add, "Add fill-up", background = FN.strong, tint = FN.surface) {
                showAdd = true
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = FN.inkCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("ALL TIME", style = FT.monoBadge, color = FN.onStrong.copy(alpha = 0.6f))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            String.format("%.1f", avgMileage),
                            style = FT.statNumber.copy(fontSize = 38.sp),
                            color = FN.butter
                        )
                        Text(
                            "km / L average",
                            style = FT.chip,
                            color = FN.onStrong.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 9.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        SummaryStat("$totalDistance", "km")
                        SummaryStat(String.format("%.1f", totalLitres), "litres")
                        SummaryStat("₹${String.format("%.0f", totalSpent)}", "spent")
                    }
                }
            }

            SectionLabel("History", color = FN.muted)

            if (entries.isEmpty()) {
                Text(
                    "No fill-ups yet — tap + to log one.",
                    style = FT.bodySmall,
                    color = FN.muted
                )
            }

            entries.forEach { entry ->
                MileageRow(entry)
            }
        }
    }

    if (showAdd) {
        AddMileageDialog(
            onDismiss = { showAdd = false },
            onCreate = { showAdd = false }
        )
    }
}

@Composable
private fun SummaryStat(value: String, label: String) {
    Column(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(value, style = FT.cardTitleSmall.copy(fontSize = 14.sp), color = FN.onStrong)
        Text(label, style = FT.monoTiny, color = FN.onStrong.copy(alpha = 0.5f))
    }
}

@Composable
private fun MileageRow(entry: com.fieldnotes.app.data.zone.MileageEntry) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = FN.surface,
        border = BorderStroke(1.dp, FN.line),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    entry.date.dayOfMonth.toString(),
                    style = FT.statNumber.copy(fontSize = 18.sp),
                    color = FN.text
                )
                Text(
                    entry.date.month.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH).uppercase(),
                    style = FT.monoBadge,
                    color = FN.muted
                )
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    entry.vehicle,
                    style = FT.cardTitle.copy(fontSize = 13.sp),
                    color = FN.text
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ColoredDot(FN.accent, size = 5.dp)
                    Text(
                        "${entry.startOdo} → ${entry.endOdo} km",
                        style = FT.monoTiny,
                        color = FN.muted
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "${String.format("%.1f", entry.litres)} L",
                        style = FT.monoTiny,
                        color = FN.muted
                    )
                    Text(
                        "₹${String.format("%.0f", entry.totalPaid)}",
                        style = FT.monoTiny,
                        color = FN.muted
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = FN.butter
            ) {
                Text(
                    String.format("%.1f", entry.kmPerL),
                    style = FT.cardTitleSmall.copy(fontSize = 15.sp),
                    color = FN.inkFixed,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AddMileageDialog(
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    var vehicle by remember { mutableStateOf("") }
    var startOdo by remember { mutableStateOf("") }
    var endOdo by remember { mutableStateOf("") }
    var pricePerL by remember { mutableStateOf("") }
    var totalPaid by remember { mutableStateOf("") }

    fun num(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

    val startV = num(startOdo)
    val endV = num(endOdo)
    val priceV = num(pricePerL)
    val paidV = num(totalPaid)
    val valid = vehicle.isNotBlank() &&
        startV != null && endV != null && endV > startV &&
        priceV != null && priceV > 0 && paidV != null && paidV > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log fill-up", style = FT.sectionTitle, color = FN.text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogField("Vehicle", vehicle, { vehicle = it }, KeyboardType.Text)
                DialogField("Starting odometer (km)", startOdo, { startOdo = it }, KeyboardType.Number)
                DialogField("Current odometer (km)", endOdo, { endOdo = it }, KeyboardType.Number)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        DialogField("Price / L", pricePerL, { pricePerL = it }, KeyboardType.Decimal)
                    }
                    Box(Modifier.weight(1f)) {
                        DialogField("Total paid", totalPaid, { totalPaid = it }, KeyboardType.Decimal)
                    }
                }
                if (startV != null && endV != null && endV <= startV) {
                    Text(
                        "Current odometer must be above the starting one.",
                        style = FT.bodySmall.copy(fontSize = 11.sp),
                        color = FN.accent
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (valid) {
                        val litres = paidV!! / priceV!!
                        ZoneData.mileage.add(
                            MileageEntry(
                                vehicle = vehicle.trim(),
                                date = LocalDate.now(),
                                startOdo = startV!!.toInt(),
                                endOdo = endV!!.toInt(),
                                litres = litres,
                                totalPaid = paidV
                            )
                        )
                        onCreate()
                    }
                },
                enabled = valid
            ) {
                Text("Log it", color = if (valid) FN.text else FN.muted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = FN.muted) }
        }
    )
}

@Composable
private fun DialogField(
    hint: String,
    value: String,
    onValue: (String) -> Unit,
    keyboardType: KeyboardType
) {
    BasicTextField(
        value = value,
        onValueChange = onValue,
        textStyle = FT.bodySmall.copy(color = FN.text),
        cursorBrush = SolidColor(FN.accent),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
