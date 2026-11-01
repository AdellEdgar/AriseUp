package io.github.adelledgar.ariseup.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.adelledgar.ariseup.data.GameClock
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.DietRuleEntity
import io.github.adelledgar.ariseup.data.db.FoodLogEntity
import io.github.adelledgar.ariseup.data.db.Meal
import io.github.adelledgar.ariseup.data.prefs.SettingsStore
import io.github.adelledgar.ariseup.data.repo.BodyRepository
import io.github.adelledgar.ariseup.data.repo.FoodRepository
import io.github.adelledgar.ariseup.data.repo.PlayerRepository
import io.github.adelledgar.ariseup.domain.Nutrition
import io.github.adelledgar.ariseup.domain.model.Macros
import io.github.adelledgar.ariseup.domain.model.NutritionTargets
import io.github.adelledgar.ariseup.system.DayCloseCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LogLine(val log: FoodLogEntity, val name: String, val amount: String, val macros: Macros)

data class RuleLine(val rule: DietRuleEntity, val value: Int)

data class NutritionDay(
    val date: LocalDate,
    val meals: Map<Meal, List<LogLine>>,
    val totals: Macros,
    val targets: NutritionTargets,
    val rules: List<RuleLine>,
)

class NutritionViewModel(
    private val db: AriseUpDatabase,
    private val players: PlayerRepository,
    private val foods: FoodRepository,
    private val body: BodyRepository,
    private val settings: SettingsStore,
    private val gameClock: GameClock,
    private val coordinator: DayCloseCoordinator,
) : ViewModel() {
    suspend fun today(): LocalDate = gameClock.today()

    /** El día en curso, y el anterior hasta 12 h después del reinicio (spec §3.3). */
    suspend fun isEditable(date: LocalDate): Boolean {
        val today = gameClock.today()
        return date == today || (date < today && coordinator.nutritionEditable(date))
    }

    fun dayState(date: LocalDate): Flow<NutritionDay> = combine(
        foods.observeLogs(date),
        foods.observeRules(),
        foods.observeRuleLogs(date),
        settings.settings,
        body.observeLatest(),
    ) { logs, rules, ruleLogs, s, latest ->
        val lines = logs.map { log ->
            val m = foods.macrosOf(log)
            val (name, amount) = when {
                log.foodId != null -> (foods.get(log.foodId)?.name ?: "Alimento borrado") to "${"%.0f".format(log.grams ?: 0.0)} g"
                else -> (log.recipeId?.let { foods.recipe(it)?.name } ?: "Receta borrada") to "${fmtServings(log.servings ?: 1.0)} rac."
            }
            LogLine(log, name, amount, m)
        }
        val profile = players.currentEntity()?.profile()
        val targets = if (profile == null) NutritionTargets(2000, 120, 60, 250) else Nutrition.targets(profile, latest?.weightKg ?: 70.0, date, s.targetOverrides)
        val values = ruleLogs.associate { it.ruleId to it.value }
        NutritionDay(
            date = date,
            meals = Meal.entries.associateWith { meal -> lines.filter { it.log.meal == meal } },
            totals = lines.fold(Macros.ZERO) { acc, l -> acc + l.macros },
            targets = targets,
            rules = rules.map { RuleLine(it, values[it.id] ?: 0) },
        )
    }

    fun delete(logId: Long) {
        viewModelScope.launch { foods.deleteLog(logId) }
    }

    fun setRule(date: LocalDate, ruleId: Long, value: Int) {
        viewModelScope.launch { if (isEditable(date)) foods.setRuleValue(date, ruleId, value) }
    }

    fun copyMeal(from: LocalDate, meal: Meal, to: LocalDate) {
        viewModelScope.launch { if (isEditable(to)) foods.copyMeal(from, meal, to, meal) }
    }

}

fun fmtServings(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)
