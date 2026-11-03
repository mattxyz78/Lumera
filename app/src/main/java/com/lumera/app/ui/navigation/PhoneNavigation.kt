package com.lumera.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lumera.app.data.model.ProfileEntity

@Composable
fun PhoneNavigation(
    current: NavDestination,
    profile: ProfileEntity?,
    requesters: Map<NavDestination, FocusRequester>,
    onNavigate: (NavDestination) -> Unit,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onLogout) {
                Text(profile?.name ?: "Profiles", maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp))
            }
            TextButton(onClick = { onNavigate(NavDestination.Settings) },
                modifier = Modifier.focusRequester(requesters.getValue(NavDestination.Settings))) { Text("Settings") }
        }
        // All destinations stay reachable on narrow screens and at large font sizes.
        LazyRow(contentPadding = PaddingValues(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(NavDestination.Home, NavDestination.Movies, NavDestination.Series,
                NavDestination.Watchlist, NavDestination.Search)) { destination ->
                FilterChip(selected = current == destination, onClick = { onNavigate(destination) },
                    modifier = Modifier.focusRequester(requesters.getValue(destination)),
                    label = { Text(destination.label) })
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
    }
}
