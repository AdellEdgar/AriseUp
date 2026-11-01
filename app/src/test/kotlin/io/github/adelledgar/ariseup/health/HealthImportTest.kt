package io.github.adelledgar.ariseup.health

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.TestEnv
import io.github.adelledgar.ariseup.data.db.AppJson
import io.github.adelledgar.ariseup.data.db.DailyProgress
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class HealthImportTest {
    private val env = TestEnv()

    private class FakeSource(val runs: List<HealthRun>) : RunSource {
        override suspend fun available() = true
        override suspend fun runsBetween(start: Instant, end: Instant) = runs.filter { it.start >= start && it.start < end }
    }

    @Test
    fun importsRunsOnceIntoDailyAndPenalty() = runTest {
        env.seedPlayer()
        val daily = env.missions.createDaily(env.today, DailyTargets(10, 10, 10, 5.0))
        val penalty = env.missions.createPenalty(env.today, DailyTargets(15, 15, 15, 7.5))
        val run = HealthRun("rec-1", env.clock.instant().minusSeconds(3600), meters = 3200.0, minutes = 18.0)
        val importer = HealthImporter(env.db, env.missions, env.gameClock, FakeSource(listOf(run, run)))
        importer.import(env.today)
        importer.import(env.today)
        fun km(id: Long) = AppJson.decodeFromString(DailyProgress.serializer(), kotlinx.coroutines.runBlocking { env.db.missionDao().get(id) }!!.progress!!).runKm
        assertEquals(3.2, km(daily), 1e-9)
        assertEquals(3.2, km(penalty), 1e-9)
        assertEquals(1, env.db.healthImportDao().all().size)
    }

    @Test
    fun runsOutsideTheDayIgnored() = runTest {
        env.seedPlayer()
        val daily = env.missions.createDaily(env.today, DailyTargets(10, 10, 10, 5.0))
        val old = HealthRun("rec-old", env.clock.instant().minusSeconds(3 * 86_400), 5000.0, 30.0)
        HealthImporter(env.db, env.missions, env.gameClock, FakeSource(listOf(old))).import(env.today)
        assertEquals(0.0, AppJson.decodeFromString(DailyProgress.serializer(), env.db.missionDao().get(daily)!!.progress!!).runKm)
    }
}
