package io.github.adelledgar.ariseup

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.data.backup.BackupManager
import io.github.adelledgar.ariseup.data.backup.ImportResult
import io.github.adelledgar.ariseup.data.db.AppJson
import io.github.adelledgar.ariseup.data.db.BodyEntryEntity
import io.github.adelledgar.ariseup.data.db.DailyProgress
import io.github.adelledgar.ariseup.data.gameDays
import io.github.adelledgar.ariseup.domain.model.DailyExercise
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.MissionKind
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import io.github.adelledgar.ariseup.health.HealthImporter
import io.github.adelledgar.ariseup.health.HealthRun
import io.github.adelledgar.ariseup.health.RunSource
import io.github.adelledgar.ariseup.system.DayCloseCoordinator
import io.github.adelledgar.ariseup.system.MissionPlanner
import io.github.adelledgar.ariseup.ui.settings.resetChangeKeepsDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests de los fallos encontrados en la revisión final. */
@RunWith(AndroidJUnit4::class)
class ReviewFixesTest {
    private val env = TestEnv(start = "2026-11-02T22:00")

    /** Hace que el generador cree siempre una urgente (a las 10:00, 3 h). */
    private object AlwaysUrgent : Random() {
        override fun nextBits(bitCount: Int) = 0
    }

    private fun planner(rng: Random = Random(0)) = MissionPlanner(env.db, env.players, env.missions, env.foods, env.settings, env.gameClock, rng)

    @Test
    fun noUrgentWithPastDeadline() = runTest {
        env.seedPlayer()
        planner(AlwaysUrgent).ensureDay(env.today)
        val urgent = env.db.missionDao().forDateAndKind(env.today, MissionKind.URGENT)
        assertTrue(urgent.all { it.deadline!! > env.clock.instant() && it.appearAt!! >= env.clock.instant().minusSeconds(1) })
        assertTrue(urgent.isEmpty(), "a las 22:00 quedan menos horas que la duración")
    }

    @Test
    fun urgentStillGeneratedEarlyInTheDay() = runTest {
        env.clock.set("2026-11-03T08:00")
        env.seedPlayer()
        planner(AlwaysUrgent).ensureDay(env.today)
        assertEquals(1, env.db.missionDao().forDateAndKind(env.today, MissionKind.URGENT).size)
    }

    @Test
    fun creationDayHasNoMandatoryMissions() = runTest {
        env.seedPlayer(createdOn = env.today)
        env.body.add(BodyEntryEntity(date = env.today, weightKg = 80.0))
        val p = planner()
        val coordinator = DayCloseCoordinator(env.db, env.players, env.foods, env.settings, env.gameClock, p)
        coordinator.catchUp()
        assertTrue(env.db.missionDao().forDate(env.today).none { it.mandatory })
        env.clock.set("2026-11-03T13:00")
        coordinator.catchUp()
        assertEquals(null, env.players.current()!!.penalty)
    }

    @Test
    fun restStartedAfterFailedNutritionPreventsPenalty() = runTest {
        env.clock.set("2026-11-02T10:00")
        env.seedPlayer()
        env.body.add(BodyEntryEntity(date = env.today, weightKg = 80.0))
        val p = planner()
        val coordinator = DayCloseCoordinator(env.db, env.players, env.foods, env.settings, env.gameClock, p)
        val d = env.today
        coordinator.catchUp()
        env.db.missionDao().forDateAndKind(d, MissionKind.DAILY).single().let { m -> for (e in DailyExercise.entries) env.missions.addDailyProgress(m.id, e, 10.0) }
        env.clock.set("2026-11-03T08:00")
        coordinator.catchUp()
        io.github.adelledgar.ariseup.data.repo.RestService(env.db, env.events, env.gameClock)
            .start(io.github.adelledgar.ariseup.domain.model.RestReason.ILLNESS, env.today, null)
        env.clock.set("2026-11-06T13:00")
        val events = coordinator.catchUp()
        assertEquals(null, env.players.current()!!.penalty)
        assertTrue(events.none { it is SystemEvent.PenaltyDegraded })
    }

    @Test
    fun resetChangeOnlyIfDayUnchanged() {
        val zone = ZoneId.of("Europe/Madrid")
        val at3 = LocalDateTime.parse("2026-11-03T03:00").atZone(zone).toInstant()
        assertFalse(resetChangeKeepsDay(at3, zone, LocalTime.of(6, 0), LocalTime.MIDNIGHT))
        val at12 = LocalDateTime.parse("2026-11-03T12:00").atZone(zone).toInstant()
        assertTrue(resetChangeKeepsDay(at12, zone, LocalTime.of(6, 0), LocalTime.MIDNIGHT))
        // Con reinicio a las 22:00, las 21:00 del día 3 pertenecen al día 2: cambiaría el día en curso.
        val at21 = LocalDateTime.parse("2026-11-03T21:00").atZone(zone).toInstant()
        assertFalse(resetChangeKeepsDay(at21, zone, LocalTime.MIDNIGHT, LocalTime.of(22, 0)))
        val at23 = LocalDateTime.parse("2026-11-03T23:00").atZone(zone).toInstant()
        assertTrue(resetChangeKeepsDay(at23, zone, LocalTime.MIDNIGHT, LocalTime.of(22, 0)))
    }

    @Test
    fun gameDaysTickerFollowsTheClock() = runTest {
        val seen = mutableListOf<java.time.LocalDate>()
        val job = launch { gameDays(env.gameClock, periodMs = 1_000).take(2).toList(seen) }
        advanceTimeBy(1_500)
        env.clock.set("2026-11-03T00:00:30")
        advanceTimeBy(1_500)
        job.join()
        assertEquals(listOf(java.time.LocalDate.of(2026, 11, 2), java.time.LocalDate.of(2026, 11, 3)), seen)
    }

    @Test
    fun oldBackupDoesNotPunishTheGap() = runTest {
        env.clock.set("2026-11-02T10:00")
        env.seedPlayer(level = 10)
        env.body.add(BodyEntryEntity(date = env.today, weightKg = 80.0))
        val p = planner()
        val coordinator = DayCloseCoordinator(env.db, env.players, env.foods, env.settings, env.gameClock, p)
        coordinator.catchUp()
        val out = ByteArrayOutputStream()
        BackupManager(env.db, env.settings, env.gameClock).export(out)
        env.clock.set("2026-12-02T10:00")
        assertEquals(ImportResult.Ok, BackupManager(env.db, env.settings, env.gameClock).import(out.toByteArray().inputStream()))
        val events = coordinator.catchUp()
        assertTrue(events.count { it is SystemEvent.PenaltyDegraded } <= 1, "eventos: $events")
        assertTrue(env.players.current()!!.progress.level >= 10)
    }

    @Test
    fun concurrentLogsAreNotLost() = runTest {
        env.seedPlayer()
        val id = env.missions.createDaily(env.today, DailyTargets(100, 10, 10, 1.0))
        withContext(Dispatchers.Default) {
            (1..20).map { async { env.missions.addDailyProgress(id, DailyExercise.PUSH_UPS, 1.0) } }.awaitAll()
        }
        val p = AppJson.decodeFromString(DailyProgress.serializer(), runBlocking { env.db.missionDao().get(id) }!!.progress!!)
        assertEquals(20, p.pushUps)
    }

    @Test
    fun foregroundImportCoversToday() = runTest {
        env.clock.set("2026-11-02T10:00")
        env.seedPlayer()
        val daily = env.missions.createDaily(env.today, DailyTargets(10, 10, 10, 5.0))
        val run = HealthRun("r", env.clock.instant().minusSeconds(600), 2000.0, 12.0)
        val source = object : RunSource {
            override suspend fun available() = true
            override suspend fun runsBetween(start: Instant, end: Instant) = listOf(run).filter { it.start >= start && it.start < end }
        }
        env.db.dayDao().upsert(io.github.adelledgar.ariseup.data.db.DayEntity(env.today, io.github.adelledgar.ariseup.data.db.DayStatus.OPEN, false, null))
        HealthImporter(env.db, env.missions, env.gameClock, source).importOpenDays()
        assertEquals(2.0, AppJson.decodeFromString(DailyProgress.serializer(), env.db.missionDao().get(daily)!!.progress!!).runKm, 1e-9)
    }
}
