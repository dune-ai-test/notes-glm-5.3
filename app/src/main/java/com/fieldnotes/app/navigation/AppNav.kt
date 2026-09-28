package com.fieldnotes.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fieldnotes.app.di.AppContainer
import com.fieldnotes.app.ui.components.PillBottomBar
import com.fieldnotes.app.ui.components.TopLevelTab
import com.fieldnotes.app.ui.editor.EditorScreen
import com.fieldnotes.app.ui.home.HomeScreen
import com.fieldnotes.app.ui.library.FolderScreen
import com.fieldnotes.app.ui.library.LibraryScreen
import com.fieldnotes.app.ui.quick.ReminderScreen
import com.fieldnotes.app.ui.search.SearchScreen
import com.fieldnotes.app.ui.settings.SettingsScreen
import com.fieldnotes.app.ui.zone.ZoneScreen
import com.fieldnotes.app.ui.trash.TrashScreen
import com.fieldnotes.app.ui.theme.FN
import kotlinx.coroutines.launch

@Composable
fun AppRoot(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val pagerState = rememberPagerState(initialPage = TopLevelTab.HOME.ordinal) {
        TopLevelTab.entries.size
    }
    val scope = rememberCoroutineScope()

    // Opens the editor with an unsaved draft (noteId <= 0). The database row is
    // only created when the user actually types something.
    val createAndOpenNote: () -> Unit = {
        navController.navigate("editor/-1")
    }

    val switchTab: (Int) -> Unit = { index ->
        scope.launch { pagerState.animateScrollToPage(index) }
    }

    // Notification taps: open the linked note once, then clear.
    LaunchedEffect(Unit) {
        container.pendingOpenNoteId.collect { noteId ->
            if (noteId != null && noteId > 0) {
                navController.navigate("editor/$noteId")
                container.pendingOpenNoteId.value = null
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(FN.bg)
    ) {
        NavHost(navController = navController, startDestination = "tabs") {
            composable("tabs") {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = TopLevelTab.entries.size - 1
                ) { page ->
                    when (TopLevelTab.entries[page]) {
                        TopLevelTab.HOME ->
                            HomeScreen(
                                navController = navController,
                                onOpenSettings = { switchTab(TopLevelTab.SETTINGS.ordinal) }
                            )
                        TopLevelTab.ZONE ->
                            ZoneScreen()
                        TopLevelTab.REMINDER ->
                            ReminderScreen(
                                navController = navController,
                                onCreateNote = createAndOpenNote
                            )
                        TopLevelTab.LIBRARY ->
                            LibraryScreen(
                                navController = navController,
                                onCreateNote = createAndOpenNote
                            )
                        TopLevelTab.SETTINGS -> SettingsScreen(navController = navController)
                    }
                }
            }
            composable("search") {
                SearchScreen(navController = navController)
            }
            composable("trash") {
                TrashScreen(navController = navController)
            }
            composable(
                route = "folder/{folderId}",
                arguments = listOf(navArgument("folderId") { type = NavType.LongType })
            ) { entry ->
                val folderId = entry.arguments?.getLong("folderId") ?: 1L
                FolderScreen(folderId = folderId, navController = navController)
            }
            composable(
                route = "editor/{noteId}?folderId={folderId}",
                arguments = listOf(
                    navArgument("noteId") { type = NavType.LongType },
                    navArgument("folderId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { entry ->
                val noteId = entry.arguments?.getLong("noteId") ?: 0L
                val folderId = entry.arguments?.getLong("folderId") ?: -1L
                EditorScreen(
                    noteId = noteId,
                    folderIdHint = folderId,
                    navController = navController
                )
            }
        }

        if (currentRoute == "tabs") {
            PillBottomBar(
                selectedTab = pagerState.targetPage,
                onSelect = switchTab,
                onFab = createAndOpenNote,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            )
        }
    }
}
