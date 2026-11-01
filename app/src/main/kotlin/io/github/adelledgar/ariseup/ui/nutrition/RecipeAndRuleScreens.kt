package io.github.adelledgar.ariseup.ui.nutrition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.adelledgar.ariseup.data.db.DietRuleEntity
import io.github.adelledgar.ariseup.data.db.FoodEntity
import io.github.adelledgar.ariseup.data.db.Meal
import io.github.adelledgar.ariseup.data.db.RecipeEntity
import io.github.adelledgar.ariseup.data.db.RecipeItemEntity
import io.github.adelledgar.ariseup.data.db.RuleType
import io.github.adelledgar.ariseup.di.AppContainer
import io.github.adelledgar.ariseup.domain.Nutrition
import io.github.adelledgar.ariseup.domain.model.Macros
import io.github.adelledgar.ariseup.ui.ScreenColumn
import io.github.adelledgar.ariseup.ui.components.ChoiceChips
import io.github.adelledgar.ariseup.ui.components.SystemButton
import io.github.adelledgar.ariseup.ui.components.SystemTextField
import io.github.adelledgar.ariseup.ui.components.SystemWindow
import io.github.adelledgar.ariseup.ui.components.toDecimalOrNull
import io.github.adelledgar.ariseup.ui.theme.SystemColors
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Añadir raciones de una receta al diario. */
@Composable
fun RecipeLogScreen(container: AppContainer, recipeId: Long, date: LocalDate, meal: Meal, onDone: () -> Unit) {
    var recipe by remember { mutableStateOf<RecipeEntity?>(null) }
    var perServing by remember { mutableStateOf(Macros.ZERO) }
    var servings by remember { mutableStateOf("1") }
    var targetMeal by remember { mutableStateOf(meal) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(recipeId) {
        recipe = container.foods.recipe(recipeId)
        perServing = container.foods.recipePerServing(recipeId)
    }
    val r = recipe ?: return
    val n = servings.toDecimalOrNull() ?: 0.0
    ScreenColumn {
        item {
            SystemWindow(r.name, onClose = onDone) {
                SystemTextField(servings, { servings = it }, "Raciones", numeric = true)
                MacroTable(perServing.times(n), "${fmtServings(n)} de ${fmtServings(r.servings)} raciones")
                ChoiceChips(Meal.entries, targetMeal, { it.label }, { targetMeal = it })
                SystemButton("Añadir", { if (n > 0) scope.launch { container.foods.logRecipe(date, targetMeal, recipeId, n); onDone() } }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun RecipeListScreen(container: AppContainer, onEdit: (Long) -> Unit, onBack: () -> Unit) {
    val recipes by container.foods.observeRecipes().collectAsStateWithLifecycle(emptyList())
    ScreenColumn {
        item {
            SystemWindow("Recetas", onClose = onBack) {
                SystemButton("Nueva receta", { onEdit(0) }, Modifier.fillMaxWidth())
                recipes.forEach { r ->
                    Text(
                        "${r.name} · ${fmtServings(r.servings)} raciones",
                        color = SystemColors.text,
                        modifier = Modifier.fillMaxWidth().clickable { onEdit(r.id) }.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

private data class Ingredient(val food: FoodEntity, val grams: String)

@Composable
fun RecipeEditorScreen(container: AppContainer, recipeId: Long, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var servings by remember { mutableStateOf("1") }
    val items = remember { mutableStateListOf<Ingredient>() }
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<FoodEntity>()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(recipeId) {
        if (recipeId != 0L) {
            container.foods.recipe(recipeId)?.let { name = it.name; servings = fmtServings(it.servings) }
            items.clear()
            container.foods.recipeItems(recipeId).forEach { it2 -> container.foods.get(it2.foodId)?.let { items += Ingredient(it, it2.grams.toInt().toString()) } }
        }
    }
    LaunchedEffect(q) { results = if (q.length < 2) emptyList() else container.foods.searchLocal(q, 8) }
    val total = items.fold(Macros.ZERO) { acc, i -> acc + Nutrition.portion(i.food.per100, i.grams.toDecimalOrNull() ?: 0.0) }
    val n = servings.toDecimalOrNull()?.takeIf { it > 0 } ?: 1.0
    ScreenColumn {
        item {
            SystemWindow(if (recipeId == 0L) "Nueva receta" else "Editar receta", onClose = onDone) {
                SystemTextField(name, { name = it }, "Nombre")
                SystemTextField(servings, { servings = it }, "Raciones", numeric = true)
                Text("Ingredientes", color = SystemColors.muted)
                items.forEachIndexed { i, ing ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(ing.food.name, color = SystemColors.text, modifier = Modifier.weight(1f))
                        SystemTextField(ing.grams, { items[i] = ing.copy(grams = it) }, "g", numeric = true, modifier = Modifier.width(90.dp))
                        IconButton(onClick = { items.removeAt(i) }) { Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = SystemColors.danger) }
                    }
                }
                SystemTextField(q, { q = it }, "Añadir ingrediente (buscar)")
                results.forEach { f ->
                    Text(f.name, color = SystemColors.text, modifier = Modifier.fillMaxWidth().clickable { items += Ingredient(f, "100"); q = "" }.padding(6.dp))
                }
                Spacer(Modifier.height(8.dp))
                MacroTable(total.times(1 / n), "Por ración")
                error?.let { Text(it, color = SystemColors.danger) }
                SystemButton("Guardar receta", {
                    if (name.isBlank() || items.isEmpty()) {
                        error = "Ponle nombre y al menos un ingrediente"
                        return@SystemButton
                    }
                    scope.launch {
                        val wasNew = recipeId == 0L
                        container.foods.createRecipe(
                            name.trim(), n,
                            items.map { RecipeItemEntity(recipeId = recipeId, foodId = it.food.id, grams = it.grams.toDecimalOrNull() ?: 0.0) },
                            id = recipeId,
                        )
                        if (wasNew) container.achievements.check()
                        onDone()
                    }
                }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun DietRulesScreen(container: AppContainer, onBack: () -> Unit) {
    val rules by container.foods.observeRules().collectAsStateWithLifecycle(emptyList())
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(RuleType.CHECK) }
    var target by remember { mutableStateOf("8") }
    var mandatory by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    ScreenColumn {
        item {
            SystemWindow("Reglas de dieta", onClose = onBack) {
                Text("Las obligatorias que no cumplas te enviarán a la Zona de Penalización.", color = SystemColors.muted, style = MaterialTheme.typography.bodySmall)
                rules.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.name, color = SystemColors.text)
                            Text(
                                (if (r.type == RuleType.CHECK) "Casilla" else "Contador: ${r.target}") + if (r.mandatory) " · obligatoria" else " · opcional",
                                color = SystemColors.muted, style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(onClick = { scope.launch { container.foods.saveRule(r.copy(archived = true)) } }) { Text("Quitar", color = SystemColors.danger) }
                    }
                }
            }
        }
        item {
            SystemWindow("Nueva regla") {
                SystemTextField(name, { name = it }, "Regla (p. ej. «Sin ultraprocesados»)")
                ChoiceChips(RuleType.entries, type, { if (it == RuleType.CHECK) "Casilla" else "Contador" }, { type = it })
                if (type == RuleType.COUNTER) SystemTextField(target, { target = it }, "Objetivo (p. ej. 8 vasos de agua)", numeric = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Obligatoria", color = SystemColors.text, modifier = Modifier.weight(1f))
                    Switch(mandatory, { mandatory = it })
                }
                SystemButton("Crear regla", {
                    if (name.isNotBlank()) scope.launch {
                        container.foods.saveRule(
                            DietRuleEntity(name = name.trim(), type = type, target = if (type == RuleType.COUNTER) target.toDecimalOrNull()?.toInt() ?: 1 else null, mandatory = mandatory),
                        )
                        name = ""
                    }
                }, Modifier.fillMaxWidth())
                Text("Las reglas nuevas cuentan como misión a partir de mañana.", color = SystemColors.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
