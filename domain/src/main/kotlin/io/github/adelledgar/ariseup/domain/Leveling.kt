package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.PlayerProgress
import io.github.adelledgar.ariseup.domain.model.Rank
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import kotlin.math.pow
import kotlin.math.roundToLong

object Leveling {
    fun xpToNext(level: Int): Long = (Balance.XP_BASE * level.toDouble().pow(Balance.XP_EXP)).roundToLong()

    fun dayPool(level: Int): Long = (Balance.POOL_BASE * level.toDouble().pow(Balance.POOL_EXP)).roundToLong()

    fun rankOf(level: Int): Rank = when {
        level >= Balance.RANK_S -> Rank.S
        level >= Balance.RANK_A -> Rank.A
        level >= Balance.RANK_B -> Rank.B
        level >= Balance.RANK_C -> Rank.C
        level >= Balance.RANK_D -> Rank.D
        else -> Rank.E
    }

    /** Suma o resta XP. Al ganar encadena subidas de nivel; al perder, la XP no baja de 0 ni se pierde nivel. */
    fun applyXp(p: PlayerProgress, delta: Long, source: String = ""): Pair<PlayerProgress, List<SystemEvent>> {
        if (delta == 0L) return p to emptyList()
        if (delta < 0) {
            val lost = minOf(p.xp, -delta)
            if (lost == 0L) return p to emptyList()
            return p.copy(xp = p.xp - lost) to listOf(SystemEvent.XpLost(lost, source))
        }
        val events = mutableListOf<SystemEvent>(SystemEvent.XpGained(delta, source))
        var level = p.level
        var xp = p.xp + delta
        var points = p.freePoints
        val startRank = rankOf(level)
        while (xp >= xpToNext(level)) {
            xp -= xpToNext(level)
            level++
            points += Balance.FREE_POINTS_PER_LEVEL
            events += SystemEvent.LevelUp(level)
        }
        val endRank = rankOf(level)
        if (endRank != startRank) events += SystemEvent.RankChanged(startRank, endRank)
        return p.copy(level = level, xp = xp, freePoints = points) to events
    }

    /** Baja un nivel (mínimo 1). La XP queda a 0; los puntos libres se conservan. */
    fun levelDown(p: PlayerProgress): Pair<PlayerProgress, List<SystemEvent>> {
        if (p.level <= 1) return p to emptyList()
        val newLevel = p.level - 1
        val events = mutableListOf<SystemEvent>(SystemEvent.LevelDown(newLevel))
        val from = rankOf(p.level)
        val to = rankOf(newLevel)
        if (from != to) events += SystemEvent.RankChanged(from, to)
        return p.copy(level = newLevel, xp = 0) to events
    }
}
