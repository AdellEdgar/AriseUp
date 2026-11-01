package io.github.adelledgar.ariseup.data.repo

import androidx.room.withTransaction
import io.github.adelledgar.ariseup.data.GameClock
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.BodyEntryEntity
import io.github.adelledgar.ariseup.domain.Balance
import io.github.adelledgar.ariseup.domain.DailyQuestScaling
import io.github.adelledgar.ariseup.domain.Reevaluation
import io.github.adelledgar.ariseup.domain.Stats
import io.github.adelledgar.ariseup.domain.model.FitnessTest
import io.github.adelledgar.ariseup.domain.model.SystemEvent

/** Aplica una reevaluación: stats, base de la misión diaria, peso y test (spec §2.2 y §2.7). */
class ReevaluationService(
    private val db: AriseUpDatabase,
    private val players: PlayerRepository,
    private val body: BodyRepository,
    private val foods: FoodRepository,
    private val missions: MissionRepository,
    private val gameClock: GameClock,
) {
    suspend fun apply(missionId: Long?, test: FitnessTest): List<SystemEvent> {
        val today = gameClock.today()
        val result = db.withTransaction {
            val state = checkNotNull(players.current())
            val player = checkNotNull(players.currentEntity())
            val now = test.copy(date = today)
            val prev = body.latestTest()
            val events = mutableListOf<SystemEvent>()
            var newState = state
            if (prev != null) {
                val deltas = Reevaluation.statDeltas(prev, now, player.goal, loggingPct(today))
                if (deltas.values.any { it != 0.0 }) {
                    newState = newState.copy(progress = newState.progress.copy(stats = Stats.applyGains(newState.progress.stats, deltas)))
                    events += SystemEvent.StatsChanged(deltas, REASON)
                }
            }
            val baseline = when {
                newState.baseline != null -> DailyQuestScaling.raiseBaseline(newState.baseline!!, now)
                now.exerciseComplete -> DailyQuestScaling.baseline(now)
                else -> null
            }
            newState = newState.copy(baseline = baseline)
            players.saveState(newState, events, today)
            body.saveTest(now, isInitial = prev == null)
            if (now.weightKg != null) body.add(BodyEntryEntity(date = today, weightKg = now.weightKg, waistCm = now.waistCm, bodyFatPct = now.bodyFatPct))
            events
        }
        return result + (missionId?.let { missions.complete(it) } ?: emptyList())
    }

    /** % de días con registro completo (≥3 comidas) en las últimas 4 semanas. */
    private suspend fun loggingPct(today: java.time.LocalDate): Double {
        val days = Balance.REEVALUATION_EVERY_DAYS
        val complete = (1..days).count { foods.mealsWithEntries(today.minusDays(it.toLong())) >= Balance.NUTRITION_MIN_MEALS }
        return complete * 100.0 / days
    }

    companion object {
        const val REASON = "Reevaluación"
    }
}
