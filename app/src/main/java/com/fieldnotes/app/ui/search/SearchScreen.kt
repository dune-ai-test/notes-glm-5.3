@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.fieldnotes.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.fieldnotes.app.data.db.NoteWithTags
import com.fieldnotes.app.data.repo.NoteRepository
import com.fieldnotes.app.data.repo.NoteSort
import com.fieldnotes.app.data.repo.filterNotes
import com.fieldnotes.app.di.LocalAppContainer
import com.fieldnotes.app.ui.components.CircleIconButton
import com.fieldnotes.app.ui.components.ListRowNote
import com.fieldnotes.app.ui.components.SectionLabel
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(repo: NoteRepository) : ViewModel() {

    val query = MutableStateFlow("")
    val results: StateFlow<List<NoteWithTags>> = combine(
        repo.activeNotes, query
    ) { notes, q ->
        filterNotes(notes, q, NoteSort.RECENT, pinnedOnly = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }

}

@Composable
fun SearchScreen(navController: NavHostController) {
    val container = LocalAppContainer.current
    val vm: SearchViewModel = viewModel(
        factory = viewModelFactory { initializer { SearchViewModel(container.noteRepository) } }
    )
    val results by vm.results.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        focusRequester.requestFocus()
    }

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
            Surface(
                shape = CircleShape,
                color = FN.surface,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = FN.muted,
                        modifier = Modifier.size(17.dp)
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = vm::setQuery,
                        textStyle = FT.body.copy(color = FN.text, fontSize = 14.sp),
                        cursorBrush = SolidColor(FN.accent),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        decorationBox = { inner ->
                            Box {
                                if (query.isEmpty()) {
                                    Text(
                                        "Search notes…",
                                        style = FT.body.copy(fontSize = 14.sp),
                                        color = FN.muted
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionLabel("${results.size} results", color = FN.muted)
        }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 4.dp, bottom = 40.dp)
        ) {
            if (results.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 70.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (query.isBlank()) "Type to search your notes." else "No matches found.",
                            style = FT.bodySmall,
                            color = FN.muted
                        )
                    }
                }
            }
            items(results, key = { it.note.id }) { entry ->
                ListRowNote(
                    entry = entry,
                    dotColor = FN.accent,
                    onOpen = { navController.navigate("editor/${entry.note.id}") },
                    onLongPress = { }
                )
            }
        }
    }
}
