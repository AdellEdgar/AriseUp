package io.github.adelledgar.ariseup.data.repo

import androidx.room.withTransaction
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.DietRuleEntity
import io.github.adelledgar.ariseup.data.db.DietRuleLogEntity
import io.github.adelledgar.ariseup.data.db.FoodEntity
import io.github.adelledgar.ariseup.data.db.FoodLogEntity
import io.github.adelledgar.ariseup.data.db.Meal
import io.github.adelledgar.ariseup.data.db.RecipeEntity
import io.github.adelledgar.ariseup.data.db.RecipeItemEntity
import io.github.adelledgar.ariseup.data.db.normalizeForSearch
import io.github.adelledgar.ariseup.domain.Nutrition
import io.github.adelledgar.ariseup.domain.model.Macros
import java.time.Clock
import java.time.LocalDate

class FoodRepository(private val db: AriseUpDatabase, private val clock: Clock) {
    private val dao get() = db.foodDao()

    suspend fun insert(f: FoodEntity): Long = dao.insert(f.copy(searchName = normalizeForSearch(listOfNotNull(f.name, f.brand).joinToString(" "))))

    suspend fun update(f: FoodEntity) {
        dao.upsert(f.copy(searchName = normalizeForSearch(listOfNotNull(f.name, f.brand).joinToString(" "))))
    }

    suspend fun get(id: Long) = dao.get(id)

    suspend fun searchLocal(q: String, limit: Int = 50): List<FoodEntity> = dao.search(normalizeForSearch(q), limit)

    suspend fun searchRecipes(q: String) = dao.searchRecipes(normalizeForSearch(q))

    suspend fun byBarcode(code: String): FoodEntity? = dao.byBarcode(code.trim())

    /** Guarda un producto de Open Food Facts, sustituyendo el que tenga el mismo código. */
    suspend fun upsertOff(food: FoodEntity): Long {
        val existing = food.barcode?.let { dao.byBarcode(it) }
        val f = food.copy(
            id = existing?.id ?: 0,
            favorite = existing?.favorite ?: false,
            lastUsedAt = existing?.lastUsedAt,
            searchName = normalizeForSearch(listOfNotNull(food.name, food.brand).joinToString(" ")),
        )
        return if (existing == null) dao.insert(f) else { dao.upsert(f); existing.id }
    }

    suspend fun setFavorite(id: Long, fav: Boolean) = dao.setFavorite(id, fav)

    suspend fun createRecipe(name: String, servings: Double, items: List<RecipeItemEntity>, id: Long = 0): Long = db.withTransaction {
        val r = RecipeEntity(id = id, name = name, searchName = normalizeForSearch(name), servings = servings)
        val recipeId = if (id == 0L) dao.insertRecipe(r) else { dao.upsertRecipe(r); id }
        dao.clearItems(recipeId)
        dao.insertItems(items.map { it.copy(id = 0, recipeId = recipeId) })
        recipeId
    }

    suspend fun recipe(id: Long) = dao.recipe(id)

    suspend fun recipeItems(id: Long) = dao.items(id)

    fun observeRecipes() = dao.observeRecipes()

    /** Macros de una ración de la receta. */
    suspend fun recipePerServing(recipeId: Long): Macros {
        val r = dao.recipe(recipeId) ?: return Macros.ZERO
        val total = dao.items(recipeId).fold(Macros.ZERO) { acc, item ->
            val f = dao.get(item.foodId) ?: return@fold acc
            acc + Nutrition.portion(f.per100, item.grams)
        }
        return total.times(1.0 / r.servings.coerceAtLeast(0.01))
    }

    suspend fun logFood(date: LocalDate, meal: Meal, foodId: Long, grams: Double): Long {
        dao.touch(foodId, clock.instant())
        return dao.insertLog(FoodLogEntity(date = date, meal = meal, foodId = foodId, grams = grams))
    }

    suspend fun logRecipe(date: LocalDate, meal: Meal, recipeId: Long, servings: Double): Long =
        dao.insertLog(FoodLogEntity(date = date, meal = meal, recipeId = recipeId, servings = servings))

    suspend fun deleteLog(id: Long) = dao.deleteLog(id)

    fun observeLogs(date: LocalDate) = dao.observeLogs(date)

    suspend fun logsFor(date: LocalDate) = dao.logsFor(date)

    /** Copia las entradas de una comida de otro día. */
    suspend fun copyMeal(from: LocalDate, fromMeal: Meal, to: LocalDate, toMeal: Meal) = db.withTransaction {
        dao.logsFor(from).filter { it.meal == fromMeal }.forEach { dao.insertLog(it.copy(id = 0, date = to, meal = toMeal)) }
    }

    suspend fun macrosOf(log: FoodLogEntity): Macros = when {
        log.foodId != null -> dao.get(log.foodId)?.let { Nutrition.portion(it.per100, log.grams ?: 0.0) } ?: Macros.ZERO
        log.recipeId != null -> recipePerServing(log.recipeId).times(log.servings ?: 1.0)
        else -> Macros.ZERO
    }

    suspend fun dayTotals(date: LocalDate): Macros = dao.logsFor(date).fold(Macros.ZERO) { acc, l -> acc + macrosOf(l) }

    suspend fun mealsWithEntries(date: LocalDate): Int = dao.logsFor(date).map { it.meal }.distinct().size

    // Reglas de dieta
    fun observeRules() = dao.observeRules()

    suspend fun activeRules() = dao.activeRules()

    suspend fun saveRule(r: DietRuleEntity): Long = dao.upsertRule(r)

    suspend fun setRuleValue(date: LocalDate, ruleId: Long, value: Int) = dao.upsertRuleLog(DietRuleLogEntity(date, ruleId, maxOf(0, value)))

    suspend fun ruleLogs(date: LocalDate) = dao.ruleLogs(date)

    fun observeRuleLogs(date: LocalDate) = dao.observeRuleLogs(date)
}
