package io.github.adelledgar.ariseup.system

import androidx.room.withTransaction
import io.github.adelledgar.ariseup.data.GameClock
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.DayEntity
import io.github.adelledgar.ariseup.data.db.DayStatus
import io.github.adelledgar.ariseup.data.prefs.SettingsStore
import io.github.adelledgar.ariseup.data.repo.FoodRepository
import io.github.adelledgar.ariseup.data.repo.PlayerRepository
import io.github.adelledgar.ariseup.domain.Balance
import io.github.adelledgar.ariseup.domain.DayCloser
import io.github.adelledgar.ariseup.domain.Nutrition
import io.github.adelledgar.ariseup.domain.RestMode
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.ExerciseDayInput
import io.github.adelledgar.ariseup.domain.model.MissionKind
import io.github.adelledgar.ariseup.domain.model.MissionOutcome
import io.github.adelledgar.ariseup.domain.model.NutritionDayInput
import io.github.adelledgar.ariseup.domain.model.RuleOutcome
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Cierra en orden todos los días pendientes. Idempotente: el estado de cada [DayEntity] dice qué falta,
 * así que llamarlo varias veces no aplica nada dos veces.
 */
class DayCloseCoordinator(
    private val db: AriseUpDatabase,
    private val players: PlayerRepository,
    private val foods: FoodRepository,
    private val settings: SettingsStore,
    private val gameClock: GameClock,
    private val planner: MissionPlanner,
) {
    /** Se llama antes de cerrar un día (sincronización de Health Connect). */
    var beforeClose: suspend (LocalDate) -> Unit = {}

    /** Se llama tras cada cierre (revisión de logros). */
    var afterClose: suspend () -> List<SystemEvent> = { emptyList() }

    private val mutex = Mutex()

    suspend fun catchUp(): List<SystemEvent> = mutex.withLock {
        if (players.current() == null) return emptyList()
        val today = gameClock.today()
        val last = db.dayDao().lastDate()
        if (last != null && today < last) return emptyList()

        expireUnconfirmedRests(today)
        if (last != null) {
            var d = last.plusDays(1)
            while (d < today) {
                planner.ensureDay(d, generateUrgent = false)
                d = d.plusDays(1)
            }
        }

        val events = mutableListOf<SystemEvent>()
        val pending = (db.dayDao().withStatusBefore(today, DayStatus.OPEN) + db.dayDao().withStatus(DayStatus.EXERCISE_CLOSED))
            .distinctBy { it.date }.sortedBy { it.date }
        for (day in pending) {
            var current = day
            if (current.status == DayStatus.OPEN) {
                beforeClose(current.date)
                events += closeExercise(current)
                current = current.copy(status = DayStatus.EXERCISE_CLOSED)
            }
            if (current.status == DayStatus.EXERCISE_CLOSED && nutritionMarginPassed(current.date)) {
                events += closeNutrition(current)
            }
        }
        planner.ensureDay(today)
        if (events.isNotEmpty()) events += afterClose()
        events
    }

    private suspend fun closeExercise(day: DayEntity): List<SystemEvent> = db.withTransaction {
        val state = players.current() ?: return@withTransaction emptyList()
        val outcomes = db.missionDao().forDate(day.date)
            .filter { it.kind in EXERCISE_KINDS }
            .map { MissionOutcome(it.id, it.kind, it.mandatory, it.completedAt != null) }
        val targets = day.targets ?: state.baseline ?: DailyTargets(Balance.MIN_REPS, Balance.MIN_REPS, Balance.MIN_REPS, Balance.MIN_RUN_KM)
        val r = DayCloser.closeExercise(state, ExerciseDayInput(day.date, outcomes, day.restActive, targets))
        players.saveState(r.state, r.events, day.date)
        db.dayDao().upsert(day.copy(status = DayStatus.EXERCISE_CLOSED))
        r.events
    }

    private suspend fun closeNutrition(day: DayEntity): List<SystemEvent> = db.withTransaction {
        val state = players.current() ?: return@withTransaction emptyList()
        val player = players.currentEntity()!!
        val dayMissions = db.missionDao().forDate(day.date)
        val nutritionMission = dayMissions.firstOrNull { it.kind == MissionKind.NUTRITION }
        val weight = db.bodyDao().latest()?.weightKg ?: DEFAULT_WEIGHT
        val targets = Nutrition.targets(player.profile(), weight, day.date, settings.current().targetOverrides)
        val totals = foods.dayTotals(day.date)
        val meals = foods.mealsWithEntries(day.date)
        val met = Nutrition.nutritionMet(targets, totals, meals)
        val ruleValues = foods.ruleLogs(day.date).associate { it.ruleId to it.value }
        val rules = dayMissions.filter { it.kind == MissionKind.DIET_RULE }.mapNotNull { m ->
            val rule = m.refId?.let { db.foodDao().rule(it) } ?: return@mapNotNull null
            val ok = rule.met(ruleValues[rule.id] ?: 0)
            if (ok) db.missionDao().markCompleted(m.id, gameClock.now())
            RuleOutcome(m.mandatory, ok)
        }
        if (met && nutritionMission != null) db.missionDao().markCompleted(nutritionMission.id, gameClock.now())
        val input = NutritionDayInput(
            day = day.date,
            evaluatedDuring = day.date.plusDays(1),
            nutritionMandatory = nutritionMission?.mandatory,
            nutritionMet = met,
            rules = rules,
            mealsWithEntries = meals,
            restActive = day.restActive,
        )
        val r = DayCloser.closeNutrition(state, input)
        players.saveState(r.state, r.events, day.date)
        db.dayDao().upsert(day.copy(status = DayStatus.CLOSED))
        r.events
    }

    /** ¿Se puede editar todavía la dieta de [day]? (hasta 12 h después del reinicio siguiente). */
    suspend fun nutritionEditable(day: LocalDate): Boolean = !nutritionMarginPassed(day)

    private suspend fun nutritionMarginPassed(day: LocalDate): Boolean {
        val end = gameClock.startOf(day.plusDays(1)).plus(Balance.NUTRITION_MARGIN_HOURS, ChronoUnit.HOURS)
        return !gameClock.now().isBefore(end)
    }

    /** Un descanso por enfermedad o lesión sin confirmar en 24 h termina hoy. */
    private suspend fun expireUnconfirmedRests(today: LocalDate) {
        val now = LocalDateTime.ofInstant(gameClock.now(), gameClock.clock.zone)
        db.restDao().all().filter { it.endedEarly == null && it.toDomain().covers(today) }.forEach {
            if (RestMode.expiredUnconfirmed(it.toDomain(), now, it.confirmPromptAt)) {
                db.restDao().upsert(it.copy(endedEarly = today))
            }
        }
    }

    private companion object {
        val EXERCISE_KINDS = setOf(MissionKind.DAILY, MissionKind.ROUTINE, MissionKind.URGENT)
        const val DEFAULT_WEIGHT = 70.0
    }
}
