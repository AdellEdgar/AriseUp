package io.github.adelledgar.ariseup.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.TestEnv
import io.github.adelledgar.ariseup.data.repo.AchievementService
import io.github.adelledgar.ariseup.domain.model.Achievement
import io.github.adelledgar.ariseup.domain.model.DailyExercise
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AchievementServiceTest {
    private val env = TestEnv()
    private val service = AchievementService(env.db, env.events)

    @Test
    fun thousandPushUpsUnlocksOnce() = runTest {
        env.seedPlayer()
        val id = env.missions.createDaily(env.today, DailyTargets(2000, 10, 10, 1.0))
        env.missions.addDailyProgress(id, DailyExercise.PUSH_UPS, 1000.0)
        val first = service.check()
        assertTrue(first.contains(SystemEvent.AchievementUnlocked(Achievement.PUSH_1K.name)))
        assertTrue(service.check().isEmpty())
        assertEquals(1, env.db.achievementDao().all().count { it.id == Achievement.PUSH_1K.name })
    }

    @Test
    fun firstDayAndRecipe() = runTest {
        env.seedPlayer()
        val id = env.missions.createDaily(env.today, DailyTargets(1, 1, 1, 1.0))
        for (e in DailyExercise.entries) env.missions.addDailyProgress(id, e, 1.0)
        env.foods.createRecipe("Batido", 1.0, emptyList())
        val unlocked = service.check().filterIsInstance<SystemEvent.AchievementUnlocked>().map { it.id }.toSet()
        assertTrue(Achievement.FIRST_DAY.name in unlocked)
        assertTrue(Achievement.FIRST_RECIPE.name in unlocked)
    }

    @Test
    fun missionCompletionHookRuns() = runTest {
        env.seedPlayer()
        env.missions.afterCompletion = { service.check() }
        val id = env.missions.createDaily(env.today, DailyTargets(1, 1, 1, 1.0))
        var events = emptyList<SystemEvent>()
        for (e in DailyExercise.entries) events = events + env.missions.addDailyProgress(id, e, 1.0)
        assertTrue(events.contains(SystemEvent.AchievementUnlocked(Achievement.FIRST_DAY.name)))
    }
}
