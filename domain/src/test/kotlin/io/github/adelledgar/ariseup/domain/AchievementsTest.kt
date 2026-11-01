package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.Achievement
import io.github.adelledgar.ariseup.domain.model.AchievementCounters
import io.github.adelledgar.ariseup.domain.model.Rank
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AchievementsTest {
    private val empty = AchievementCounters(
        firstDayCompleted = false, maxStreak = 0, penaltiesCleared = 0, penaltyClearedOnFirstDay = false,
        ranksReached = setOf(Rank.E), totalPushUps = 0, totalRunKm = 0.0, nutritionDaysMet = 0,
        recipesCreated = 0, allStatsImprovedInReeval = false, urgentUnderOneHour = false,
    )

    @Test
    fun nothingAtStart() {
        assertTrue(Achievements.newlyUnlocked(empty, emptySet()).isEmpty())
    }

    @Test
    fun achievementsOnlyNew() {
        val c = empty.copy(maxStreak = 8, firstDayCompleted = true)
        assertEquals(listOf(Achievement.STREAK_7), Achievements.newlyUnlocked(c, setOf(Achievement.FIRST_DAY)))
    }

    @Test
    fun thresholds() {
        val c = empty.copy(totalPushUps = 10_000, totalRunKm = 100.0, ranksReached = setOf(Rank.E, Rank.D, Rank.C), penaltiesCleared = 1)
        val got = Achievements.newlyUnlocked(c, emptySet()).toSet()
        assertEquals(
            setOf(Achievement.PUSH_1K, Achievement.PUSH_10K, Achievement.RUN_100, Achievement.RANK_D, Achievement.RANK_C, Achievement.PENALTY_SURVIVOR),
            got,
        )
    }

    @Test
    fun titles() {
        assertEquals("El que superó la penalización", Achievement.PENALTY_SURVIVOR.title)
        assertEquals("Corredor incansable", Achievement.RUN_100.title)
    }
}
