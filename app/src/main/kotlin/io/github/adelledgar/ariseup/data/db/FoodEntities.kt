package io.github.adelledgar.ariseup.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.adelledgar.ariseup.domain.model.Macros
import kotlinx.serialization.Serializable
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate

enum class FoodSource { GENERIC, OFF, CUSTOM }

enum class Meal(val label: String) {
    BREAKFAST("Desayuno"),
    LUNCH("Comida"),
    SNACK("Merienda"),
    DINNER("Cena"),
    OTHER("Snacks"),
}

/** Minúsculas y sin acentos, para buscar. */
fun normalizeForSearch(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase().trim()

/** Alimento con valores por 100 g/ml. */
@Serializable
@Entity(tableName = "food", indices = [Index(value = ["barcode"], unique = true), Index("searchName")])
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val searchName: String,
    val brand: String?,
    val barcode: String?,
    val source: FoodSource,
    val category: String? = null,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val sugars: Double,
    val fat: Double,
    val saturated: Double,
    val fiber: Double,
    val salt: Double,
    val servingG: Double? = null,
    val unitG: Double? = null,
    val incomplete: Boolean = false,
    @Serializable(InstantSerializer::class) val lastUsedAt: Instant? = null,
    val favorite: Boolean = false,
) {
    val per100: Macros get() = Macros(kcal, protein, carbs, sugars, fat, saturated, fiber, salt)

    companion object {
        fun create(
            name: String,
            brand: String?,
            barcode: String?,
            source: FoodSource,
            kcal: Double = 0.0,
            protein: Double = 0.0,
            carbs: Double = 0.0,
            sugars: Double = 0.0,
            fat: Double = 0.0,
            saturated: Double = 0.0,
            fiber: Double = 0.0,
            salt: Double = 0.0,
            servingG: Double? = null,
            unitG: Double? = null,
            incomplete: Boolean = false,
            category: String? = null,
        ) = FoodEntity(
            name = name, searchName = normalizeForSearch(listOfNotNull(name, brand).joinToString(" ")), brand = brand,
            barcode = barcode, source = source, category = category, kcal = kcal, protein = protein, carbs = carbs,
            sugars = sugars, fat = fat, saturated = saturated, fiber = fiber, salt = salt, servingG = servingG,
            unitG = unitG, incomplete = incomplete,
        )
    }
}

@Serializable
@Entity(tableName = "recipe")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val searchName: String,
    val servings: Double,
    val favorite: Boolean = false,
)

@Serializable
@Entity(tableName = "recipe_item", indices = [Index("recipeId")])
data class RecipeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val foodId: Long,
    val grams: Double,
)

/** Una entrada del diario: un alimento en gramos o una receta en raciones. */
@Serializable
@Entity(tableName = "food_log", indices = [Index("date")])
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Serializable(LocalDateSerializer::class) val date: LocalDate,
    val meal: Meal,
    val foodId: Long? = null,
    val recipeId: Long? = null,
    val grams: Double? = null,
    val servings: Double? = null,
)

enum class RuleType { CHECK, COUNTER }

@Serializable
@Entity(tableName = "diet_rule")
data class DietRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: RuleType,
    val target: Int?,
    val mandatory: Boolean,
    val archived: Boolean = false,
) {
    fun met(value: Int): Boolean = when (type) {
        RuleType.CHECK -> value >= 1
        RuleType.COUNTER -> value >= (target ?: 1)
    }
}

@Serializable
@Entity(tableName = "diet_rule_log", primaryKeys = ["date", "ruleId"])
data class DietRuleLogEntity(
    @Serializable(LocalDateSerializer::class) val date: LocalDate,
    val ruleId: Long,
    val value: Int,
)
