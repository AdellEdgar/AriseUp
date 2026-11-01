package io.github.adelledgar.ariseup.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.TestEnv
import io.github.adelledgar.ariseup.data.db.BodyEntryEntity
import io.github.adelledgar.ariseup.data.db.DailyProgress
import io.github.adelledgar.ariseup.data.db.AppJson
import io.github.adelledgar.ariseup.data.repo.ReevaluationService
import io.github.adelledgar.ariseup.domain.model.DailyExercise
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.FitnessTest
import io.github.adelledgar.ariseup.domain.model.MissionKind
import io.github.adelledgar.ariseup.domain.model.Stat
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import io.github.adelledgar.ariseup.ui.missions.MissionsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class MissionsViewModelTest {
    private val env = TestEnv()
    private val reeval = ReevaluationService(env.db, env.players, env.body, env.foods, env.missions, env.gameClock)
    private val vm = MissionsViewModel(env.db, env.missions, env.routines, env.body, reeval, env.gameClock)

    @Test
    fun partialPushUpsDoNotComplete() = runTest {
        env.seedPlayer()
        val id = env.missions.createDaily(env.today, DailyTargets(20, 10, 10, 1.0))
        vm.addDailySync(id, DailyExercise.PUSH_UPS, 10.0)
        vm.addDailySync(id, DailyExercise.PUSH_UPS, 10.0)
        val m = env.db.missionDao().get(id)!!
        assertNull(m.completedAt)
        assertEquals(20, AppJson.decodeFromString(DailyProgress.serializer(), m.progress!!).pushUps)
    }

    @Test
    fun allFourCompleteOnce() = runTest {
        env.seedPlayer()
        val id = env.missions.createDaily(env.today, DailyTargets(10, 10, 10, 1.0))
        val events = mutableListOf<SystemEvent>()
        for (e in DailyExercise.entries) events += vm.addDailySync(id, e, 10.0)
        events += vm.addDailySync(id, DailyExercise.PUSH_UPS, 5.0)
        assertEquals(1, events.count { it is SystemEvent.XpGained })
        assertNotNull(env.db.missionDao().get(id)!!.completedAt)
    }

    @Test
    fun weighInCompletesMission() = runTest {
        env.seedPlayer()
        val id = env.missions.createSimple(env.today, MissionKind.WEIGH_IN, "Registro de condición física", mandatory = false)
        vm.weighInSync(id, 81.5, null, null)
        assertEquals(81.5, env.body.on(env.today)!!.weightKg)
        assertNotNull(env.db.missionDao().get(id)!!.completedAt)
    }

    @Test
    fun reevaluationAppliesDeltasAndRaisesBaseline() = runTest {
        env.seedPlayer(baseline = DailyTargets(10, 15, 20, 1.0))
        env.body.saveTest(FitnessTest(env.today.minusDays(28), 20, 30, 40, 2000, 10.0, 80.0, null, null), isInitial = true)
        env.body.add(BodyEntryEntity(date = env.today.minusDays(28), weightKg = 80.0))
        val id = env.missions.createSimple(env.today, MissionKind.REEVALUATION, "Reevaluación del Jugador", mandatory = false)
        val events = vm.reevaluateSync(id, FitnessTest(env.today, 30, 30, 40, 2000, 10.0, 80.0, null, null))
        val s = env.players.current()!!
        assertEquals(DailyTargets(15, 15, 20, 1.0), s.baseline)
        // flexiones +50 %, sentadillas +0 % → media 25 % → FUE +5
        assertEquals(15.0, s.progress.stats[Stat.FUE], 1e-9)
        assertTrue(events.any { it is SystemEvent.StatsChanged })
        assertNotNull(env.db.missionDao().get(id)!!.completedAt)
        assertEquals(env.today, env.body.latestTest()!!.date)
    }
}
