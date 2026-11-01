package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.Goal
import io.github.adelledgar.ariseup.domain.model.Stat
import kotlin.test.Test
import kotlin.test.assertEquals

class ReevaluationTest {
    private val t0 = test()
    private val t1 = test(date = t0.date.plusDays(28))

    private fun deltas(prev: io.github.adelledgar.ariseup.domain.model.FitnessTest, now: io.github.adelledgar.ariseup.domain.model.FitnessTest, goal: Goal = Goal.MAINTAIN, logging: Double = 60.0) =
        Reevaluation.statDeltas(prev, now.copy(date = prev.date.plusDays(28)), goal, logging)

    @Test
    fun fueFromPushAndSquats() {
        assertEquals(4.0, deltas(test(push = 20, squat = 40), test(push = 24, squat = 48))[Stat.FUE])
    }

    @Test
    fun capsAtFive() {
        assertEquals(5.0, deltas(test(cooper = 1000), test(cooper = 2000))[Stat.AGI])
        assertEquals(-5.0, deltas(test(cooper = 2000), test(cooper = 1000))[Stat.AGI])
    }

    @Test
    fun senFromReach() {
        assertEquals(2.0, deltas(test(reach = 20.0), test(reach = 22.0))[Stat.SEN])
    }

    @Test
    fun senWithSmallBase() {
        // Con base cercana a 0, el porcentaje se calcula sobre 10 cm.
        assertEquals(2.0, deltas(test(reach = 0.0), test(reach = 1.0))[Stat.SEN])
    }

    @Test
    fun missingMetricsGiveZero() {
        val d = deltas(t0, t1)
        assertEquals(0.0, d[Stat.FUE])
        assertEquals(0.0, d[Stat.AGI])
        assertEquals(0.0, d[Stat.SEN])
        assertEquals(0.0, d[Stat.VIT])
    }

    @Test
    fun vitLoseFat() {
        assertEquals(3.0, deltas(test(weight = 100.0), test(weight = 97.0), Goal.LOSE_FAT)[Stat.VIT])
        assertEquals(-2.0, deltas(test(weight = 100.0), test(weight = 102.0), Goal.LOSE_FAT)[Stat.VIT])
    }

    @Test
    fun vitGainMuscleCappedAt2PctPerMonth() {
        assertEquals(2.0, deltas(test(weight = 80.0), test(weight = 80.8), Goal.GAIN_MUSCLE)[Stat.VIT])
        assertEquals(4.0, deltas(test(weight = 80.0), test(weight = 84.0), Goal.GAIN_MUSCLE)[Stat.VIT])
        assertEquals(-2.0, deltas(test(weight = 80.0), test(weight = 79.2), Goal.GAIN_MUSCLE)[Stat.VIT])
    }

    @Test
    fun vitMaintain() {
        assertEquals(2.0, deltas(test(weight = 80.0), test(weight = 81.0))[Stat.VIT])
        assertEquals(0.0, deltas(test(weight = 80.0), test(weight = 82.0))[Stat.VIT])
        assertEquals(-2.0, deltas(test(weight = 80.0), test(weight = 83.0))[Stat.VIT])
    }

    @Test
    fun vitBodyFatAddsAndCaps() {
        assertEquals(5.0, deltas(test(weight = 100.0, fat = 30.0), test(weight = 96.0, fat = 27.0), Goal.LOSE_FAT)[Stat.VIT])
    }

    @Test
    fun intFromLogging() {
        assertEquals(5.0, deltas(t0, t1, logging = 100.0)[Stat.INT])
        assertEquals(-3.0, deltas(t0, t1, logging = 20.0)[Stat.INT])
        assertEquals(0.0, deltas(t0, t1, logging = 65.0)[Stat.INT])
    }
}
