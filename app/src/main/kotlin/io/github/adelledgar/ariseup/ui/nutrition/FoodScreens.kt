package io.github.adelledgar.ariseup.ui.nutrition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.adelledgar.ariseup.data.db.FoodEntity
import io.github.adelledgar.ariseup.data.db.FoodSource
import io.github.adelledgar.ariseup.data.db.Meal
import io.github.adelledgar.ariseup.data.db.RecipeEntity
import io.github.adelledgar.ariseup.di.AppContainer
import io.github.adelledgar.ariseup.domain.Nutrition
import io.github.adelledgar.ariseup.domain.model.Macros
import io.github.adelledgar.ariseup.network.OffResult
import io.github.adelledgar.ariseup.scan.BarcodeScanner
import io.github.adelledgar.ariseup.ui.ScreenColumn
import io.github.adelledgar.ariseup.ui.components.ChoiceChips
import io.github.adelledgar.ariseup.ui.components.InfoRow
import io.github.adelledgar.ariseup.ui.components.SystemButton
import io.github.adelledgar.ariseup.ui.components.SystemTextField
import io.github.adelledgar.ariseup.ui.components.SystemWindow
import io.github.adelledgar.ariseup.ui.components.toDecimalOrNull
import io.github.adelledgar.ariseup.ui.theme.LocalSystemPalette
import io.github.adelledgar.ariseup.ui.theme.SystemColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

private fun f1(v: Double) = "%.1f".format(Locale("es"), v)

/** Búsqueda: primero local en vivo; Open Food Facts solo al pulsar. */
@Composable
fun FoodSearchScreen(container: AppContainer, date: LocalDate, meal: Meal, onPickFood: (Long) -> Unit, onPickRecipe: (Long) -> Unit, onNewFood: () -> Unit, onBack: () -> Unit) {
    var q by remember { mutableStateOf("") }
    var local by remember { mutableStateOf(emptyList<FoodEntity>()) }
    var recipes by remember { mutableStateOf(emptyList<RecipeEntity>()) }
    var remote by remember { mutableStateOf<OffResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(q) {
        local = container.foods.searchLocal(q, 40)
        recipes = if (q.isBlank()) emptyList() else container.foods.searchRecipes(q)
        remote = null
    }
    ScreenColumn {
        item {
            SystemWindow("Añadir a ${meal.label.lowercase()}", onClose = onBack) {
                SystemTextField(q, { q = it }, "Buscar alimento")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SystemButton("Buscar en internet", {
                        if (q.isNotBlank()) scope.launch {
                            loading = true
                            remote = container.off.search(q)
                            loading = false
                        }
                    }, Modifier.weight(1f), enabled = q.isNotBlank() && !loading)
                    SystemButton("Crear alimento", onNewFood, Modifier.weight(1f))
                }
            }
        }
        if (recipes.isNotEmpty()) item {
            SystemWindow("Recetas") { recipes.forEach { r -> ResultRow(r.name, "Receta · ${fmtServings(r.servings)} raciones") { onPickRecipe(r.id) } } }
        }
        item {
            SystemWindow(if (q.isBlank()) "Recientes y favoritos" else "En tu dispositivo") {
                if (local.isEmpty()) Text("Sin resultados.", color = SystemColors.muted)
                local.forEach { f -> ResultRow(f.name + (f.brand?.let { " · $it" } ?: ""), "${f.kcal.toInt()} kcal · P ${f1(f.protein)} g /100 g", f.favorite) { onPickFood(f.id) } }
            }
        }
        if (loading) item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LocalSystemPalette.current.accent) } }
        remote?.let { r ->
            item {
                SystemWindow("Open Food Facts") {
                    when (r) {
                        is OffResult.Found -> r.foods.forEach { f ->
                            ResultRow(f.name + (f.brand?.let { " · $it" } ?: ""), "${f.kcal.toInt()} kcal /100 g" + if (f.incomplete) " · datos incompletos" else "") {
                                scope.launch { onPickFood(container.foods.upsertOff(f)) }
                            }
                        }
                        OffResult.NotFound -> Text("[Sin resultados]", color = SystemColors.muted)
                        OffResult.Offline -> Text("[Sin conexión con el servidor]", color = SystemColors.danger)
                        is OffResult.Error -> Text(r.msg, color = SystemColors.danger)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(title: String, subtitle: String, favorite: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = SystemColors.text)
            Text(subtitle, color = SystemColors.muted, style = MaterialTheme.typography.bodySmall)
        }
        if (favorite) Icon(Icons.Default.Star, contentDescription = "Favorito", tint = SystemColors.gold)
    }
}

private enum class Unit2(val label: String) { GRAMS("Gramos"), SERVING("Raciones"), UNIT("Unidades") }

/** Ficha de un alimento: cantidad con macros en vivo y añadir al diario. */
@Composable
fun FoodDetailScreen(container: AppContainer, foodId: Long, date: LocalDate, meal: Meal, onDone: () -> Unit, onEdit: (Long) -> Unit) {
    var food by remember { mutableStateOf<FoodEntity?>(null) }
    LaunchedEffect(foodId) { food = container.foods.get(foodId) }
    val f = food ?: return
    var unit by remember { mutableStateOf(Unit2.GRAMS) }
    var amount by remember { mutableStateOf("100") }
    var targetMeal by remember { mutableStateOf(meal) }
    val scope = rememberCoroutineScope()
    val units = listOfNotNull(Unit2.GRAMS, Unit2.SERVING.takeIf { f.servingG != null }, Unit2.UNIT.takeIf { f.unitG != null })
    val grams = (amount.toDecimalOrNull() ?: 0.0) * when (unit) {
        Unit2.GRAMS -> 1.0
        Unit2.SERVING -> f.servingG ?: 0.0
        Unit2.UNIT -> f.unitG ?: 0.0
    }
    val m = Nutrition.portion(f.per100, grams)
    ScreenColumn {
        item {
            SystemWindow(f.name, onClose = onDone) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(listOfNotNull(f.brand, sourceLabel(f.source)).joinToString(" · "), color = SystemColors.muted, modifier = Modifier.weight(1f))
                    IconButton(onClick = { scope.launch { container.foods.setFavorite(f.id, !f.favorite); food = f.copy(favorite = !f.favorite) } }) {
                        Icon(if (f.favorite) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = "Favorito", tint = SystemColors.gold)
                    }
                }
                if (f.incomplete) Text("[Datos incompletos: revisa la etiqueta y edítalo]", color = SystemColors.gold, style = MaterialTheme.typography.bodySmall)
                ChoiceChips(units, unit, { it.label }, { unit = it; amount = if (it == Unit2.GRAMS) "100" else "1" })
                SystemTextField(amount, { amount = it }, "Cantidad", numeric = true, suffix = if (unit == Unit2.GRAMS) "g" else null)
                Spacer(Modifier.height(8.dp))
                MacroTable(m, "${grams.toInt()} g")
                Text("Comida", color = SystemColors.muted)
                ChoiceChips(Meal.entries, targetMeal, { it.label }, { targetMeal = it })
                Spacer(Modifier.height(8.dp))
                SystemButton("Añadir", {
                    if (grams > 0) scope.launch {
                        container.foods.logFood(date, targetMeal, f.id, grams)
                        onDone()
                    }
                }, Modifier.fillMaxWidth())
                if (f.source != FoodSource.GENERIC) SystemButton("Editar alimento", { onEdit(f.id) }, Modifier.fillMaxWidth(), color = SystemColors.muted)
            }
        }
    }
}

fun sourceLabel(s: FoodSource) = when (s) {
    FoodSource.GENERIC -> "Tabla genérica (USDA)"
    FoodSource.OFF -> "Open Food Facts"
    FoodSource.CUSTOM -> "Creado por ti"
}

@Composable
fun MacroTable(m: Macros, header: String) {
    Column {
        InfoRow("Cantidad", header)
        InfoRow("Calorías", "${m.kcal.toInt()} kcal", valueColor = LocalSystemPalette.current.accent)
        InfoRow("Proteína", "${f1(m.protein)} g")
        InfoRow("Hidratos", "${f1(m.carbs)} g (azúcares ${f1(m.sugars)} g)")
        InfoRow("Grasa", "${f1(m.fat)} g (saturadas ${f1(m.saturated)} g)")
        InfoRow("Fibra", "${f1(m.fiber)} g")
        InfoRow("Sal", "${f1(m.salt)} g")
    }
}

/** Escáner: busca en local, después en Open Food Facts; si no aparece, crear con el código. */
@Composable
fun ScanScreen(container: AppContainer, onFound: (Long) -> Unit, onNotFound: (String) -> Unit, onBack: () -> Unit) {
    var status by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(code) {
        val c = code ?: return@LaunchedEffect
        container.foods.byBarcode(c)?.let { onFound(it.id); return@LaunchedEffect }
        status = "[Consultando Open Food Facts…]"
        when (val r = container.off.product(c)) {
            is OffResult.Found -> onFound(container.foods.upsertOff(r.foods.first()))
            OffResult.NotFound -> onNotFound(c)
            OffResult.Offline -> status = "[Sin conexión con el servidor] Puedes crear el alimento a mano."
            is OffResult.Error -> status = r.msg
        }
    }
    Column(Modifier.fillMaxSize()) {
        SystemWindow("Escáner", onClose = onBack, modifier = Modifier.padding(16.dp)) {
            Text(status ?: "Apunta al código de barras del envase.", color = SystemColors.text)
            if (status != null && code != null) SystemButton("Crear alimento con el código $code", { onNotFound(code!!) }, Modifier.fillMaxWidth())
        }
        if (code == null) Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp)) { BarcodeScanner(onCode = { code = it }) }
    }
}

/** Crear o editar un alimento copiando la etiqueta (valores por 100 g). */
@Composable
fun FoodEditorScreen(container: AppContainer, foodId: Long?, barcode: String?, onSaved: (Long) -> Unit, onBack: () -> Unit) {
    val fields = remember { mutableStateOf(List(10) { "" }) }
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var existing by remember { mutableStateOf<FoodEntity?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(foodId) {
        if (foodId != null) container.foods.get(foodId)?.let { f ->
            existing = f
            name = f.name
            brand = f.brand.orEmpty()
            fields.value = listOf(f.kcal, f.protein, f.carbs, f.sugars, f.fat, f.saturated, f.fiber, f.salt).map { it.toString() } +
                listOf(f.servingG?.toString().orEmpty(), f.unitG?.toString().orEmpty())
        }
    }
    val labels = listOf("Calorías (kcal)", "Proteína (g)", "Hidratos (g)", "Azúcares (g)", "Grasa (g)", "Saturadas (g)", "Fibra (g)", "Sal (g)", "Ración (g, opcional)", "Peso por unidad (g, opcional)")
    ScreenColumn {
        item {
            SystemWindow(if (existing == null) "Nuevo alimento" else "Editar alimento", onClose = onBack) {
                (barcode ?: existing?.barcode)?.let { Text("Código: $it", color = SystemColors.muted) }
                SystemTextField(name, { name = it }, "Nombre")
                SystemTextField(brand, { brand = it }, "Marca (opcional)")
                Text("Valores por 100 g", color = SystemColors.muted)
                labels.forEachIndexed { i, l ->
                    SystemTextField(fields.value[i], { v -> fields.value = fields.value.toMutableList().also { it[i] = v } }, l, numeric = true)
                }
                error?.let { Text(it, color = SystemColors.danger) }
                SystemButton("Guardar", {
                    val v = fields.value.map { it.toDecimalOrNull() }
                    if (name.isBlank() || v[0] == null) {
                        error = "Nombre y calorías son obligatorios"
                        return@SystemButton
                    }
                    scope.launch {
                        val base = FoodEntity.create(
                            name = name.trim(), brand = brand.ifBlank { null }, barcode = barcode ?: existing?.barcode,
                            source = existing?.source ?: FoodSource.CUSTOM, kcal = v[0]!!, protein = v[1] ?: 0.0, carbs = v[2] ?: 0.0,
                            sugars = v[3] ?: 0.0, fat = v[4] ?: 0.0, saturated = v[5] ?: 0.0, fiber = v[6] ?: 0.0, salt = v[7] ?: 0.0,
                            servingG = v[8], unitG = v[9], incomplete = v.take(8).any { it == null },
                        )
                        val id = existing?.let { e -> container.foods.update(base.copy(id = e.id, favorite = e.favorite, lastUsedAt = e.lastUsedAt)); e.id }
                            ?: container.foods.insert(base)
                        onSaved(id)
                    }
                }, Modifier.fillMaxWidth())
            }
        }
    }
}
