package io.github.adelledgar.ariseup.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.TestEnv
import io.github.adelledgar.ariseup.data.db.FoodEntity
import io.github.adelledgar.ariseup.data.db.FoodSource
import io.github.adelledgar.ariseup.data.db.Meal
import io.github.adelledgar.ariseup.system.DayCloseCoordinator
import io.github.adelledgar.ariseup.system.MissionPlanner
import io.github.adelledgar.ariseup.ui.nutrition.NutritionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class NutritionViewModelTest {
    private val env = TestEnv(start = "2026-11-02T10:00")
    private val d = LocalDate.of(2026, 11, 2)
    private val planner = MissionPlanner(env.db, env.players, env.missions, env.foods, env.settings, env.gameClock)
    private val coordinator = DayCloseCoordinator(env.db, env.players, env.foods, env.settings, env.gameClock, planner)
    private fun vm() = NutritionViewModel(env.db, env.players, env.foods, env.body, env.settings, env.gameClock, coordinator)

    @Test
    fun yesterdayEditableUntilMargin() = runTest {
        env.clock.set("2026-11-03T11:59")
        assertTrue(vm().isEditable(d))
        assertTrue(vm().isEditable(d.plusDays(1)))
        env.clock.set("2026-11-03T12:00")
        assertFalse(vm().isEditable(d))
        assertFalse(vm().isEditable(d.minusDays(5)))
    }

    @Test
    fun dayStateGroupsMealsAndTargets() = runTest {
        env.seedPlayer()
        val chicken = env.foods.insert(FoodEntity.create("Pollo", null, null, FoodSource.CUSTOM, kcal = 165.0, protein = 31.0))
        env.foods.logFood(d, Meal.LUNCH, chicken, 200.0)
        val s = vm().dayState(d).first()
        assertEquals(330.0, s.totals.kcal, 1e-9)
        assertEquals(1, s.meals.getValue(Meal.LUNCH).size)
        assertEquals("Pollo", s.meals.getValue(Meal.LUNCH).single().name)
        assertTrue(s.meals.getValue(Meal.BREAKFAST).isEmpty())
        assertTrue(s.targets.kcal > 1000)
    }
}
