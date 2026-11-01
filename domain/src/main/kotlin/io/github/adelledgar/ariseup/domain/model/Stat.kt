package io.github.adelledgar.ariseup.domain.model

import io.github.adelledgar.ariseup.domain.Balance
import kotlinx.serialization.Serializable
import kotlin.math.floor

@Serializable
enum class Stat(val label: String) {
    FUE("Fuerza"),
    AGI("Agilidad"),
    VIT("Vitalidad"),
    SEN("Sentido"),
    INT("Inteligencia"),
}

/** Valores de las 5 stats. Ninguno baja de [Balance.STAT_MIN]. */
@Serializable
data class StatBlock(val values: Map<Stat, Double>) {
    operator fun get(s: Stat): Double = values[s] ?: Balance.STAT_INITIAL

    fun plus(gains: Map<Stat, Double>): StatBlock =
        StatBlock(Stat.entries.associateWith { maxOf(Balance.STAT_MIN, this[it] + (gains[it] ?: 0.0)) })

    fun displayed(s: Stat): Int = floor(this[s]).toInt()

    companion object {
        fun initial() = StatBlock(Stat.entries.associateWith { Balance.STAT_INITIAL })
    }
}
