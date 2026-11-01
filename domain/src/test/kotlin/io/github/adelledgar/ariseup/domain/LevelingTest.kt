package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.Rank
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LevelingTest {
    @Test
    fun xpCurve() {
        assertEquals(100, Leveling.xpToNext(1))
        assertEquals(283, Leveling.xpToNext(2))
        assertEquals(3162, Leveling.xpToNext(10))
    }

    @Test
    fun dayPool() {
        assertEquals(100, Leveling.dayPool(1))
        assertEquals(794, Leveling.dayPool(10))
    }

    @Test
    fun ranks() {
        assertEquals(Rank.E, Leveling.rankOf(9))
        assertEquals(Rank.D, Leveling.rankOf(10))
        assertEquals(Rank.B, Leveling.rankOf(45))
        assertEquals(Rank.S, Leveling.rankOf(100))
    }

    @Test
    fun levelUpCarriesOverAndGivesPoints() {
        val (p, ev) = Leveling.applyXp(progress(level = 9, xp = 2600), 600)
        assertEquals(10, p.level)
        assertEquals(500, p.xp)
        assertEquals(3, p.freePoints)
        assertTrue(ev.contains(SystemEvent.LevelUp(10)))
        assertTrue(ev.contains(SystemEvent.RankChanged(Rank.E, Rank.D)))
    }

    @Test
    fun multipleLevelUps() {
        val (p, ev) = Leveling.applyXp(progress(level = 1, xp = 0), 100 + 283 + 10)
        assertEquals(3, p.level)
        assertEquals(10, p.xp)
        assertEquals(6, p.freePoints)
        assertEquals(2, ev.count { it is SystemEvent.LevelUp })
    }

    @Test
    fun xpLossNeverBelowZeroNorLevelDown() {
        val (p, ev) = Leveling.applyXp(progress(level = 5, xp = 50), -500)
        assertEquals(5, p.level)
        assertEquals(0, p.xp)
        assertEquals(listOf<SystemEvent>(SystemEvent.XpLost(50, "")), ev)
    }

    @Test
    fun levelDownFloorsAtOne() {
        val (p, ev) = Leveling.levelDown(progress(level = 1, xp = 30))
        assertEquals(1, p.level)
        assertTrue(ev.isEmpty())
    }

    @Test
    fun levelDownResetsXpKeepsFreePoints() {
        val (p, ev) = Leveling.levelDown(progress(level = 10, xp = 900, freePoints = 2))
        assertEquals(9, p.level)
        assertEquals(0, p.xp)
        assertEquals(2, p.freePoints)
        assertTrue(ev.contains(SystemEvent.LevelDown(9)))
        assertTrue(ev.contains(SystemEvent.RankChanged(Rank.D, Rank.E)))
    }

    @Test
    fun xpAlwaysWithinLevelBounds() = runBlocking {
        checkAll(Arb.int(1..150), Arb.long(-1_000_000L..1_000_000L)) { level, delta ->
            val (p, _) = Leveling.applyXp(progress(level = level), delta)
            assertTrue(p.xp >= 0)
            assertTrue(p.xp < Leveling.xpToNext(p.level))
        }
    }
}
