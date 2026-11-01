package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.Stat
import io.github.adelledgar.ariseup.domain.model.StatBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StatsTest {
    @Test
    fun applyGains() {
        assertEquals(10.3, Stats.applyGains(StatBlock.initial(), mapOf(Stat.FUE to 0.3))[Stat.FUE], 1e-9)
    }

    @Test
    fun degradeSubtractsOne() {
        val s = Stats.degrade(StatBlock.initial())
        Stat.entries.forEach { assertEquals(9.0, s[it], 1e-9) }
    }

    @Test
    fun degradeFloorsAtOne() {
        assertEquals(1.0, Stats.degrade(StatBlock.initial().plus(mapOf(Stat.INT to -8.5)))[Stat.INT], 1e-9)
    }

    @Test
    fun spendFreePoint() {
        val p = Stats.spendFreePoint(progress(freePoints = 2), Stat.AGI)
        assertEquals(1, p.freePoints)
        assertEquals(11.0, p.stats[Stat.AGI], 1e-9)
    }

    @Test
    fun spendWithoutPointsFails() {
        assertFailsWith<IllegalStateException> { Stats.spendFreePoint(progress(freePoints = 0), Stat.AGI) }
    }

    @Test
    fun displayedFloors() {
        assertEquals(10, StatBlock.initial().plus(mapOf(Stat.VIT to 0.99)).displayed(Stat.VIT))
    }
}
