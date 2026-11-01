package io.github.adelledgar.ariseup.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.TestEnv
import io.github.adelledgar.ariseup.data.db.FoodEntity
import io.github.adelledgar.ariseup.data.db.FoodSource
import io.github.adelledgar.ariseup.data.db.Meal
import io.github.adelledgar.ariseup.data.db.RecipeItemEntity
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class FoodRepositoryTest {
    private val env = TestEnv()

    private fun food(name: String, protein: Double = 0.0, kcal: Double = 0.0, barcode: String? = null) =
        FoodEntity.create(name = name, brand = null, barcode = barcode, source = FoodSource.CUSTOM, kcal = kcal, protein = protein)

    @Test
    fun searchIgnoresAccentsAndCase() = runTest {
        env.foods.insert(food("Plátano"))
        assertEquals(1, env.foods.searchLocal("PLATANO").size)
        assertEquals(1, env.foods.searchLocal("plát").size)
    }

    @Test
    fun byBarcode() = runTest {
        env.foods.insert(food("Yogur", barcode = "8410000000001"))
        assertEquals("Yogur", env.foods.byBarcode("8410000000001")!!.name)
    }

    @Test
    fun upsertOffReplacesSameBarcode() = runTest {
        env.foods.upsertOff(food("Viejo", barcode = "123").copy(source = FoodSource.OFF))
        env.foods.upsertOff(food("Nuevo", barcode = "123").copy(source = FoodSource.OFF))
        assertEquals("Nuevo", env.foods.byBarcode("123")!!.name)
        assertEquals(1, env.foods.searchLocal("").size)
    }

    @Test
    fun dayTotalsSumsRecipesAndFoods() = runTest {
        val chicken = env.foods.insert(food("Pollo", protein = 31.0, kcal = 165.0))
        val rice = env.foods.insert(food("Arroz", protein = 2.7, kcal = 130.0))
        val recipe = env.foods.createRecipe("Arroz solo", servings = 2.0, items = listOf(RecipeItemEntity(recipeId = 0, foodId = rice, grams = 100.0)))
        env.foods.logFood(env.today, Meal.LUNCH, chicken, grams = 150.0)
        env.foods.logRecipe(env.today, Meal.DINNER, recipe, servings = 1.0)
        val totals = env.foods.dayTotals(env.today)
        assertEquals(46.5 + 1.35, totals.protein, 1e-9)
        assertEquals(247.5 + 65.0, totals.kcal, 1e-9)
        assertEquals(2, env.foods.mealsWithEntries(env.today))
    }
}
