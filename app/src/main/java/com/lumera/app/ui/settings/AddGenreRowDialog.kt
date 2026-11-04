package com.lumera.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.lumera.app.data.repository.AddonRepository
import com.lumera.app.ui.addons.VoidButton
import com.lumera.app.ui.addons.VoidInput
import androidx.compose.ui.focus.focusRequester
import com.lumera.app.ui.addons.VoidDialog
import kotlinx.coroutines.delay

@Composable
fun AddGenreRowDialog(
    catalogs: List<AddonRepository.DiscoverCatalog>,
    adding: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onAdd: (AddonRepository.DiscoverCatalog, String) -> Unit
) {
    var selected by remember(catalogs) { mutableStateOf<AddonRepository.DiscoverCatalog?>(null) }
    val first = remember { FocusRequester() }
    val catalog = selected
    var customGenre by remember(catalog) { mutableStateOf("") }
    LaunchedEffect(catalog, catalogs) { delay(100); runCatching { first.requestFocus() } }
    VoidDialog(onDismissRequest = onDismiss,
        title = if (catalog == null) "Add genre row — choose source" else "Choose genre — ${catalog.catalogName}") {
        Text("Add a separate row for any genre offered by your installed sources.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        if (adding) {
            Text("Adding genre row…")
        } else if (catalogs.isEmpty()) {
            Text("No enabled source offers genre choices. Add or enable a genre-capable add-on in Settings.")
        } else if (catalog != null && catalog.genres.isEmpty()) {
            Text("This source accepts a genre name without publishing a list. Enter a genre it supports.")
            VoidInput(customGenre, { customGenre = it }, "Genre name", Modifier.fillMaxWidth().focusRequester(first))
            Spacer(Modifier.height(12.dp))
            VoidButton("Add genre row", { if (customGenre.isNotBlank()) onAdd(catalog, customGenre.trim()) }, Modifier.fillMaxWidth())
        } else {
            LazyColumn(Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (catalog == null) {
                    itemsIndexed(catalogs) { index, source ->
                        VoidButton("${source.addonName} · ${source.catalogName} · ${source.type}",
                            { selected = source }, Modifier.fillMaxWidth(),
                            focusRequester = if (index == 0) first else null)
                    }
                } else {
                    itemsIndexed(catalog.genres.distinct().sorted()) { index, genre ->
                        VoidButton(genre, { onAdd(catalog, genre) }, Modifier.fillMaxWidth(),
                            focusRequester = if (index == 0) first else null)
                    }
                }
            }
        }
        if (error != null) { Spacer(Modifier.height(8.dp)); Text(error, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(16.dp))
        if (!adding) {
            VoidButton(if (catalog == null) "Close" else "Back to sources",
                { if (catalog == null) onDismiss() else selected = null }, Modifier.fillMaxWidth(),
                focusRequester = if (catalogs.isEmpty()) first else null)
        }
    }
}
