package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.ActivityLevel
import io.github.adelledgar.ariseup.domain.model.Goal
import io.github.adelledgar.ariseup.domain.model.Macros
import io.github.adelledgar.ariseup.domain.model.NutritionTargets
import io.github.adelledgar.ariseup.domain.model.Profile
import io.github.adelledgar.ariseup.domain.model.Sex
import io.github.adelledgar.ariseup.domain.model.TargetOverrides
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NutritionTest {
    private val today = LocalDate.of(2026, 11, 1)
    private val none = TargetOverrides(null, null, null, null)

    private fun profile(sex: Sex = Sex.MALE, goal: Goal = Goal.LOSE_FAT) =
        Profile("Jugador", sex, today.minusYears(30), 180.0, ActivityLevel.MODERATE, goal)

    private fun macros(kcal: Double = 0.0, protein: Double = 0.0) = Macros.ZERO.copy(kcal = kcal, protein = protein)

    @Test
    fun bmrMale() {
        assertEquals(1780.0, Nutrition.bmr(Sex.MALE, 80.0, 180.0, 30), 0.01)
    }

    @Test
    fun bmrFemale() {
        assertEquals(1320.25, Nutrition.bmr(Sex.FEMALE, 60.0, 165.0, 30), 0.01)
    }

    @Test
    fun targetsLoseFat() {
        assertEquals(NutritionTargets(2207, 160, 66, 243), Nutrition.targets(profile(), 80.0, today, none))
    }

    @Test
    fun targetsMaintainUsesLowerProtein() {
        val t = Nutrition.targets(profile(goal = Goal.MAINTAIN), 80.0, today, none)
        assertEquals(2759, t.kcal)
        assertEquals(128, t.proteinG)
    }

    @Test
    fun overridesWin() {
        val t = Nutrition.targets(profile(), 80.0, today, TargetOverrides(2000, null, null, null))
        assertEquals(2000, t.kcal)
        assertEquals(60, t.fatG)
    }

    @Test
    fun portion() {
        assertEquals(31.0, Nutrition.portion(macros(protein = 31.0), 100.0).protein, 1e-9)
        assertEquals(46.5, Nutrition.portion(macros(protein = 31.0), 150.0).protein, 1e-9)
    }

    @Test
    fun kj() {
        assertEquals(100.0, Nutrition.kjToKcal(418.4), 1e-9)
    }

    @Test
    fun macrosAdd() {
        assertEquals(30.0, (macros(kcal = 10.0) + macros(kcal = 20.0)).kcal, 1e-9)
    }

    @Test
    fun metBoundaries() {
        val t = NutritionTargets(2000, 150, 60, 200)
        assertTrue(Nutrition.nutritionMet(t, macros(kcal = 2200.0, protein = 135.0), 3))
        assertTrue(Nutrition.nutritionMet(t, macros(kcal = 1800.0, protein = 150.0), 3))
        assertFalse(Nutrition.nutritionMet(t, macros(kcal = 2201.0, protein = 150.0), 3))
        assertFalse(Nutrition.nutritionMet(t, macros(kcal = 1799.0, protein = 150.0), 3))
        assertFalse(Nutrition.nutritionMet(t, macros(kcal = 2000.0, protein = 134.0), 3))
        assertFalse(Nutrition.nutritionMet(t, macros(kcal = 2000.0, protein = 150.0), 2))
    }
}
