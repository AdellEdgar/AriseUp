package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.CloseResult
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.ExerciseDayInput
import io.github.adelledgar.ariseup.domain.model.MissionKind
import io.github.adelledgar.ariseup.domain.model.NutritionDayInput
import io.github.adelledgar.ariseup.domain.model.PlayerProgress
import io.github.adelledgar.ariseup.domain.model.PlayerState
import io.github.adelledgar.ariseup.domain.model.Reward
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import java.time.LocalDate

/** Cierre de día: evalúa misiones y aplica penalización, degradación y racha (spec §2.6, §2.8, §3.3). */
object DayCloser {
    fun closeExercise(s: PlayerState, input: ExerciseDayInput): CloseResult {
        val events = mutableListOf<SystemEvent>()
        var progress = s.progress
        var penalty = s.penalty

        if (penalty != null && !input.restActive) {
            val (p, ps, ev) = Penalty.degradeOnClose(progress, penalty, input.day)
            progress = p
            penalty = ps
            events += ev
        }

        if (!input.restActive) {
            input.outcomes.filter { it.kind == MissionKind.URGENT && !it.completed }.forEach { urgent ->
                val (p, ev) = Leveling.applyXp(progress, -UrgentQuestGenerator.urgentFailurePenalty(progress.level), "Misión urgente fallida")
                progress = p
                events += ev
                events += SystemEvent.UrgentFailed(urgent.id)
            }
        }

        if (input.restActive) return CloseResult(PlayerState(progress, penalty, s.baseline), events)

        val failed = input.outcomes.any { it.mandatory && !it.completed && it.kind in EXERCISE_MANDATORY }
        if (failed) {
            if (penalty == null) {
                val start = input.day.plusDays(1)
                penalty = Penalty.start(start, start, input.currentTargets)
                events += SystemEvent.PenaltyStarted(start.toString())
            }
            progress = resetStreak(progress, events)
        } else if (penalty == null) {
            val streak = progress.streak + 1
            progress = progress.copy(streak = streak)
            events += SystemEvent.StreakChanged(streak)
            if (streak % 7 == 0) {
                progress = progress.copy(stats = Stats.applyGains(progress.stats, Stats.STREAK_WEEK_GAIN))
                events += SystemEvent.StatsChanged(Stats.STREAK_WEEK_GAIN, "Racha de $streak días")
            }
        }
        return CloseResult(PlayerState(progress, penalty, s.baseline), events)
    }

    fun closeNutrition(s: PlayerState, input: NutritionDayInput): CloseResult {
        val events = mutableListOf<SystemEvent>()
        var progress = s.progress
        var penalty = s.penalty
        val level = progress.level

        fun grant(reward: Reward, source: String) {
            val (p, ev) = Leveling.applyXp(progress, reward.xp, source)
            progress = p.copy(stats = Stats.applyGains(p.stats, reward.stats))
            events += ev
            if (reward.stats.isNotEmpty()) events += SystemEvent.StatsChanged(reward.stats, source)
        }

        if (input.nutritionMandatory != null && input.nutritionMet) {
            grant(Rewards.forCompletion(MissionKind.NUTRITION, level), "Nutrición del cazador")
        }
        input.rules.filter { it.met }.forEach {
            grant(Rewards.forCompletion(MissionKind.DIET_RULE, level, shareCount = input.rules.size), "Regla de dieta")
        }
        if (input.mealsWithEntries >= Balance.NUTRITION_MIN_MEALS) {
            grant(Reward(0, Stats.MEALS_LOGGED_GAIN), "Registro completo")
        }

        val failed = (input.nutritionMandatory == true && !input.nutritionMet) || input.rules.any { it.mandatory && !it.met }
        if (failed && !input.restActive && !input.evaluatedDuringRest) {
            if (penalty == null) {
                penalty = startNutritionPenalty(input.evaluatedDuring, s.baseline, level)
                events += SystemEvent.PenaltyStarted(input.evaluatedDuring.toString())
            }
            progress = resetStreak(progress, events)
        }
        return CloseResult(PlayerState(progress, penalty, s.baseline), events)
    }

    private val EXERCISE_MANDATORY = setOf(MissionKind.DAILY, MissionKind.ROUTINE)

    private fun startNutritionPenalty(evaluatedDuring: LocalDate, baseline: DailyTargets?, level: Int) = Penalty.start(
        today = evaluatedDuring,
        firstDegradeOnCloseOf = evaluatedDuring.plusDays(1),
        current = baseline?.let { DailyQuestScaling.targets(it, level, false) } ?: FALLBACK_TARGETS,
    )

    /** Objetivos de penalización si aún no hay test inicial (solo posible con la dieta activada antes del test). */
    private val FALLBACK_TARGETS = DailyTargets(Balance.MIN_REPS * 2, Balance.MIN_REPS * 2, Balance.MIN_REPS * 2, Balance.MIN_RUN_KM)

    private fun resetStreak(p: PlayerProgress, events: MutableList<SystemEvent>): PlayerProgress {
        if (p.streak == 0) return p
        events += SystemEvent.StreakChanged(0)
        return p.copy(streak = 0)
    }
}
