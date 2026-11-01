package io.github.adelledgar.ariseup.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.adelledgar.ariseup.domain.BodyShape
import io.github.adelledgar.ariseup.domain.model.Rank
import io.github.adelledgar.ariseup.ui.theme.SystemColors
import kotlin.math.sin
import kotlin.random.Random

enum class AvatarMode { NORMAL, PENALTY, REST, RANK_UP }

private fun particlesFor(rank: Rank) = when (rank) {
    Rank.E -> 0
    Rank.D -> 6
    Rank.C -> 12
    Rank.B -> 20
    Rank.A -> 30
    Rank.S -> 40
}

/** La silueta de sombra del Jugador (spec §4.5). */
@Composable
fun ShadowAvatar(shape: BodyShape, mode: AvatarMode, rank: Rank, modifier: Modifier = Modifier, ghost: BodyShape? = null) {
    val fat by animateFloatAsState(shape.fat.toFloat(), tween(1200), label = "fat")
    val muscle by animateFloatAsState(shape.muscle.toFloat(), tween(1200), label = "muscle")
    val height by animateFloatAsState(shape.heightScale.toFloat(), tween(1200), label = "height")
    val loop = rememberInfiniteTransition(label = "avatar")
    val breath by loop.animateFloat(0.99f, 1.01f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "breath")
    val time by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "time")
    val flicker by loop.animateFloat(0.35f, 1f, infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "flicker")

    val color = when (mode) {
        AvatarMode.PENALTY -> SystemColors.danger
        else -> SystemColors.accent
    }
    val alpha = when (mode) {
        AvatarMode.REST -> 0.5f
        AvatarMode.PENALTY -> flicker
        else -> 1f
    }
    val seeds = remember(rank) { List(particlesFor(rank) + if (mode == AvatarMode.RANK_UP) 30 else 0) { Random(it).nextFloat() } }
    val animated = shape.copy(fat = fat.toDouble(), muscle = muscle.toDouble(), heightScale = height.toDouble())

    Canvas(modifier.semantics { contentDescription = "Silueta del Jugador" }) {
        val figureH = size.height * 0.9f
        val figureW = figureH * 0.62f
        val origin = Offset((size.width - figureW) / 2, size.height * 0.04f)

        fun toPath(s: BodyShape): Path {
            val pts = AvatarGeometry.outline(s, hunched = mode == AvatarMode.PENALTY).map { Offset(origin.x + it.x * figureW, origin.y + it.y * figureH / 1.08f) }
            return smoothPath(pts)
        }

        // Aura de partículas que ascienden
        seeds.forEachIndexed { i, seed ->
            val phase = (time + seed) % 1f
            val x = origin.x + figureW * (0.2f + 0.6f * ((seed * 7.3f) % 1f)) + sin((phase + i) * 6.28f) * 6f
            val y = origin.y + figureH * (1f - phase)
            drawCircle(color.copy(alpha = (1f - phase) * 0.6f * alpha), radius = 2f + 3f * seed, center = Offset(x, y))
        }

        ghost?.let {
            drawPath(toPath(it), SystemColors.muted.copy(alpha = 0.45f), style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))))
        }

        scale(breath, pivot = Offset(size.width / 2, origin.y + figureH)) {
            val path = toPath(animated)
            glow(path, color.copy(alpha = alpha))
            drawPath(path, Brush.verticalGradient(listOf(Color(0xFF0E1A33), Color(0xFF02040A)), startY = origin.y, endY = origin.y + figureH), alpha = 0.95f)
            drawPath(path, color.copy(alpha = alpha), style = Stroke(width = 2.5f))
        }
    }
}

private fun DrawScope.glow(path: Path, color: Color) {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 10f
        this.color = color.copy(alpha = color.alpha * 0.7f).toArgb()
        maskFilter = BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL)
    }
    drawContext.canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
}

/** Curva suave que pasa por los puntos medios de cada tramo. */
private fun smoothPath(pts: List<Offset>): Path = Path().apply {
    if (pts.isEmpty()) return@apply
    val first = (pts.last() + pts.first()) / 2f
    moveTo(first.x, first.y)
    for (i in pts.indices) {
        val p = pts[i]
        val next = pts[(i + 1) % pts.size]
        val mid = (p + next) / 2f
        quadraticTo(p.x, p.y, mid.x, mid.y)
    }
    close()
}
