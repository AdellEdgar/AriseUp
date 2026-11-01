package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.DailyExercise
import io.github.adelledgar.ariseup.domain.model.MissionKind
import io.github.adelledgar.ariseup.domain.model.Reward
import io.github.adelledgar.ariseup.domain.model.RoutineCategory
import io.github.adelledgar.ariseup.domain.model.Stat
import kotlin.math.roundToLong

/** Recompensa al completar una misión (spec §2.1 y §2.2). */
object Rewards {
    fun forCompletion(
        kind: MissionKind,
        level: Int,
        shareCount: Int = 1,
        category: RoutineCategory? = null,
        urgentExercise: DailyExercise? = null,
    ): Reward {
        val pool = Leveling.dayPool(level).toDouble()
        fun xp(share: Double) = (pool * share / shareCount.coerceAtLeast(1)).roundToLong()
        return when (kind) {
            MissionKind.DAILY -> Reward(xp(Balance.SHARE_DAILY), mapOf(Stat.FUE to 0.3, Stat.AGI to 0.3, Stat.VIT to 0.1))
            MissionKind.ROUTINE -> Reward(
                xp(Balance.SHARE_ROUTINES),
                mapOf(requireNotNull(category) { "Una rutina necesita categoría" }.stat to 0.3),
            )
            MissionKind.NUTRITION -> Reward(xp(Balance.SHARE_NUTRITION), mapOf(Stat.VIT to 0.3, Stat.INT to 0.1))
            MissionKind.DIET_RULE -> Reward(xp(Balance.SHARE_RULES), mapOf(Stat.INT to 0.05))
            MissionKind.WEIGH_IN -> Reward(xp(Balance.SHARE_WEIGH_IN), mapOf(Stat.VIT to 0.05))
            MissionKind.URGENT -> Reward(
                xp(Balance.SHARE_URGENT),
                mapOf((if (urgentExercise == DailyExercise.RUN) Stat.AGI else Stat.FUE) to 0.3),
            )
            MissionKind.PENALTY, MissionKind.REEVALUATION -> Reward(0, emptyMap())
        }
    }
}
