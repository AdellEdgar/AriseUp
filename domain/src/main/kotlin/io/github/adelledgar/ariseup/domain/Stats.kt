package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.PlayerProgress
import io.github.adelledgar.ariseup.domain.model.Stat
import io.github.adelledgar.ariseup.domain.model.StatBlock

object Stats {
    val MEALS_LOGGED_GAIN: Map<Stat, Double> = mapOf(Stat.INT to 0.1)
    val STREAK_WEEK_GAIN: Map<Stat, Double> = mapOf(Stat.VIT to 0.5)

    fun applyGains(s: StatBlock, g: Map<Stat, Double>): StatBlock = s.plus(g)

    /** −1 en cada stat (mínimo 1). */
    fun degrade(s: StatBlock): StatBlock = s.plus(Stat.entries.associateWith { -1.0 })

    fun spendFreePoint(p: PlayerProgress, stat: Stat): PlayerProgress {
        check(p.freePoints > 0) { "No quedan puntos libres" }
        return p.copy(freePoints = p.freePoints - 1, stats = p.stats.plus(mapOf(stat to 1.0)))
    }
}
