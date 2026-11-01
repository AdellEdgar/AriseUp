package io.github.adelledgar.ariseup.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.adelledgar.ariseup.ui.theme.LocalSystemPalette
import io.github.adelledgar.ariseup.ui.theme.SystemColors
import kotlinx.coroutines.delay

/** Texto que aparece letra a letra, como los mensajes del Sistema. */
@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    charDelayMs: Long = 25,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = SystemColors.text,
) {
    var shown by remember(text) { mutableIntStateOf(0) }
    LaunchedEffect(text) {
        while (shown < text.length) {
            delay(charDelayMs)
            shown++
        }
    }
    Text(text.take(shown), modifier = modifier, style = style, color = color)
}

/** Barra de progreso con el estilo del Sistema. */
@Composable
fun StatBar(
    label: String,
    value: Float,
    max: Float,
    modifier: Modifier = Modifier,
    valueText: String = "${value.toInt()}/${max.toInt()}",
    color: Color = LocalSystemPalette.current.accent,
) {
    val fraction by animateFloatAsState((if (max <= 0f) 0f else value / max).coerceIn(0f, 1f), tween(700), label = "bar")
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = SystemColors.text, modifier = Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = color)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0x33FFFFFF))
                .border(1.dp, color.copy(alpha = 0.4f)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(color),
            )
        }
    }
}

/** Botón del Sistema: borde de acento y esquinas cortadas. */
@Composable
fun SystemButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = LocalSystemPalette.current.accent) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = CutCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) color else SystemColors.muted),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** Fila etiqueta-valor. */
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = SystemColors.text) {
    Row(modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SystemColors.muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(value, color = valueColor, style = MaterialTheme.typography.bodyMedium)
    }
}
