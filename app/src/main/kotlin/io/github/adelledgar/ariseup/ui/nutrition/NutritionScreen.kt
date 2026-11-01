package io.github.adelledgar.ariseup.ui.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.adelledgar.ariseup.data.db.Meal
import io.github.adelledgar.ariseup.data.db.RuleType
import io.github.adelledgar.ariseup.di.AppContainer
import io.github.adelledgar.ariseup.ui.ScreenColumn
import io.github.adelledgar.ariseup.ui.components.ChoiceChips
import io.github.adelledgar.ariseup.ui.components.StatBar
import io.github.adelledgar.ariseup.ui.components.SystemButton
import io.github.adelledgar.ariseup.ui.components.SystemWindow
import io.github.adelledgar.ariseup.ui.containerViewModel
import io.github.adelledgar.ariseup.ui.theme.LocalSystemPalette
import io.github.adelledgar.ariseup.ui.theme.SystemColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NutritionScreen(
    container: AppContainer,
    onSearch: (LocalDate, Meal) -> Unit,
    onScan: (LocalDate, Meal) -> Unit,
    onRecipes: () -> Unit,
    onRules: () -> Unit,
) {
    val vm = containerViewModel {
        NutritionViewModel(container.db, container.players, container.foods, container.body, container.settings, container.gameClock, container.coordinator)
    }
    var today by remember { mutableStateOf<LocalDate?>(null) }
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var yesterdayEditable by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        today = vm.today()
        date = date ?: today
        yesterdayEditable = vm.isEditable(today!!.minusDays(1))
    }
    val d = date ?: return
    val day by remember(d) { vm.dayState(d) }.collectAsStateWithLifecycle(null)
    val accent = LocalSystemPalette.current.accent
    val editable = d == today || (d == today?.minusDays(1) && yesterdayEditable)

    ScreenColumn {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("[Nutrición]", style = MaterialTheme.typography.headlineMedium, color = accent, modifier = Modifier.weight(1f))
                IconButton(onClick = { onScan(d, Meal.SNACK) }, enabled = editable) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear", tint = accent)
                }
            }
            val options = listOfNotNull(today, today?.minusDays(1)?.takeIf { yesterdayEditable })
            if (options.size > 1) ChoiceChips(options, d, { if (it == today) "Hoy" else "Ayer (hasta las 12:00)" }, { date = it })
            else Text(d.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es"))), color = SystemColors.muted)
        }
        val s = day ?: return@ScreenColumn
        item {
            SystemWindow("Balance del día") {
                StatBar("Calorías", s.totals.kcal.toFloat(), s.targets.kcal.toFloat(), valueText = "${s.totals.kcal.toInt()}/${s.targets.kcal} kcal")
                Spacer(Modifier.height(8.dp))
                StatBar("Proteína", s.totals.protein.toFloat(), s.targets.proteinG.toFloat(), valueText = "${s.totals.protein.toInt()}/${s.targets.proteinG} g")
                Spacer(Modifier.height(8.dp))
                StatBar("Hidratos", s.totals.carbs.toFloat(), s.targets.carbsG.toFloat(), valueText = "${s.totals.carbs.toInt()}/${s.targets.carbsG} g")
                Spacer(Modifier.height(8.dp))
                StatBar("Grasa", s.totals.fat.toFloat(), s.targets.fatG.toFloat(), valueText = "${s.totals.fat.toInt()}/${s.targets.fatG} g")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Azúcares ${s.totals.sugars.toInt()} g · Fibra ${s.totals.fiber.toInt()} g · Sal ${"%.1f".format(Locale("es"), s.totals.salt)} g",
                    color = SystemColors.muted, style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Meal.entries.forEach { meal ->
            item(key = meal.name) {
                val lines = s.meals[meal].orEmpty()
                SystemWindow("${meal.label} · ${lines.sumOf { it.macros.kcal }.toInt()} kcal") {
                    lines.forEach { l ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(l.name, color = SystemColors.text)
                                Text("${l.amount} · ${l.macros.kcal.toInt()} kcal · P ${l.macros.protein.toInt()} g", color = SystemColors.muted, style = MaterialTheme.typography.bodySmall)
                            }
                            if (editable) IconButton(onClick = { vm.delete(l.log.id) }) { Icon(Icons.Default.Close, contentDescription = "Quitar", tint = SystemColors.muted) }
                        }
                    }
                    if (editable) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { onSearch(d, meal) }) { Icon(Icons.Default.Add, null); Text("Añadir") }
                        TextButton(onClick = { onScan(d, meal) }) { Icon(Icons.Default.QrCodeScanner, null); Text("Escanear") }
                        TextButton(onClick = { vm.copyMeal(d.minusDays(1), meal, d) }) { Icon(Icons.Default.ContentCopy, null); Text("Copiar de ayer") }
                    }
                }
            }
        }
        item {
            SystemWindow("Reglas de dieta") {
                if (s.rules.isEmpty()) Text("Aún no tienes reglas. Créalas para que el Sistema te las exija.", color = SystemColors.muted)
                s.rules.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(r.rule.name + if (r.rule.mandatory) "" else " (opcional)", color = SystemColors.text, modifier = Modifier.weight(1f))
                        when (r.rule.type) {
                            RuleType.CHECK -> Checkbox(
                                checked = r.value >= 1, enabled = editable,
                                onCheckedChange = { vm.setRule(d, r.rule.id, if (it) 1 else 0) },
                                colors = CheckboxDefaults.colors(checkedColor = accent),
                            )
                            RuleType.COUNTER -> Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { vm.setRule(d, r.rule.id, r.value - 1) }, enabled = editable) { Text("−") }
                                Text("${r.value}/${r.rule.target ?: 1}", color = if (r.rule.met(r.value)) SystemColors.success else SystemColors.text)
                                TextButton(onClick = { vm.setRule(d, r.rule.id, r.value + 1) }, enabled = editable) { Text("+") }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SystemButton("Gestionar reglas", onRules, Modifier.weight(1f))
                    SystemButton("Recetas", onRecipes, Modifier.weight(1f))
                }
            }
        }
    }
}
