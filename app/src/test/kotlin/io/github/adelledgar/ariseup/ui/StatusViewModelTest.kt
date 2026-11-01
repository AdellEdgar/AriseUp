package io.github.adelledgar.ariseup.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.TestEnv
import io.github.adelledgar.ariseup.data.db.BodyEntryEntity
import io.github.adelledgar.ariseup.data.db.PenaltyEntity
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.Rank
import io.github.adelledgar.ariseup.ui.status.StatusViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class StatusViewModelTest {
    private val env = TestEnv()

    private fun vm() = StatusViewModel(env.db, env.players, env.body, env.gameClock)

    @Test
    fun exposesProgressAndShape() = runTest {
        env.seedPlayer(level = 12, xp = 100)
        env.body.add(BodyEntryEntity(date = env.today.minusDays(10), weightKg = 90.0))
        env.body.add(BodyEntryEntity(date = env.today, weightKg = 85.0))
        val s = vm().uiFlow.first { it != null }!!
        assertEquals(12, s.level)
        assertEquals(Rank.D, s.rank)
        assertEquals(85.0, s.weightKg)
        assertEquals(-5.0, s.weightDelta!!, 1e-9)
        assertTrue(s.ghost != null && s.ghost.fat > s.shape.fat)
        assertEquals(false, s.penalty)
    }

    @Test
    fun penaltyFlag() = runTest {
        env.seedPlayer()
        env.db.penaltyDao().upsert(PenaltyEntity(startedOn = env.today, firstDegradeOnCloseOf = env.today, targets = DailyTargets(15, 15, 15, 1.5), degradeCount = 0))
        assertTrue(vm().uiFlow.first { it != null }!!.penalty)
    }
}
