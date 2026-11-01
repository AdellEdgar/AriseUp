package io.github.adelledgar.ariseup.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.adelledgar.ariseup.ui.theme.LocalSystemPalette
import io.github.adelledgar.ariseup.ui.theme.SystemColors

val SystemShape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)

/** Contorno que resplandece, como los paneles del Sistema. */
fun Modifier.systemGlow(color: Color, radius: Dp = 10.dp, alpha: Float = 0.55f): Modifier = drawBehind {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 2.dp.toPx()
        this.color = color.copy(alpha = alpha).toArgb()
        maskFilter = BlurMaskFilter(radius.toPx(), BlurMaskFilter.Blur.NORMAL)
    }
    drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
}

/**
 * Ventana del Sistema: panel semitransparente con borde cian, encabezado entre corchetes
 * y un barrido de escaneo al aparecer.
 */
@Composable
fun SystemWindow(
    title: String,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    accent: Color = LocalSystemPalette.current.accent,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
    Box(
        modifier
            .fillMaxWidth()
            .systemGlow(accent)
            .background(Brush.verticalGradient(listOf(Color(0xE60D1A33), SystemColors.panel)), SystemShape)
            .border(1.dp, accent.copy(alpha = 0.9f), SystemShape)
            .drawWithContent {
                val h = size.height * reveal.value
                clipRect(bottom = h) { this@drawWithContent.drawContent() }
                if (reveal.value < 1f) {
                    drawLine(accent, Offset(0f, h), Offset(size.width, h), strokeWidth = 2.dp.toPx())
                }
            },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "[$title]",
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    modifier = Modifier.weight(1f),
                )
                if (onClose != null) {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = accent) }
                }
            }
            Box(
                Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.8f), Color.Transparent)))
                    .padding(top = 1.dp),
            )
            content()
        }
    }
}
