package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.Achievement
import io.github.adelledgar.ariseup.domain.model.AchievementCounters
import io.github.adelledgar.ariseup.domain.model.Rank

object Achievements {
    fun newlyUnlocked(c: AchievementCounters, already: Set<Achievement>): List<Achievement> =
        Achievement.entries.filter { it !in already && earned(it, c) }

    private fun earned(a: Achievement, c: AchievementCounters): Boolean = when (a) {
        Achievement.FIRST_DAY -> c.firstDayCompleted
        Achievement.STREAK_7 -> c.maxStreak >= 7
        Achievement.STREAK_30 -> c.maxStreak >= 30
        Achievement.STREAK_100 -> c.maxStreak >= 100
        Achievement.STREAK_365 -> c.maxStreak >= 365
        Achievement.PENALTY_SURVIVOR -> c.penaltiesCleared >= 1
        Achievement.PENALTY_SAME_DAY -> c.penaltyClearedOnFirstDay
        Achievement.RANK_D -> Rank.D in c.ranksReached
        Achievement.RANK_C -> Rank.C in c.ranksReached
        Achievement.RANK_B -> Rank.B in c.ranksReached
        Achievement.RANK_A -> Rank.A in c.ranksReached
        Achievement.RANK_S -> Rank.S in c.ranksReached
        Achievement.PUSH_1K -> c.totalPushUps >= 1_000
        Achievement.PUSH_10K -> c.totalPushUps >= 10_000
        Achievement.RUN_100 -> c.totalRunKm >= 100.0
        Achievement.NUTRITION_30 -> c.nutritionDaysMet >= 30
        Achievement.FIRST_RECIPE -> c.recipesCreated >= 1
        Achievement.ALL_STATS_UP -> c.allStatsImprovedInReeval
        Achievement.URGENT_FAST -> c.urgentUnderOneHour
    }
}
