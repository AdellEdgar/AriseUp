package io.github.adelledgar.ariseup.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.adelledgar.ariseup.ui.theme.LocalSystemPalette

/** ViewModel con dependencias del contenedor, sin fábricas a mano. */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(key: String? = null, crossinline create: () -> VM): VM =
    viewModel(key = key, initializer = { create() })

/** Columna desplazable estándar de las pantallas. */
@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
fun ScreenTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, color = LocalSystemPalette.current.accent)
}
