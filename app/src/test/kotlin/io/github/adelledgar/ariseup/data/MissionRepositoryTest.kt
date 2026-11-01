package io.github.adelledgar.ariseup.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.TestEnv
import io.github.adelledgar.ariseup.data.db.DayEntity
import io.github.adelledgar.ariseup.data.db.DayStatus
import io.github.adelledgar.ariseup.data.db.PenaltyEntity
import io.github.adelledgar.ariseup.domain.model.DailyExercise
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class MissionRepositoryTest {
    private val env = TestEnv()
    private val targets = DailyTargets(10, 10, 10, 1.0)

    @Test
    fun completeTwiceRewardsOnce() = runTest {
        env.seedPlayer()
        val id = env.missions.createDaily(env.today, targets)
        val first = env.missions.complete(id)
        val second = env.missions.complete(id)
        assertTrue(first.any { it is SystemEvent.XpGained })
        assertTrue(second.isEmpty())
        assertEquals(45, env.players.state().first()!!.progress.xp)
    }

    @Test
    fun dailyProgressAutoCompletes() = runTest {
        env.seedPlayer()
        val id = env.missions.createDaily(env.today, targets)
        env.missions.addDailyProgress(id, DailyExercise.PUSH_UPS, 10.0)
        env.missions.addDailyProgress(id, DailyExercise.SIT_UPS, 10.0)
        env.missions.addDailyProgress(id, DailyExercise.SQUATS, 5.0)
        env.missions.addDailyProgress(id, DailyExercise.SQUATS, 5.0)
        assertNull(env.db.missionDao().get(id)!!.completedAt)
        val events = env.missions.addDailyProgress(id, DailyExercise.RUN, 1.0)
        assertTrue(events.any { it is SystemEvent.XpGained })
        assertTrue(env.db.missionDao().get(id)!!.completedAt != null)
    }

    @Test
    fun completingPenaltyClearsIt() = runTest {
        env.seedPlayer()
        env.db.penaltyDao().upsert(PenaltyEntity(startedOn = env.today, firstDegradeOnCloseOf = env.today, targets = targets.scaled(1.5), degradeCount = 0))
        val id = env.missions.createPenalty(env.today, targets.scaled(1.5))
        val events = env.missions.complete(id)
        assertTrue(events.contains(SystemEvent.PenaltyCleared(true)))
        assertNull(env.players.state().first()!!.penalty)
    }

    @Test
    fun closedDayRejectsProgress() = runTest {
        env.seedPlayer()
        val yesterday = env.today.minusDays(1)
        val id = env.missions.createDaily(yesterday, targets)
        env.db.dayDao().upsert(DayEntity(yesterday, DayStatus.EXERCISE_CLOSED, restActive = false, targets = targets))
        val e = assertFailsWith<IllegalStateException> { env.missions.addDailyProgress(id, DailyExercise.PUSH_UPS, 1.0) }
        assertEquals("[Este día ya está cerrado]", e.message)
    }
}
