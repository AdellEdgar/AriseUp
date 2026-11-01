package io.github.adelledgar.ariseup.system

import androidx.room.withTransaction
import io.github.adelledgar.ariseup.data.GameClock
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.DayEntity
import io.github.adelledgar.ariseup.data.db.DayStatus
import io.github.adelledgar.ariseup.data.prefs.NutritionMode
import io.github.adelledgar.ariseup.data.prefs.SettingsStore
import io.github.adelledgar.ariseup.data.prefs.WeighInFrequency
import io.github.adelledgar.ariseup.data.repo.FoodRepository
import io.github.adelledgar.ariseup.data.repo.MissionRepository
import io.github.adelledgar.ariseup.data.repo.PlayerRepository
import io.github.adelledgar.ariseup.domain.Balance
import io.github.adelledgar.ariseup.domain.DailyQuestScaling
import io.github.adelledgar.ariseup.domain.RestMode
import io.github.adelledgar.ariseup.domain.UrgentQuestGenerator
import io.github.adelledgar.ariseup.domain.model.MissionKind
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/** Crea las misiones de un día (una sola vez por día). */
class MissionPlanner(
    private val db: AriseUpDatabase,
    private val players: PlayerRepository,
    private val missions: MissionRepository,
    private val foods: FoodRepository,
    private val settings: SettingsStore,
    private val gameClock: GameClock,
    private val rng: Random = Random.Default,
) {
    /** Avisa de una urgente nueva: (id de misión, instante en que aparece). */
    var onUrgentCreated: (Long, Instant) -> Unit = { _, _ -> }

    suspend fun ensureDay(day: LocalDate, generateUrgent: Boolean = true) {
        val urgent = db.withTransaction {
            val state = players.current() ?: return@withTransaction null
            ensurePenaltyMission(day)
            if (db.dayDao().get(day) != null) return@withTransaction null

            val s = settings.current()
            val rests = db.restDao().all().map { it.toDomain() }
            val rest = rests.any { it.covers(day) }
            // El día en que se crea el Jugador es de prueba: nada es obligatorio (se puede empezar a las 23:30).
            val optional = rest || players.currentEntity()?.createdOn == day
            val targets = state.baseline?.let {
                DailyQuestScaling.targets(it, state.progress.level, RestMode.isReturnWeek(day, rests))
            }
            db.dayDao().upsert(DayEntity(day, DayStatus.OPEN, restActive = rest, targets = targets))

            if (targets != null) missions.createDaily(day, targets, mandatory = !optional)

            val dayBit = 1 shl (day.dayOfWeek.value - 1)
            db.routineDao().active().filter { it.daysOfWeekMask and dayBit != 0 }.forEach {
                missions.createSimple(day, MissionKind.ROUTINE, it.name, mandatory = it.mandatory && !optional, refId = it.id)
            }

            when (s.nutritionMode) {
                NutritionMode.MANDATORY -> missions.createSimple(day, MissionKind.NUTRITION, "Nutrición del cazador", mandatory = !optional)
                NutritionMode.OPTIONAL -> missions.createSimple(day, MissionKind.NUTRITION, "Nutrición del cazador", mandatory = false)
                NutritionMode.OFF -> Unit
            }
            foods.activeRules().forEach {
                missions.createSimple(day, MissionKind.DIET_RULE, it.name, mandatory = it.mandatory && !optional, refId = it.id)
            }

            if (weighInDue(day, s.weighInFrequency)) {
                missions.createSimple(day, MissionKind.WEIGH_IN, "Registro de condición física", mandatory = false)
            }
            if (reevaluationDue(day, rests)) {
                missions.createSimple(day, MissionKind.REEVALUATION, "Reevaluación del Jugador", mandatory = false)
            }

            if (!generateUrgent || targets == null) return@withTransaction null
            val monday = day.minusDays((day.dayOfWeek.value - 1).toLong())
            val spec = UrgentQuestGenerator.maybeGenerate(
                day, rng, db.missionDao().urgentCount(monday, monday.plusDays(6)), optional, state.penalty != null, targets,
            ) ?: return@withTransaction null
            val dayStart = gameClock.startOf(day)
            val dayEnd = gameClock.startOf(day.plusDays(1))
            val scheduled = LocalDateTime.of(day, spec.appearAt).atZone(gameClock.clock.zone).toInstant()
            val appear = maxOf(dayStart, scheduled, gameClock.now())
            val deadline = appear.plus(spec.durationHours.toLong(), ChronoUnit.HOURS)
            // Nunca una urgente que ya no se pueda cumplir entera dentro del día.
            if (deadline > dayEnd) return@withTransaction null
            missions.createUrgent(day, spec.exercise, spec.amount, appear, deadline) to appear
        }
        urgent?.let { (id, at) -> onUrgentCreated(id, at) }
    }

    /** La misión de penalización es una sola y sigue abierta de un día a otro hasta cumplirse. */
    private suspend fun ensurePenaltyMission(day: LocalDate) {
        val penalty = db.penaltyDao().get() ?: return
        if (db.missionDao().openOfKind(MissionKind.PENALTY) != null) return
        missions.createPenalty(maxOf(day, penalty.startedOn), penalty.targets)
    }

    private suspend fun weighInDue(day: LocalDate, freq: WeighInFrequency): Boolean {
        if (freq == WeighInFrequency.OFF) return false
        val latest = db.bodyDao().latest() ?: return true
        val every = if (freq == WeighInFrequency.DAILY) 1 else 2
        return ChronoUnit.DAYS.between(latest.date, day) >= every
    }

    private suspend fun reevaluationDue(day: LocalDate, rests: List<io.github.adelledgar.ariseup.domain.model.RestPeriod>): Boolean {
        val last = db.bodyDao().latestTest() ?: return false
        if (!last.toDomain().exerciseComplete) return false
        val days = ChronoUnit.DAYS.between(last.date, day)
        if (days >= Balance.REEVALUATION_EVERY_DAYS) return true
        val endedLongRest = rests.any { r -> RestMode.suggestsReevaluation(r) && r.effectiveEnd?.let { it < day && it >= last.date } == true }
        return endedLongRest
    }
}
