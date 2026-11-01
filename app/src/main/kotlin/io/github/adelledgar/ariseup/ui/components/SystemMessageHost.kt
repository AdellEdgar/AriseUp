package io.github.adelledgar.ariseup.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.adelledgar.ariseup.ui.missions.SystemMessage
import io.github.adelledgar.ariseup.ui.theme.SystemColors
import kotlinx.coroutines.flow.Flow

/** Muestra en cola las ventanas del Sistema que emite un ViewModel, con su sonido. */
@Composable
fun SystemMessageHost(messages: Flow<SystemMessage>) {
    val queue = remember { mutableStateListOf<SystemMessage>() }
    LaunchedEffect(messages) { messages.collect { queue += it } }
    val current = queue.firstOrNull() ?: return
    LaunchedEffect(current) {
        when {
            current.error -> SystemSounds.alert()
            current.levelUp -> SystemSounds.levelUp()
            else -> SystemSounds.ding()
        }
    }
    Dialog(onDismissRequest = { queue.removeAt(0) }) {
        SystemWindow(current.title, accent = when {
            current.error -> SystemColors.danger
            current.levelUp -> SystemColors.gold
            else -> SystemColors.accent
        }) {
            current.lines.forEach { TypewriterText(it, style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(12.dp))
            SystemButton("Aceptar", { queue.removeAt(0) }, Modifier.fillMaxWidth())
        }
    }
}
