package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.Sex

/** Parámetros de la silueta del Jugador; [fat] y [muscle] van de 0 a 1. */
data class BodyShape(val fat: Double, val muscle: Double, val heightScale: Double, val sex: Sex)

/** Spec §4.5. */
object BodyShapeModel {
    fun bmi(weightKg: Double, heightCm: Double): Double {
        val m = heightCm / 100
        return weightKg / (m * m)
    }

    /** Fórmula de Deurenberg. */
    fun estimatedBodyFat(bmi: Double, age: Int, sex: Sex): Double =
        1.2 * bmi + 0.23 * age - 10.8 * (if (sex == Sex.MALE) 1 else 0) - 5.4

    fun shape(sex: Sex, age: Int, heightCm: Double, weightKg: Double, bodyFatPct: Double?, fue: Double): BodyShape {
        val fatPct = bodyFatPct ?: estimatedBodyFat(bmi(weightKg, heightCm), age, sex)
        val (lo, hi) = if (sex == Sex.MALE) 8.0 to 40.0 else 15.0 to 48.0
        return BodyShape(
            fat = ((fatPct - lo) / (hi - lo)).coerceIn(0.0, 1.0),
            muscle = ((fue - 5) / 95).coerceIn(0.0, 1.0),
            heightScale = 0.92 + 0.16 * ((heightCm - 150) / 50).coerceIn(0.0, 1.0),
            sex = sex,
        )
    }
}
