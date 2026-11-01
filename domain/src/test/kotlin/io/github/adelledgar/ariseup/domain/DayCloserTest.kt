package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.ExerciseDayInput
import io.github.adelledgar.ariseup.domain.model.MissionKind
import io.github.adelledgar.ariseup.domain.model.MissionOutcome
import io.github.adelledgar.ariseup.domain.model.NutritionDayInput
import io.github.adelledgar.ariseup.domain.model.PlayerState
import io.github.adelledgar.ariseup.domain.model.RuleOutcome
import io.github.adelledgar.ariseup.domain.model.Stat
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DayCloserTest {
    private val d = LocalDate.of(2026, 11, 2)
    private val targets = DailyTargets(10, 10, 10, 1.0)
    private val fresh = PlayerState(progress(level = 10, xp = 0), penalty = null, baseline = targets)

    private fun PlayerState.withStreak(n: Int) = copy(progress = progress.copy(streak = n))

    private fun daily(completed: Boolean) = MissionOutcome(1, MissionKind.DAILY, mandatory = true, completed = completed)
    private fun routine(mandatory: Boolean, completed: Boolean) = MissionOutcome(2, MissionKind.ROUTINE, mandatory, completed)
    private fun urgent(completed: Boolean) = MissionOutcome(3, MissionKind.URGENT, mandatory = false, completed = completed)

    private fun input(day: LocalDate = d, outcomes: List<MissionOutcome>, restActive: Boolean = false) =
        ExerciseDayInput(day, outcomes, restActive, targets)

    private fun nutInput(
        day: LocalDate = d,
        evaluatedDuring: LocalDate = d.plusDays(1),
        nutritionMandatory: Boolean? = true,
        nutritionMet: Boolean = true,
        rules: List<RuleOutcome> = emptyList(),
        meals: Int = 3,
        restActive: Boolean = false,
    ) = NutritionDayInput(day, evaluatedDuring, nutritionMandatory, nutritionMet, rules, meals, restActive)

    @Test
    fun failedDailyStartsPenalty() {
        val r = DayCloser.closeExercise(fresh.withStreak(4), input(outcomes = listOf(daily(false))))
        val p = assertNotNull(r.state.penalty)
        assertEquals(d.plusDays(1), p.startedOn)
        assertEquals(d.plusDays(1), p.firstDegradeOnCloseOf)
        assertEquals(DailyTargets(15, 15, 15, 1.5), p.targets)
        assertEquals(0, r.state.progress.streak)
        assertTrue(r.events.contains(SystemEvent.PenaltyStarted(d.plusDays(1).toString())))
        assertTrue(r.events.contains(SystemEvent.StreakChanged(0)))
    }

    @Test
    fun completedDayIncrementsStreak() {
        val r = DayCloser.closeExercise(fresh.withStreak(2), input(outcomes = listOf(daily(true), routine(true, true))))
        assertEquals(3, r.state.progress.streak)
        assertNull(r.state.penalty)
        assertEquals(0, r.state.progress.xp) // las misiones de ejercicio se recompensan al completarse
    }

    @Test
    fun optionalRoutineFailDoesNotPenalize() {
        assertNull(DayCloser.closeExercise(fresh, input(outcomes = listOf(daily(true), routine(mandatory = false, completed = false)))).state.penalty)
    }

    @Test
    fun mandatoryRoutineFailPenalizes() {
        assertNotNull(DayCloser.closeExercise(fresh, input(outcomes = listOf(daily(true), routine(mandatory = true, completed = false)))).state.penalty)
    }

    @Test
    fun restSkipsPenaltyAndFreezesStreak() {
        val r = DayCloser.closeExercise(fresh.withStreak(5), input(outcomes = listOf(daily(false)), restActive = true))
        assertNull(r.state.penalty)
        assertEquals(5, r.state.progress.streak)
    }

    @Test
    fun streakWeekBonus() {
        val r = DayCloser.closeExercise(fresh.withStreak(6), input(outcomes = listOf(daily(true))))
        assertEquals(7, r.state.progress.streak)
        assertEquals(10.5, r.state.progress.stats[Stat.VIT], 1e-9)
    }

    @Test
    fun noStreakWhileInPenalty() {
        val inPenalty = DayCloser.closeExercise(fresh, input(outcomes = listOf(daily(false)))).state
        val r = DayCloser.closeExercise(inPenalty, input(day = d.plusDays(1), outcomes = listOf(daily(true))))
        assertEquals(0, r.state.progress.streak)
    }

    @Test
    fun fiveDaysIgnored() {
        var s = DayCloser.closeExercise(fresh, input(outcomes = listOf(daily(false)))).state
        val all = mutableListOf<SystemEvent>()
        for (i in 1..5L) {
            val r = DayCloser.closeExercise(s, input(day = d.plusDays(i), outcomes = listOf(daily(false))))
            s = r.state
            all += r.events
        }
        assertEquals(7, s.progress.level)
        assertEquals(5, all.count { it is SystemEvent.PenaltyDegraded })
        assertEquals(3, all.count { it is SystemEvent.LevelDown })
        assertEquals(0, all.count { it is SystemEvent.PenaltyStarted })
        assertEquals(5.0, s.progress.stats[Stat.FUE], 1e-9)
    }

    @Test
    fun urgentFailLosesXpNoPenalty() {
        val s = PlayerState(progress(level = 1, xp = 50), null, targets)
        val r = DayCloser.closeExercise(s, input(outcomes = listOf(daily(true), urgent(false))))
        assertEquals(40, r.state.progress.xp)
        assertNull(r.state.penalty)
        assertTrue(r.events.contains(SystemEvent.UrgentFailed(3)))
    }

    @Test
    fun urgentIgnoredInRest() {
        val s = PlayerState(progress(level = 1, xp = 50), null, targets)
        assertEquals(50, DayCloser.closeExercise(s, input(outcomes = listOf(urgent(false)), restActive = true)).state.progress.xp)
    }

    @Test
    fun nutritionPenaltyHasGraceUntilNextClose() {
        val r = DayCloser.closeNutrition(fresh, nutInput(nutritionMet = false))
        val p = assertNotNull(r.state.penalty)
        assertEquals(d.plusDays(1), p.startedOn)
        assertEquals(d.plusDays(2), p.firstDegradeOnCloseOf)
    }

    @Test
    fun nutritionFailWithExistingPenaltyDoesNotRestart() {
        val inPenalty = DayCloser.closeExercise(fresh, input(outcomes = listOf(daily(false)))).state
        val r = DayCloser.closeNutrition(inPenalty, nutInput(nutritionMet = false))
        assertSame(inPenalty.penalty, r.state.penalty)
        assertTrue(r.events.none { it is SystemEvent.PenaltyStarted })
    }

    @Test
    fun disabledNutritionNeverPenalizes() {
        assertNull(DayCloser.closeNutrition(fresh, nutInput(nutritionMandatory = null, nutritionMet = false)).state.penalty)
    }

    @Test
    fun optionalNutritionDoesNotPenalize() {
        assertNull(DayCloser.closeNutrition(fresh, nutInput(nutritionMandatory = false, nutritionMet = false)).state.penalty)
    }

    @Test
    fun mandatoryRuleFailPenalizes() {
        val r = DayCloser.closeNutrition(fresh, nutInput(rules = listOf(RuleOutcome(mandatory = true, met = false))))
        assertNotNull(r.state.penalty)
    }

    @Test
    fun nutritionRewards() {
        val s = PlayerState(progress(level = 1, xp = 0), null, targets)
        val r = DayCloser.closeNutrition(s, nutInput(rules = listOf(RuleOutcome(false, true), RuleOutcome(false, false))))
        // nutrición 25 + una regla de dos (100·0,10/2 = 5)
        assertEquals(30, r.state.progress.xp)
        assertEquals(10.0 + 0.1 + 0.05 + 0.1, r.state.progress.stats[Stat.INT], 1e-9)
        assertEquals(10.3, r.state.progress.stats[Stat.VIT], 1e-9)
    }

    @Test
    fun restDayDoesNotDegradeExistingPenalty() {
        val inPenalty = DayCloser.closeExercise(fresh, input(outcomes = listOf(daily(false)))).state
        val r = DayCloser.closeExercise(inPenalty, input(day = d.plusDays(1), outcomes = listOf(daily(false)), restActive = true))
        assertTrue(r.events.none { it is SystemEvent.PenaltyDegraded })
        assertEquals(inPenalty.progress, r.state.progress)
    }

    @Test
    fun nutritionEvaluatedDuringRestNoPenalty() {
        val r = DayCloser.closeNutrition(fresh, nutInput(nutritionMet = false).copy(evaluatedDuringRest = true))
        assertNull(r.state.penalty)
    }

    @Test
    fun nutritionInRestNeverPenalizes() {
        assertNull(DayCloser.closeNutrition(fresh, nutInput(nutritionMet = false, restActive = true)).state.penalty)
    }
}
