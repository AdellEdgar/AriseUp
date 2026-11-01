package io.github.adelledgar.ariseup.system

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.TestEnv
import io.github.adelledgar.ariseup.data.db.BodyEntryEntity
import io.github.adelledgar.ariseup.data.db.DayStatus
import io.github.adelledgar.ariseup.data.db.RestPeriodEntity
import io.github.adelledgar.ariseup.data.prefs.WeighInFrequency
import io.github.adelledgar.ariseup.domain.model.DailyExercise
import io.github.adelledgar.ariseup.domain.model.MissionKind
import io.github.adelledgar.ariseup.domain.model.RestReason
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class DayCloseCoordinatorTest {
    private val env = TestEnv(start = "2026-11-02T10:00")
    private val d = LocalDate.of(2026, 11, 2)
    private val planner = MissionPlanner(env.db, env.players, env.missions, env.foods, env.settings, env.gameClock, Random(0))
    private val coordinator = DayCloseCoordinator(env.db, env.players, env.foods, env.settings, env.gameClock, planner)

    private suspend fun missionsOf(day: LocalDate, kind: MissionKind) = env.db.missionDao().forDateAndKind(day, kind)

    @Test
    fun catchUpFiveDaysAppliesEachOnce() = runTest {
        env.seedPlayer(level = 10)
        env.body.add(BodyEntryEntity(date = d, weightKg = 80.0))
        coordinator.catchUp()
        assertEquals(1, missionsOf(d, MissionKind.DAILY).size)
        env.clock.set("2026-11-08T13:00")
        val events = coordinator.catchUp()
        // d falla → penalización desde d+1; cierres d+1..d+5 degradan; d+3, d+4 y d+5 bajan nivel
        assertEquals(1, events.count { it is SystemEvent.PenaltyStarted })
        assertEquals(5, events.count { it is SystemEvent.PenaltyDegraded })
        assertEquals(7, env.players.current()!!.progress.level)
        assertTrue(coordinator.catchUp().isEmpty())
        assertEquals(7, env.players.current()!!.progress.level)
    }

    @Test
    fun nutritionWaitsForMargin() = runTest {
        env.seedPlayer()
        env.body.add(BodyEntryEntity(date = d, weightKg = 80.0))
        coordinator.catchUp()
        env.clock.set("2026-11-03T11:59")
        coordinator.catchUp()
        assertEquals(DayStatus.EXERCISE_CLOSED, env.db.dayDao().get(d)!!.status)
        env.clock.set("2026-11-03T12:00")
        coordinator.catchUp()
        assertEquals(DayStatus.CLOSED, env.db.dayDao().get(d)!!.status)
    }

    @Test
    fun nonMidnightReset() = runTest {
        env.settings.update { it.copy(resetMinutes = 4 * 60) }
        env.seedPlayer()
        env.clock.set("2026-11-03T02:00")
        coordinator.catchUp()
        val daily = missionsOf(d, MissionKind.DAILY).single()
        for (e in DailyExercise.entries) env.missions.addDailyProgress(daily.id, e, 10.0)
        assertNotNull(env.db.missionDao().get(daily.id)!!.completedAt)
        assertEquals(d, env.db.missionDao().get(daily.id)!!.date)
    }

    @Test
    fun clockGoingBackDoesNothing() = runTest {
        env.seedPlayer()
        coordinator.catchUp()
        env.clock.set("2026-11-05T10:00")
        coordinator.catchUp()
        val level = env.players.current()!!.progress
        env.clock.set("2026-11-03T10:00")
        assertTrue(coordinator.catchUp().isEmpty())
        assertEquals(level, env.players.current()!!.progress)
        assertNull(env.db.dayDao().get(LocalDate.of(2026, 11, 3)).takeIf { it?.status == DayStatus.OPEN })
    }

    @Test
    fun weighInEveryTwoDays() = runTest {
        env.settings.update { it.copy(weighInFrequency = WeighInFrequency.EVERY_2_DAYS) }
        env.seedPlayer()
        env.body.add(BodyEntryEntity(date = d, weightKg = 80.0))
        planner.ensureDay(d.plusDays(1))
        planner.ensureDay(d.plusDays(2))
        assertTrue(missionsOf(d.plusDays(1), MissionKind.WEIGH_IN).isEmpty())
        assertEquals(1, missionsOf(d.plusDays(2), MissionKind.WEIGH_IN).size)
    }

    @Test
    fun noWeighInIfAlreadyWeighedToday() = runTest {
        env.seedPlayer()
        env.body.add(BodyEntryEntity(date = d, weightKg = 80.0))
        planner.ensureDay(d)
        assertTrue(missionsOf(d, MissionKind.WEIGH_IN).isEmpty())
    }

    @Test
    fun restDayMissionsAreOptional() = runTest {
        env.seedPlayer()
        env.db.restDao().insert(RestPeriodEntity(reason = RestReason.VACATION, start = d, end = d.plusDays(2), lastConfirmed = d))
        planner.ensureDay(d)
        assertTrue(env.db.missionDao().forDate(d).none { it.mandatory })
        assertTrue(env.db.dayDao().get(d)!!.restActive)
    }

    @Test
    fun penaltyMissionCreatedOnceAndCarriesOver() = runTest {
        env.seedPlayer()
        env.body.add(BodyEntryEntity(date = d, weightKg = 80.0))
        coordinator.catchUp()
        env.clock.set("2026-11-03T10:00")
        coordinator.catchUp()
        env.clock.set("2026-11-04T10:00")
        coordinator.catchUp()
        assertEquals(1, env.db.missionDao().allOfKind(MissionKind.PENALTY).size)
    }

    @Test
    fun completedDayNoPenalty() = runTest {
        env.seedPlayer()
        env.body.add(BodyEntryEntity(date = d, weightKg = 80.0))
        env.settings.update { it.copy(nutritionMode = io.github.adelledgar.ariseup.data.prefs.NutritionMode.OFF) }
        coordinator.catchUp()
        val daily = missionsOf(d, MissionKind.DAILY).single()
        for (e in DailyExercise.entries) env.missions.addDailyProgress(daily.id, e, 10.0)
        env.clock.set("2026-11-03T13:00")
        coordinator.catchUp()
        assertNull(env.players.current()!!.penalty)
        assertEquals(1, env.players.current()!!.progress.streak)
        assertFalse(env.db.missionDao().forDate(LocalDate.of(2026, 11, 3)).isEmpty())
    }
}
