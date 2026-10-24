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
import androidx.compose.ui.text.input.ImeAction
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
import com.fieldnotes.app.data.repo.SettingsRepository
import com.fieldnotes.app.data.repo.NoteSort
import com.fieldnotes.app.data.repo.filterNotes
import com.fieldnotes.app.di.LocalAppContainer
import com.fieldnotes.app.util.plural
import com.fieldnotes.app.ui.components.CircleIconButton
import com.fieldnotes.app.ui.components.ListRowNote
import com.fieldnotes.app.ui.components.PillChip
import com.fieldnotes.app.ui.components.SectionLabel
import com.fieldnotes.app.ui.theme.FN
import com.fieldnotes.app.ui.theme.FT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repo: NoteRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    val query = MutableStateFlow("")
    val recentSearches: StateFlow<List<String>> = settings.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val results: StateFlow<List<NoteWithTags>> = combine(
        repo.activeNotes, query
    ) { notes, q ->
        filterNotes(notes, q, NoteSort.RECENT, pinnedOnly = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        query.value = value
    }

    /** Records a committed query in the recent-searches list. */
    fun commitQuery(value: String) {
        viewModelScope.launch { settings.addRecentSearch(value) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { settings.clearRecentSearches() }
    }
}

@Composable
fun SearchScreen(navController: NavHostController) {
    val container = LocalAppContainer.current
    val vm: SearchViewModel = viewModel(
        factory = viewModelFactory { initializer { SearchViewModel(container.noteRepository, container.settingsRepository) } }
    )
    val results by vm.results.collectAsStateWithLifecycle()
    val recentSearches by vm.recentSearches.collectAsStateWithLifecycle()
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
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = { vm.commitQuery(query) }
                        ),
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
            if (query.isBlank()) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionLabel("Recent searches", color = FN.muted)
                    if (recentSearches.isNotEmpty()) {
                        Text(
                            "Clear",
                            style = FT.chipSmall,
                            color = FN.accent,
                            modifier = Modifier.clickable { vm.clearRecentSearches() }
                        )
                    }
                }
                if (recentSearches.isEmpty()) {
                    Text(
                        "Recent searches will appear here.",
                        style = FT.bodySmall,
                        color = FN.muted
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        recentSearches.forEach { past ->
                            PillChip(
                                label = past,
                                selected = false,
                                onClick = {
                                    vm.setQuery(past)
                                    vm.commitQuery(past)
                                }
                            )
                        }
                    }
                }
            } else {
                SectionLabel(
                    "${results.size} ${plural(results.size, "result", "results")}",
                    color = FN.muted
                )
            }
        }

        if (query.isBlank()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = FN.dotGray,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    "Search your notes",
                    style = FT.sectionTitle,
                    color = FN.text,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    "Titles and contents, across every folder.",
                    style = FT.bodySmall,
                    color = FN.muted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
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
                            "No matches found.",
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
                    onLongPress = { },
                    highlight = query
                )
            }
        }
        }
    }
}
