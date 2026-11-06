package com.lumera.app.ui.profiles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.lumera.app.ui.addons.VoidButton
import kotlinx.coroutines.delay

@Composable
fun WizardAgeGroupStep(selected: String?, onSelect: (String) -> Unit, onBack: () -> Unit) {
    val choices = listOf("under12" to "Under 12 — U / PG", "12to14" to "12–14 — up to UK 12 / 12A", "15to17" to "15–17 — up to UK 15", "adult" to "18+ (Adult)")
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(100); runCatching { first.requestFocus() } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Choose an age group")
        Text("Under-18 profiles block titles without verified UK ratings. A parent PIN protects changes.")
        Spacer(Modifier.height(16.dp))
        choices.forEachIndexed { index, (id, label) ->
            VoidButton(if (selected == id) "$label ✓" else label, { onSelect(id) },
                Modifier.widthIn(max = 400.dp).fillMaxWidth(),
                focusRequester = if (index == 0) first else null)
            Spacer(Modifier.height(8.dp))
        }
        VoidButton("Back", onBack, Modifier.widthIn(max = 400.dp).fillMaxWidth())
    }
}
