package com.fieldnotes.app.ui.zone

import androidx.compose.runtime.mutableStateListOf
import java.time.LocalDate

data class MileageEntry(
    val vehicle: String,
    val date: LocalDate,
    val startOdo: Int,
    val endOdo: Int,
    val litres: Double,
    val totalPaid: Double
) {
    val distance: Int get() = (endOdo - startOdo).coerceAtLeast(0)
    val kmPerL: Double get() = if (litres > 0) distance / litres else 0.0
    val costPerKm: Double get() = if (distance > 0) totalPaid / distance else 0.0
}

data class VaultEntry(
    val title: String,
    val username: String,
    val password: String
)

/**
 * In-memory demo store for the Zone tools — shared across screens so hub
 * counts and histories stay in sync. Nothing persists yet; wiring this to
 * Room comes later.
 */
object ZoneData {
    val mileage = mutableStateListOf(
        MileageEntry("Activa 125", LocalDate.now().minusDays(4), 8610, 8875, 6.1, 647.0),
        MileageEntry("Swift", LocalDate.now().minusDays(2), 34420, 34700, 24.1, 2555.0),
        MileageEntry("Activa 125", LocalDate.now().minusDays(30), 8390, 8610, 5.2, 551.0),
        MileageEntry("Swift", LocalDate.now().minusDays(20), 34050, 34420, 31.5, 3339.0),
        MileageEntry("Activa 125", LocalDate.now().minusDays(64), 8120, 8390, 4.1, 640.0)
    )

    val vault = mutableStateListOf(
        VaultEntry("Gmail", "me@gmail.com", "hb#2026!x"),
        VaultEntry("Home Wi-Fi", "TP-Link_5G", "butterfly-88"),
        VaultEntry("Bank portal", "maya.c", "S3cure!pass")
    )
}
