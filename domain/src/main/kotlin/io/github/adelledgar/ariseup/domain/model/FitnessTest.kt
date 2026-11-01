package io.github.adelledgar.ariseup.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import kotlin.math.ceil

/** Test de evaluación (inicial o reevaluación). Todos los campos son opcionales: se puede rellenar por partes. */
data class FitnessTest(
    val date: LocalDate,
    val pushUps: Int?,
    val sitUps: Int?,
    val squats: Int?,
    val cooperMeters: Int?,
    val sitAndReachCm: Double?,
    val weightKg: Double?,
    val waistCm: Double?,
    val bodyFatPct: Double?,
) {
    val exerciseComplete: Boolean
        get() = pushUps != null && sitUps != null && squats != null && cooperMeters != null
}

@Serializable
data class DailyTargets(val pushUps: Int, val sitUps: Int, val squats: Int, val runKm: Double) {
    /** Multiplica los objetivos: repeticiones hacia arriba y km al siguiente múltiplo de 0,5. */
    fun scaled(f: Double): DailyTargets = DailyTargets(
        pushUps = ceil(pushUps * f - 1e-9).toInt(),
        sitUps = ceil(sitUps * f - 1e-9).toInt(),
        squats = ceil(squats * f - 1e-9).toInt(),
        runKm = ceil(runKm * f * 2 - 1e-9) / 2,
    )
}
