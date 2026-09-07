package com.fieldnotes.app.ui.quick

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.fieldnotes.app.ui.theme.WarmPaper

@Composable
fun QuickScreen(navController: NavHostController, onCreateNote: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(WarmPaper)
    )
}
