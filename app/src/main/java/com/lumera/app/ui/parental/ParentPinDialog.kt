package com.lumera.app.ui.parental

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.lumera.app.data.parental.ParentPin
import kotlinx.coroutines.launch

@Composable
fun ParentPinDialog(controller: ParentPin) {
    val prompt by controller.prompt.collectAsState()
    val error by controller.error.collectAsState()
    val request = prompt ?: return
    val reason = request.reason
    var pin by remember(request.id) { mutableStateOf("") }
    var confirm by remember(request.id) { mutableStateOf("") }
    var ratingKey by remember(request.id) { mutableStateOf("") }
    var busy by remember(request.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val setup = !controller.configured
    val pinFocus = remember(request.id) { FocusRequester() }
    LaunchedEffect(request.id) { pinFocus.requestFocus() }
    AlertDialog(onDismissRequest = { if (!busy) controller.cancel() },
        title = { Text(if (setup) "Set up parent PIN" else "Parent PIN required") },
        text = { Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (setup) "Choose a parent PIN for profiles you lock and for profile changes while a lock is in use. Keep it private." else reason)
            OutlinedTextField(pin, { if (it.length <= 8) pin = it }, modifier = Modifier.focusRequester(pinFocus), label = { Text("PIN") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
            if (setup) OutlinedTextField(confirm, { if (it.length <= 8) confirm = it }, label = { Text("Confirm PIN") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
            Column {
                if (controller.ratingApiKey.isBlank()) Text("Child profiles block unrated content. A TMDB API key is needed to verify UK ratings.")
                OutlinedTextField(ratingKey, { ratingKey = it }, label = { Text("TMDB API key (blank keeps current)") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation())
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(enabled = !busy, onClick = { scope.launch { busy = true; try { controller.submit(pin, confirm, ratingKey) } finally { busy = false } } }) { Text("Continue") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { controller.cancel() }) { Text("Cancel") } })
}
