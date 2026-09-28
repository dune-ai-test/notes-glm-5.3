package com.fieldnotes.app.ui.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector

/** All folder icon choices, keyed for storage in FolderEntity.iconKey. */
val FolderIconChoices: List<Pair<String, ImageVector>> = listOf(
    "work" to Icons.Outlined.Work,
    "heart" to Icons.Outlined.FavoriteBorder,
    "bulb" to Icons.Outlined.Lightbulb,
    "archive" to Icons.Outlined.Archive,
    "shopping" to Icons.Outlined.ShoppingCart,
    "food" to Icons.Outlined.Restaurant,
    "travel" to Icons.Outlined.Flight,
    "study" to Icons.Outlined.School,
    "music" to Icons.Outlined.MusicNote,
    "book" to Icons.Outlined.MenuBook,
    "photo" to Icons.Outlined.PhotoCamera,
    "pets" to Icons.Outlined.Pets,
    "car" to Icons.Outlined.DirectionsCar,
    "fitness" to Icons.Outlined.FitnessCenter,
    "games" to Icons.Outlined.SportsEsports,
    "money" to Icons.Outlined.AccountBalance,
    "star" to Icons.Outlined.StarBorder
)

fun folderIcon(iconKey: String): ImageVector =
    FolderIconChoices.firstOrNull { it.first == iconKey }?.second ?: Icons.Outlined.Work
