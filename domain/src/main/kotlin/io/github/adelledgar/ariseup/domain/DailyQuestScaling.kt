package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.FitnessTest
import kotlin.math.roundToInt

/** Misión diaria "Preparación para volverse fuerte" (spec §2.3). */
object DailyQuestScaling {
    fun baseline(test: FitnessTest): DailyTargets {
        require(test.exerciseComplete) { "El test no tiene los cuatro ejercicios" }
        return DailyTargets(
            pushUps = repsBaseline(test.pushUps!!),
            sitUps = repsBaseline(test.sitUps!!),
            squats = repsBaseline(test.squats!!),
            runKm = kmBaseline(test.cooperMeters!!),
        )
    }

    /** Una reevaluación solo sube la base, nunca la baja. Los datos ausentes conservan el valor anterior. */
    fun raiseBaseline(old: DailyTargets, newTest: FitnessTest): DailyTargets = DailyTargets(
        pushUps = maxOf(old.pushUps, newTest.pushUps?.let(::repsBaseline) ?: 0),
        sitUps = maxOf(old.sitUps, newTest.sitUps?.let(::repsBaseline) ?: 0),
        squats = maxOf(old.squats, newTest.squats?.let(::repsBaseline) ?: 0),
        runKm = maxOf(old.runKm, newTest.cooperMeters?.let(::kmBaseline) ?: 0.0),
    )

    fun targets(baseline: DailyTargets, level: Int, returningFromRest: Boolean): DailyTargets {
        val t = DailyTargets(
            pushUps = reps(baseline.pushUps, level),
            sitUps = reps(baseline.sitUps, level),
            squats = reps(baseline.squats, level),
            runKm = km(baseline.runKm, level),
        )
        if (!returningFromRest) return t
        val f = Balance.REST_RETURN_FACTOR
        return DailyTargets(
            pushUps = (t.pushUps * f).roundToInt(),
            sitUps = (t.sitUps * f).roundToInt(),
            squats = (t.squats * f).roundToInt(),
            runKm = round1(t.runKm * f),
        )
    }

    private fun repsBaseline(max: Int) = maxOf(Balance.MIN_REPS, (max * Balance.BASELINE_FRACTION).roundToInt())

    private fun kmBaseline(cooperMeters: Int): Double {
        val km = cooperMeters / 1000.0 * Balance.BASELINE_FRACTION
        return maxOf(Balance.MIN_RUN_KM, (km * 2).roundToInt() / 2.0)
    }

    private fun progressFraction(level: Int) = ((level - 1).toDouble() / (Balance.GOAL_LEVEL - 1)).coerceIn(0.0, 1.0)

    private fun extraSteps(level: Int) = maxOf(0, (level - Balance.GOAL_LEVEL) / 2)

    private fun reps(t0: Int, level: Int): Int {
        val goal = maxOf(Balance.GOAL_REPS, t0)
        return (t0 + (goal - t0) * progressFraction(level)).roundToInt() + extraSteps(level) * Balance.POST_GOAL_REPS_PER_2LVL
    }

    private fun km(t0: Double, level: Int): Double {
        val goal = maxOf(Balance.GOAL_KM, t0)
        return round1(t0 + (goal - t0) * progressFraction(level) + extraSteps(level) * Balance.POST_GOAL_KM_PER_2LVL)
    }

    private fun round1(x: Double) = (x * 10).roundToInt() / 10.0
}
