package com.fieldnotes.app.ui.zone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT

private fun toDouble(s: String): Double? =
    s.trim().replace(',', '.').toDoubleOrNull()

/**
 * Mileage calculator — live, nothing persisted.
 * mileage (km/l) = (current − starting odometer) / (total paid ÷ price per litre)
 */
@Composable
fun MileageToolCard(modifier: Modifier = Modifier) {
    var vehicle by remember { mutableStateOf("") }
    var startOdo by remember { mutableStateOf("") }
    var currentOdo by remember { mutableStateOf("") }
    var pricePerL by remember { mutableStateOf("") }
    var totalPaid by remember { mutableStateOf("") }

    val startV = toDouble(startOdo)
    val currentV = toDouble(currentOdo)
    val priceV = toDouble(pricePerL)
    val paidV = toDouble(totalPaid)

    val distance = if (startV != null && currentV != null && currentV > startV) currentV - startV else null
    val litres = if (priceV != null && paidV != null && priceV > 0) paidV / priceV else null
    val mileage = if (distance != null && litres != null && litres > 0) distance / litres else null
    val costPerKm = if (distance != null && distance > 0 && paidV != null) paidV / distance else null
    val hasResult = mileage != null

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = FN.inkCard,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(FN.butter)
                    )
                    Text("MILEAGE", style = FT.monoBadge, color = FN.onStrong)
                }
                Icon(
                    Icons.Outlined.RestartAlt,
                    contentDescription = "Reset",
                    tint = FN.onStrong.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            vehicle = ""
                            startOdo = ""
                            currentOdo = ""
                            pricePerL = ""
                            totalPaid = ""
                        }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MileageField("Vehicle", vehicle, { vehicle = it }, keyboardType = null)
                MileageField("Starting odometer (km)", startOdo, { startOdo = it }, KeyboardType.Number)
                MileageField("Current odometer (km)", currentOdo, { currentOdo = it }, KeyboardType.Number)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        MileageField("Fuel price / L", pricePerL, { pricePerL = it }, KeyboardType.Decimal)
                    }
                    Box(Modifier.weight(1f)) {
                        MileageField("Total paid", totalPaid, { totalPaid = it }, KeyboardType.Decimal)
                    }
                }
            }

            if (hasResult && distance != null && litres != null) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.06f)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = String.format("%.1f", mileage),
                                style = FT.statNumber.copy(fontSize = 40.sp),
                                color = FN.butter
                            )
                            Text(
                                "km / L",
                                style = FT.chip,
                                color = FN.onStrong.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (vehicle.isNotBlank()) {
                                Text(
                                    vehicle,
                                    style = FT.monoTiny,
                                    color = FN.onStrong.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            StatChip("${String.format("%.0f", distance)} km", "distance")
                            StatChip("${String.format("%.1f", litres)} L", "fuel")
                            if (costPerKm != null) {
                                StatChip(String.format("%.2f", costPerKm), "per km")
                            }
                        }
                    }
                }
            } else {
                Text(
                    "Enter the odometer before and after a fill, the fuel price and what you paid — mileage updates live.",
                    style = FT.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                    color = FN.onStrong.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Column(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(value, style = FT.cardTitleSmall.copy(fontSize = 13.sp), color = FN.onStrong)
        Text(label, style = FT.monoTiny, color = FN.onStrong.copy(alpha = 0.5f))
    }
}

@Composable
private fun MileageField(
    hint: String,
    value: String,
    onValue: (String) -> Unit,
    keyboardType: KeyboardType?
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.08f)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValue,
            textStyle = FT.bodySmall.copy(color = FN.onStrong),
            cursorBrush = SolidColor(FN.butter),
            singleLine = true,
            keyboardOptions = if (keyboardType != null) {
                KeyboardOptions(keyboardType = keyboardType)
            } else {
                KeyboardOptions.Default
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(hint, style = FT.bodySmall, color = FN.onStrong.copy(alpha = 0.4f))
                    }
                    inner()
                }
            }
        )
    }
}
