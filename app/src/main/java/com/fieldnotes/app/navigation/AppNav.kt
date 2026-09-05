package com.fieldnotes.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fieldnotes.app.di.AppContainer
import com.fieldnotes.app.ui.components.PillBottomBar
import com.fieldnotes.app.ui.components.topLevelRoutes
import com.fieldnotes.app.ui.editor.EditorScreen
import com.fieldnotes.app.ui.home.HomeScreen
import com.fieldnotes.app.ui.library.FolderScreen
import com.fieldnotes.app.ui.library.LibraryScreen
import com.fieldnotes.app.ui.quick.QuickScreen
import com.fieldnotes.app.ui.search.SearchScreen
import com.fieldnotes.app.ui.settings.SettingsScreen
import com.fieldnotes.app.ui.theme.WarmPaper
import kotlinx.coroutines.launch

@Composable
fun AppRoot(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val scope = rememberCoroutineScope()
    var fabBusy by remember { mutableStateOf(false) }

    val createAndOpenNote: () -> Unit = {
        if (!fabBusy) {
            fabBusy = true
            scope.launch {
                val id = container.noteRepository.createNote()
                container.settingsRepository.touchStreak(com.fieldnotes.app.util.TimeFormat.epochDay())
                fabBusy = false
                navController.navigate("editor/$id")
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(WarmPaper)
    ) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(navController = navController)
            }
            composable("quick") {
                QuickScreen(navController = navController, onCreateNote = createAndOpenNote)
            }
            composable("library") {
                LibraryScreen(navController = navController, onCreateNote = createAndOpenNote)
            }
            composable("settings") {
                SettingsScreen()
            }
            composable("search") {
                SearchScreen(navController = navController)
            }
            composable(
                route = "folder/{folderId}",
                arguments = listOf(navArgument("folderId") { type = NavType.LongType })
            ) { entry ->
                val folderId = entry.arguments?.getLong("folderId") ?: 1L
                FolderScreen(folderId = folderId, navController = navController)
            }
            composable(
                route = "editor/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
            ) { entry ->
                val noteId = entry.arguments?.getLong("noteId") ?: 0L
                EditorScreen(noteId = noteId, navController = navController)
            }
        }

        if (currentRoute in topLevelRoutes) {
            PillBottomBar(
                currentRoute = currentRoute,
                onSelect = { route ->
                    if (route != currentRoute) navController.navigateTopLevel(route)
                },
                onFab = createAndOpenNote,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            )
        }
    }
}

fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
