package io.github.adelledgar.ariseup.data.seed

import android.content.Context
import io.github.adelledgar.ariseup.data.db.AppJson
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.FoodEntity
import io.github.adelledgar.ariseup.data.db.FoodSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
private data class GenericFood(
    val name: String,
    val category: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val sugars: Double,
    val fat: Double,
    val saturated: Double,
    val fiber: Double,
    val salt: Double,
    val incomplete: Boolean,
)

/** Carga la tabla genérica (USDA SR Legacy en español) en el primer arranque. */
class GenericFoodsSeeder(private val context: Context, private val db: AriseUpDatabase) {
    /** Devuelve cuántos alimentos ha insertado (0 si ya estaban). */
    suspend fun seedIfEmpty(): Int {
        if (db.foodDao().countBySource(FoodSource.GENERIC) > 0) return 0
        val json = context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val foods = AppJson.decodeFromString(ListSerializer(GenericFood.serializer()), json).map {
            FoodEntity.create(
                name = it.name, brand = null, barcode = null, source = FoodSource.GENERIC, kcal = it.kcal,
                protein = it.protein, carbs = it.carbs, sugars = it.sugars, fat = it.fat, saturated = it.saturated,
                fiber = it.fiber, salt = it.salt, incomplete = it.incomplete, category = it.category,
            )
        }
        db.foodDao().insertAll(foods)
        return foods.size
    }

    private companion object {
        const val ASSET = "generic_foods.json"
    }
}
