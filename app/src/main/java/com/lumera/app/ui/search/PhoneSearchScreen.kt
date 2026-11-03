package com.lumera.app.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lumera.app.data.model.stremio.MetaItem
import com.lumera.app.ui.home.PhoneContentRow

@Composable
fun PhoneSearchScreen(state: SearchViewModel.SearchState, onQueryChange: (String) -> Unit, onMovieClick: (MetaItem) -> Unit) {
    Column(Modifier.fillMaxSize().imePadding()) {
        OutlinedTextField(value = state.query, onValueChange = onQueryChange, singleLine = true,
            label = { Text("Search films and TV") }, modifier = Modifier.fillMaxWidth().padding(16.dp))
        if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(Modifier.weight(1f)) {
            if (state.query.length < 3) item {
                Text("Enter at least 3 characters to search your configured services.", modifier = Modifier.padding(16.dp))
            }
            if (state.movies.isNotEmpty()) item { PhoneContentRow("Films", state.movies, onMovieClick) }
            if (state.series.isNotEmpty()) item { PhoneContentRow("TV series", state.series, onMovieClick) }
            if (!state.isLoading && state.query.length >= 3 && state.results.isEmpty()) item {
                Text("No results for “${state.query}”", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
