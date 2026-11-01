package io.github.adelledgar.ariseup.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.TestEnv
import io.github.adelledgar.ariseup.data.db.BodyEntryEntity
import io.github.adelledgar.ariseup.data.db.DayEntity
import io.github.adelledgar.ariseup.data.db.DayStatus
import io.github.adelledgar.ariseup.domain.model.Rank
import io.github.adelledgar.ariseup.domain.model.Stat
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import io.github.adelledgar.ariseup.ui.progress.DayMark
import io.github.adelledgar.ariseup.ui.progress.ProgressViewModel
import io.github.adelledgar.ariseup.ui.progress.describe
import io.github.adelledgar.ariseup.ui.progress.movingAverage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ProgressViewModelTest {
    private val env = TestEnv()

    @Test
    fun eventTexts() {
        assertEquals("[+45 XP] Misión diaria", describe(SystemEvent.XpGained(45, "Misión diaria")))
        assertEquals("[Has bajado al nivel 9]", describe(SystemEvent.LevelDown(9)))
        assertEquals("[Ha subido de nivel: 10]", describe(SystemEvent.LevelUp(10)))
        assertEquals("[Nuevo rango: D]", describe(SystemEvent.RankChanged(Rank.E, Rank.D)))
        assertEquals("[Has entrado en la Zona de Penalización]", describe(SystemEvent.PenaltyStarted("2026-11-03")))
    }

    @Test
    fun calendarMarksFailedDays() = runTest {
        env.seedPlayer()
        val d = env.today.minusDays(2)
        val ok = env.today.minusDays(1)
        env.db.dayDao().upsert(DayEntity(d, DayStatus.CLOSED, restActive = false, targets = null))
        env.db.dayDao().upsert(DayEntity(ok, DayStatus.CLOSED, restActive = false, targets = null))
        env.db.dayDao().upsert(DayEntity(env.today, DayStatus.OPEN, restActive = false, targets = null))
        env.events.append(listOf(SystemEvent.PenaltyStarted(ok.toString())), d)
        val vm = ProgressViewModel(env.db, env.body, env.gameClock)
        val marks = vm.uiFlow.first().calendar.associate { it.date to it.mark }
        assertEquals(DayMark.FAILED, marks[d])
        assertEquals(DayMark.DONE, marks[ok])
        assertEquals(DayMark.TODAY, marks[env.today])
    }

    @Test
    fun statsHistoryAccumulates() = runTest {
        env.seedPlayer()
        env.events.append(listOf(SystemEvent.StatsChanged(mapOf(Stat.FUE to 0.3), "a")), env.today.minusDays(1))
        env.events.append(listOf(SystemEvent.StatsChanged(mapOf(Stat.FUE to 0.3), "b")), env.today)
        env.body.add(BodyEntryEntity(date = env.today, weightKg = 80.0))
        val ui = ProgressViewModel(env.db, env.body, env.gameClock).uiFlow.first()
        assertEquals(listOf(10.3, 10.6), ui.stats.getValue(Stat.FUE).map { Math.round(it.second * 10) / 10.0 })
        assertEquals(80.0, ui.weight.single().second)
    }

    @Test
    fun movingAverage7() {
        assertEquals(listOf(1.0, 1.5, 2.0), movingAverage(listOf(1.0, 2.0, 3.0), 7))
        assertEquals(6.0, movingAverage((1..9).map { it.toDouble() }, 7).last(), 1e-9)
    }
}
