package com.lumera.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lumera.app.data.model.stremio.MetaItem
import com.lumera.app.domain.*
import com.lumera.app.ui.components.LumeraCard

@Composable
fun PhoneHomeScreen(
    state: HomeViewModel.HomeState,
    tab: DashboardTab,
    profileId: Int?,
    onMovieClick: (MetaItem) -> Unit,
    onViewMore: (String, List<MetaItem>, String) -> Unit,
    onHubClick: (HubItem) -> Unit
) {
    val screen = when(tab) { DashboardTab.HOME -> "home"; DashboardTab.MOVIES -> "movies"; DashboardTab.SERIES -> "series" }
    if (state.isLoading || state.loadedScreen != screen || state.loadedProfileId != profileId) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val history = remember(state.history, state.seriesNextUp) { buildContinueWatchingItems(state.history, state.seriesNextUp) }
    key(profileId, tab) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (history.isNotEmpty()) item("continue") { PhoneContentRow("Continue Watching", history, onMovieClick) }
            items(state.mixedRows, key = { it.id }) { row ->
                when (row) {
                    is CategoryRow -> PhoneContentRow(row.title, row.items, onMovieClick,
                        onMore = { onViewMore(row.title, row.items, row.id) })
                    is HubGroupRow -> Column {
                        Text(row.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                        LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(row.items) { hub -> HubCard(hubItem = hub, shape = row.shape, onClick = { onHubClick(hub) }) }
                        }
                    }
                }
            }
            if (state.mixedRows.isEmpty() && history.isEmpty()) item("empty") {
                Text("No content yet. Configure your services and add-ons in Settings.", modifier = Modifier.padding(24.dp))
            }
        }
    }
}

@Composable
fun PhoneContentRow(title: String, items: List<MetaItem>, onMovieClick: (MetaItem) -> Unit, onMore: (() -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (onMore != null) TextButton(onClick = onMore) { Text("See all") }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items) { movie ->
                Column(Modifier.width(140.dp)) {
                    LumeraCard(movie.name, movie.poster, onClick = { onMovieClick(movie) }, progress = movie.progress, hasNewEpisode = movie.hasNewEpisode)
                    Text(movie.name, maxLines = 2, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}
