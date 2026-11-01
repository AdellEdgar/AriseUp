package io.github.adelledgar.ariseup.ui.components

import androidx.compose.ui.geometry.Offset
import io.github.adelledgar.ariseup.domain.BodyShape
import io.github.adelledgar.ariseup.domain.model.Sex

/**
 * Contorno frontal de la silueta en coordenadas normalizadas (x e y de 0 a 1, centro en x = 0,5).
 * La mitad derecha son 24 puntos (de la coronilla a la entrepierna); la izquierda es su reflejo.
 * Cada punto se interpola entre cuatro complexiones según grasa y músculo (spec §4.5).
 */
object AvatarGeometry {
    const val SHOULDER = 4
    const val WAIST = 14
    const val HIP = 15

    // y, y distancia al centro para: delgado, con grasa, musculado, musculado con grasa
    private val Y = floatArrayOf(
        0.00f, 0.04f, 0.095f, 0.12f, 0.15f, 0.18f, 0.27f, 0.35f, 0.43f, 0.50f, 0.51f, 0.43f,
        0.21f, 0.25f, 0.36f, 0.45f, 0.55f, 0.72f, 0.82f, 0.95f, 0.99f, 0.995f, 0.72f, 0.54f,
    )
    private val LEAN = floatArrayOf(
        0f, .055f, .045f, .035f, .15f, .175f, .20f, .22f, .24f, .255f, .225f, .20f,
        .13f, .125f, .095f, .11f, .105f, .075f, .07f, .045f, .06f, .015f, .02f, 0f,
    )
    private val FAT = floatArrayOf(
        0f, .065f, .068f, .058f, .165f, .2f, .25f, .29f, .30f, .31f, .275f, .255f,
        .17f, .185f, .22f, .205f, .185f, .11f, .1f, .06f, .072f, .022f, .04f, 0f,
    )
    private val MUSCULAR = floatArrayOf(
        0f, .055f, .047f, .05f, .19f, .215f, .235f, .245f, .26f, .27f, .235f, .215f,
        .15f, .145f, .105f, .12f, .118f, .082f, .082f, .05f, .065f, .015f, .022f, 0f,
    )
    private val MUSCULAR_FAT = floatArrayOf(
        0f, .065f, .066f, .062f, .2f, .235f, .27f, .3f, .31f, .315f, .28f, .26f,
        .18f, .19f, .195f, .19f, .175f, .108f, .1f, .058f, .072f, .022f, .036f, 0f,
    )

    // Ajuste de proporciones por sexo (multiplica la distancia al centro)
    private val FEMALE = floatArrayOf(
        1f, .95f, .92f, .9f, .88f, .88f, .9f, .9f, .9f, .9f, .9f, .9f,
        .92f, .97f, .9f, 1.14f, 1.1f, 1.04f, 1f, .95f, .95f, 1f, 1f, 1f,
    )

    fun outline(shape: BodyShape, hunched: Boolean = false): List<Offset> {
        val f = shape.fat.toFloat().coerceIn(0f, 1f)
        val m = shape.muscle.toFloat().coerceIn(0f, 1f)
        val h = shape.heightScale.toFloat()
        val right = Y.indices.map { i ->
            val lean = lerp(LEAN[i], FAT[i], f)
            val strong = lerp(MUSCULAR[i], MUSCULAR_FAT[i], f)
            var dx = lerp(lean, strong, m) * if (shape.sex == Sex.FEMALE) FEMALE[i] else 1f
            var y = Y[i]
            if (hunched && i in 3..13) {
                y += if (i <= 5) 0.012f else 0.006f
                if (i in 4..5) dx *= 0.97f
            }
            Offset(0.5f + dx, y * h)
        }
        val left = right.reversed().map { Offset(1f - it.x, it.y) }
        return right + left
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}
