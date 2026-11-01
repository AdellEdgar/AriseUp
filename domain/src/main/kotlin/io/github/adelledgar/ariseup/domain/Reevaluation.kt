package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.FitnessTest
import io.github.adelledgar.ariseup.domain.model.Goal
import io.github.adelledgar.ariseup.domain.model.Stat
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.truncate

/** Bonus o pérdida de stats al comparar dos tests (spec §2.2). */
object Reevaluation {
    private const val CAP = 5.0

    fun statDeltas(prev: FitnessTest, now: FitnessTest, goal: Goal, loggingCompletePct: Double): Map<Stat, Double> {
        val fue = listOfNotNull(pct(prev.pushUps, now.pushUps), pct(prev.squats, now.squats))
            .takeIf { it.isNotEmpty() }?.average()
        val agi = pct(prev.cooperMeters, now.cooperMeters)
        val sen = if (prev.sitAndReachCm != null && now.sitAndReachCm != null) {
            (now.sitAndReachCm - prev.sitAndReachCm) / maxOf(abs(prev.sitAndReachCm), 10.0) * 100
        } else {
            null
        }
        return mapOf(
            Stat.FUE to fromPct(fue),
            Stat.AGI to fromPct(agi),
            Stat.SEN to fromPct(sen),
            Stat.VIT to vitality(prev, now, goal),
            Stat.INT to intelligence(loggingCompletePct),
        )
    }

    private fun pct(prev: Int?, now: Int?): Double? =
        if (prev == null || now == null || prev <= 0) null else (now - prev).toDouble() / prev * 100

    private fun fromPct(p: Double?): Double = if (p == null) 0.0 else (p / 5).roundToInt().toDouble().coerceIn(-CAP, CAP)

    private fun vitality(prev: FitnessTest, now: FitnessTest, goal: Goal): Double {
        var points = 0.0
        if (prev.weightKg != null && now.weightKg != null && prev.weightKg > 0) {
            val change = (now.weightKg - prev.weightKg) / prev.weightKg * 100
            points += when (goal) {
                Goal.LOSE_FAT -> steps(-change / 1.0)
                Goal.GAIN_MUSCLE -> {
                    val months = maxOf(1.0, ChronoUnit.DAYS.between(prev.date, now.date) / 28.0)
                    steps(minOf(change, 2.0 * months) / 0.5)
                }
                Goal.MAINTAIN -> when {
                    abs(change) <= 1.5 -> 2.0
                    abs(change) <= 3.0 -> 0.0
                    else -> -2.0
                }
            }
        }
        if (prev.bodyFatPct != null && now.bodyFatPct != null) {
            points += steps(prev.bodyFatPct - now.bodyFatPct)
        }
        return points.coerceIn(-CAP, CAP) + 0.0
    }

    /** Pasos completos, tolerando el error de coma flotante (0,9999999 cuenta como 1). */
    private fun steps(x: Double): Double = truncate(x + sign(x) * 1e-9)

    private fun intelligence(p: Double): Double = when {
        p > 80 -> ((p - 80) / 4).roundToInt().toDouble()
        p < 50 -> -((50 - p) / 10).roundToInt().toDouble()
        else -> 0.0
    }.coerceIn(-CAP, CAP)
}
