package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.PenaltyState
import io.github.adelledgar.ariseup.domain.model.Stat
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PenaltyTest {
    private val d = LocalDate.of(2026, 11, 2)
    private val state = Penalty.start(d, d, DailyTargets(10, 10, 10, 1.0))

    @Test
    fun penaltyTargets150() {
        assertEquals(DailyTargets(15, 15, 15, 1.5), state.targets)
        assertEquals(0, state.degradeCount)
    }

    @Test
    fun firstTwoClosesNoLevelLoss() {
        val (p1, s1, ev1) = Penalty.degradeOnClose(progress(level = 10, xp = 1000), state, d)
        assertEquals(368, p1.xp)
        assertEquals(10, p1.level)
        assertEquals(9.0, p1.stats[Stat.FUE], 1e-9)
        assertTrue(ev1.contains(SystemEvent.PenaltyDegraded(1)))
        val (p2, s2, _) = Penalty.degradeOnClose(p1, s1, d.plusDays(1))
        assertEquals(0, p2.xp)
        assertEquals(10, p2.level)
        assertEquals(2, s2.degradeCount)
    }

    @Test
    fun thirdCloseLevelsDown() {
        var p = progress(level = 10, xp = 1000)
        var s: PenaltyState = state
        var events = emptyList<SystemEvent>()
        repeat(3) { i ->
            val r = Penalty.degradeOnClose(p, s, d.plusDays(i.toLong()))
            p = r.first
            s = r.second
            events = r.third
        }
        assertEquals(9, p.level)
        assertEquals(0, p.xp)
        assertTrue(events.contains(SystemEvent.LevelDown(9)))
        assertTrue(events.contains(SystemEvent.PenaltyDegraded(3)))
    }

    @Test
    fun graceDayNotDegraded() {
        val s = Penalty.start(d, d.plusDays(1), DailyTargets(10, 10, 10, 1.0))
        val (p, s2, ev) = Penalty.degradeOnClose(progress(level = 10, xp = 1000), s, d)
        assertEquals(1000, p.xp)
        assertEquals(s, s2)
        assertTrue(ev.isEmpty())
    }

    @Test
    fun clearOnFirstDay() {
        assertEquals(SystemEvent.PenaltyCleared(true), Penalty.clear(state, d))
        assertEquals(SystemEvent.PenaltyCleared(false), Penalty.clear(state, d.plusDays(1)))
    }
}
