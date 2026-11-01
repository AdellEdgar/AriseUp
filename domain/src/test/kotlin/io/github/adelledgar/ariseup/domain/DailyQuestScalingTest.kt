package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DailyQuestScalingTest {
    private val base = DailyTargets(10, 15, 20, 1.0)

    @Test
    fun baselineIsHalfOfMax() {
        assertEquals(base, DailyQuestScaling.baseline(test(push = 20, sit = 30, squat = 40, cooper = 2000)))
    }

    @Test
    fun baselineMinimums() {
        assertEquals(DailyTargets(5, 5, 5, 1.0), DailyQuestScaling.baseline(test(push = 3, sit = 4, squat = 6, cooper = 900)))
    }

    @Test
    fun baselineRunRoundsToHalfKm() {
        assertEquals(1.5, DailyQuestScaling.baseline(test(push = 20, sit = 20, squat = 20, cooper = 2600)).runKm)
    }

    @Test
    fun reachesGoalAtLevel45() {
        assertEquals(DailyTargets(100, 100, 100, 10.0), DailyQuestScaling.targets(base, 45, false))
    }

    @Test
    fun levelOneIsBaseline() {
        assertEquals(base, DailyQuestScaling.targets(base, 1, false))
    }

    @Test
    fun postGoalGrowth() {
        val t = DailyQuestScaling.targets(base, 55, false)
        assertEquals(105, t.pushUps)
        assertEquals(10.5, t.runKm, 1e-9)
    }

    @Test
    fun strongStartKeepsBaselineAsGoal() {
        assertEquals(120, DailyQuestScaling.targets(DailyTargets(120, 15, 20, 1.0), 45, false).pushUps)
    }

    @Test
    fun returningFromRestIs70Percent() {
        assertEquals(70, DailyQuestScaling.targets(DailyTargets(100, 100, 100, 10.0), 45, true).pushUps)
    }

    @Test
    fun reevaluationNeverLowersBaseline() {
        val old = DailyTargets(20, 20, 20, 2.0)
        assertEquals(old, DailyQuestScaling.raiseBaseline(old, test(push = 10, sit = 10, squat = 10, cooper = 1000)))
    }

    @Test
    fun reevaluationRaisesImprovedExercises() {
        val old = DailyTargets(20, 20, 20, 2.0)
        assertEquals(DailyTargets(30, 20, 20, 2.0), DailyQuestScaling.raiseBaseline(old, test(push = 60, sit = 10, squat = 10, cooper = 1000)))
    }

    @Test
    fun scaledRoundsUp() {
        assertEquals(DailyTargets(15, 15, 15, 1.5), DailyTargets(10, 10, 10, 1.0).scaled(1.5))
        assertEquals(DailyTargets(16, 16, 16, 2.0), DailyTargets(11, 11, 11, 1.1).scaled(1.4))
    }

    @Test
    fun targetsMonotonicInLevel() {
        runBlocking {
            checkAll(Arb.int(1..200)) { level ->
                val a = DailyQuestScaling.targets(base, level, false)
                val b = DailyQuestScaling.targets(base, level + 1, false)
                assertTrue(b.pushUps >= a.pushUps && b.sitUps >= a.sitUps && b.squats >= a.squats && b.runKm >= a.runKm)
            }
        }
    }
}
