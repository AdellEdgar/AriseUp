package io.github.adelledgar.ariseup.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.TestEnv
import io.github.adelledgar.ariseup.domain.Penalty
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.Stat
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class PlayerRepositoryTest {
    private val env = TestEnv()

    @Test
    fun noPlayerBeforeOnboarding() = runTest {
        assertNull(env.players.state().first())
    }

    @Test
    fun saveStateRoundTripsPenaltyAndEvents() = runTest {
        env.seedPlayer(level = 5)
        val s = env.players.current()!!
        val penalty = Penalty.start(env.today, env.today, DailyTargets(10, 10, 10, 1.0))
        env.players.saveState(s.copy(progress = s.progress.copy(xp = 77), penalty = penalty), listOf(SystemEvent.PenaltyStarted(env.today.toString())))
        val back = env.players.current()!!
        assertEquals(77, back.progress.xp)
        assertEquals(penalty, back.penalty)
        assertEquals(1, env.db.eventDao().count())
        env.players.saveState(back.copy(penalty = null), emptyList())
        assertNull(env.players.current()!!.penalty)
    }

    @Test
    fun spendFreePoint() = runTest {
        env.seedPlayer()
        val s = env.players.current()!!
        env.players.saveState(s.copy(progress = s.progress.copy(freePoints = 1)), emptyList())
        env.players.spendFreePoint(Stat.AGI)
        val back = env.players.current()!!
        assertEquals(0, back.progress.freePoints)
        assertEquals(11.0, back.progress.stats[Stat.AGI], 1e-9)
    }
}
