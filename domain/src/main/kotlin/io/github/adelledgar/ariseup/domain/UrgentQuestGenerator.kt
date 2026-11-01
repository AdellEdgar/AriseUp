package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.DailyExercise
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.ceil
import kotlin.math.roundToLong
import kotlin.random.Random

data class UrgentSpec(val exercise: DailyExercise, val amount: Double, val appearAt: LocalTime, val durationHours: Int)

/** Misiones urgentes (spec §2.5). */
object UrgentQuestGenerator {
    private const val FIRST_MINUTE = 10 * 60
    private const val LAST_MINUTE = 19 * 60

    fun maybeGenerate(
        day: LocalDate,
        rng: Random,
        urgentsThisWeek: Int,
        inRest: Boolean,
        inPenalty: Boolean,
        targets: DailyTargets,
    ): UrgentSpec? {
        if (inRest || inPenalty || urgentsThisWeek >= Balance.URGENT_MAX_PER_WEEK) return null
        if (rng.nextDouble() >= Balance.URGENT_PROBABILITY) return null
        val exercise = DailyExercise.entries[rng.nextInt(DailyExercise.entries.size)]
        val fraction = 0.3 + rng.nextDouble() * 0.3
        val amount = when (exercise) {
            DailyExercise.PUSH_UPS -> ceil(targets.pushUps * fraction)
            DailyExercise.SIT_UPS -> ceil(targets.sitUps * fraction)
            DailyExercise.SQUATS -> ceil(targets.squats * fraction)
            DailyExercise.RUN -> maxOf(0.5, ceil(targets.runKm * fraction * 2) / 2)
        }
        val minute = FIRST_MINUTE + rng.nextInt(LAST_MINUTE - FIRST_MINUTE + 1)
        return UrgentSpec(exercise, amount, LocalTime.of(minute / 60, minute % 60), 3 + rng.nextInt(4))
    }

    fun urgentFailurePenalty(level: Int): Long = (Balance.URGENT_FAIL_XP_LOSS * Leveling.xpToNext(level)).roundToLong()
}
