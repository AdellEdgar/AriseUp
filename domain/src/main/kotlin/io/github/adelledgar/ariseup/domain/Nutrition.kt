package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.Goal
import io.github.adelledgar.ariseup.domain.model.Macros
import io.github.adelledgar.ariseup.domain.model.NutritionTargets
import io.github.adelledgar.ariseup.domain.model.Profile
import io.github.adelledgar.ariseup.domain.model.Sex
import io.github.adelledgar.ariseup.domain.model.TargetOverrides
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

/** Objetivos y evaluación de la nutrición (spec §3). */
object Nutrition {
    /** Gasto en reposo (Mifflin-St Jeor). */
    fun bmr(sex: Sex, weightKg: Double, heightCm: Double, age: Int): Double =
        10 * weightKg + 6.25 * heightCm - 5 * age + if (sex == Sex.MALE) 5 else -161

    fun targets(p: Profile, weightKg: Double, today: LocalDate, overrides: TargetOverrides): NutritionTargets {
        val tdee = bmr(p.sex, weightKg, p.heightCm, p.age(today)) * p.activity.factor
        val kcal = overrides.kcal ?: (tdee * kcalFactor(p.goal)).roundToInt()
        val protein = overrides.proteinG ?: (weightKg * if (p.goal == Goal.MAINTAIN) 1.6 else 2.0).roundToInt()
        val fat = overrides.fatG ?: (kcal * 0.27 / 9).roundToInt()
        val carbs = overrides.carbsG ?: maxOf(0, ((kcal - protein * 4 - fat * 9) / 4.0).roundToInt())
        return NutritionTargets(kcal, protein, fat, carbs)
    }

    private fun kcalFactor(goal: Goal) = when (goal) {
        Goal.LOSE_FAT -> 0.8
        Goal.MAINTAIN -> 1.0
        Goal.GAIN_MUSCLE -> 1.1
    }

    fun portion(per100: Macros, grams: Double): Macros = per100.times(grams / 100.0)

    fun kjToKcal(kj: Double): Double = kj / 4.184

    fun nutritionMet(t: NutritionTargets, eaten: Macros, mealsWithEntries: Int): Boolean =
        abs(eaten.kcal - t.kcal) <= t.kcal * Balance.NUTRITION_KCAL_TOLERANCE + 1e-9 &&
            eaten.protein >= t.proteinG * Balance.NUTRITION_PROTEIN_MIN - 1e-9 &&
            mealsWithEntries >= Balance.NUTRITION_MIN_MEALS
}
