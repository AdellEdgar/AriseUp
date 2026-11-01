package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.DailyExercise
import io.github.adelledgar.ariseup.domain.model.MissionKind
import io.github.adelledgar.ariseup.domain.model.Reward
import io.github.adelledgar.ariseup.domain.model.RoutineCategory
import io.github.adelledgar.ariseup.domain.model.Stat
import kotlin.test.Test
import kotlin.test.assertEquals

class RewardsTest {
    @Test
    fun dailyRewardAtLevel1() {
        assertEquals(Reward(45, mapOf(Stat.FUE to 0.3, Stat.AGI to 0.3, Stat.VIT to 0.1)), Rewards.forCompletion(MissionKind.DAILY, 1))
    }

    @Test
    fun routineShareSplits() {
        val r = Rewards.forCompletion(MissionKind.ROUTINE, 1, shareCount = 2, category = RoutineCategory.MOBILITY)
        assertEquals(10, r.xp)
        assertEquals(mapOf(Stat.SEN to 0.3), r.stats)
    }

    @Test
    fun nutritionReward() {
        assertEquals(Reward(25, mapOf(Stat.VIT to 0.3, Stat.INT to 0.1)), Rewards.forCompletion(MissionKind.NUTRITION, 1))
    }

    @Test
    fun ruleReward() {
        val r = Rewards.forCompletion(MissionKind.DIET_RULE, 1, shareCount = 4)
        assertEquals(3, r.xp)
        assertEquals(mapOf(Stat.INT to 0.05), r.stats)
    }

    @Test
    fun weighInReward() {
        assertEquals(Reward(3, mapOf(Stat.VIT to 0.05)), Rewards.forCompletion(MissionKind.WEIGH_IN, 1))
    }

    @Test
    fun urgentReward() {
        assertEquals(Reward(50, mapOf(Stat.AGI to 0.3)), Rewards.forCompletion(MissionKind.URGENT, 1, urgentExercise = DailyExercise.RUN))
        assertEquals(Reward(50, mapOf(Stat.FUE to 0.3)), Rewards.forCompletion(MissionKind.URGENT, 1, urgentExercise = DailyExercise.SQUATS))
    }

    @Test
    fun penaltyAndReevaluationGiveNothing() {
        assertEquals(Reward(0, emptyMap()), Rewards.forCompletion(MissionKind.PENALTY, 10))
        assertEquals(Reward(0, emptyMap()), Rewards.forCompletion(MissionKind.REEVALUATION, 10))
    }

    @Test
    fun scalesWithLevel() {
        assertEquals(357, Rewards.forCompletion(MissionKind.DAILY, 10).xp)
    }
}
