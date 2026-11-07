package com.lumera.app.ui.profiles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.lumera.app.ui.addons.VoidButton
import kotlinx.coroutines.delay

@Composable
fun WizardAgeGroupStep(selected: String?, pinProtected: Boolean, hasRatingKey: Boolean,
    onContinue: (String, Boolean, String) -> Unit, onBack: () -> Unit) {
    var chosenAge by remember { mutableStateOf(selected) }
    var locked by remember { mutableStateOf(pinProtected) }
    var showRatingSetup by remember { mutableStateOf(false) }
    var ratingKey by remember { mutableStateOf("") }
    val choices = listOf("under12" to "Under 12 — U / PG", "12to14" to "12–14 — up to UK 12 / 12A", "15to17" to "15–17 — up to UK 15", "adult" to "18+ (Adult)")
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(100); runCatching { first.requestFocus() } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Choose an age group")
        Text("Age limits apply whether or not you choose a PIN.")
        Spacer(Modifier.height(16.dp))
        choices.forEachIndexed { index, (id, label) ->
            VoidButton(if (chosenAge == id) "$label ✓" else label, { chosenAge = id },
                Modifier.widthIn(max = 400.dp).fillMaxWidth(),
                focusRequester = if (index == 0) first else null)
            Spacer(Modifier.height(8.dp))
        }
        VoidButton("PIN to open profile: ${if (locked) "ON" else "OFF"}", { locked = !locked },
            Modifier.widthIn(max = 400.dp).fillMaxWidth())
        Text("When any profile uses a PIN, the parent PIN also protects profile changes.")
        if (chosenAge != null && chosenAge != "adult") {
            Text(if (hasRatingKey) "Titles without verified UK ratings are blocked." else
                "A TMDB key is needed for UK ratings. Until configured, unverified titles are blocked.")
            VoidButton("Ratings setup", { showRatingSetup = !showRatingSetup }, Modifier.widthIn(max = 400.dp).fillMaxWidth())
            if (showRatingSetup) OutlinedTextField(ratingKey, { ratingKey = it },
                label = { Text("TMDB key (blank keeps current)") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth())
        }
        Spacer(Modifier.height(8.dp))
        VoidButton("Continue", { chosenAge?.let { onContinue(it, locked, ratingKey) } },
            Modifier.widthIn(max = 400.dp).fillMaxWidth(), enabled = chosenAge != null)
        Spacer(Modifier.height(8.dp))
        VoidButton("Back", onBack, Modifier.widthIn(max = 400.dp).fillMaxWidth())
    }
}
