package io.github.adelledgar.ariseup.domain.model

import kotlinx.serialization.Serializable

/** Valores nutricionales (por 100 g/ml, o de una porción ya calculada). */
@Serializable
data class Macros(
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val sugars: Double,
    val fat: Double,
    val saturated: Double,
    val fiber: Double,
    val salt: Double,
) {
    operator fun plus(o: Macros) = Macros(
        kcal + o.kcal, protein + o.protein, carbs + o.carbs, sugars + o.sugars,
        fat + o.fat, saturated + o.saturated, fiber + o.fiber, salt + o.salt,
    )

    fun times(f: Double) = Macros(kcal * f, protein * f, carbs * f, sugars * f, fat * f, saturated * f, fiber * f, salt * f)

    companion object {
        val ZERO = Macros(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }
}

@Serializable
data class NutritionTargets(val kcal: Int, val proteinG: Int, val fatG: Int, val carbsG: Int)

@Serializable
data class TargetOverrides(val kcal: Int?, val proteinG: Int?, val fatG: Int?, val carbsG: Int?) {
    companion object {
        val NONE = TargetOverrides(null, null, null, null)
    }
}
