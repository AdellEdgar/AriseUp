package io.github.adelledgar.ariseup.domain.model

import java.time.LocalDate

/** Estado completo del Jugador. [baseline] es null hasta completar el test inicial. */
data class PlayerState(
    val progress: PlayerProgress,
    val penalty: PenaltyState?,
    val baseline: DailyTargets?,
)

data class MissionOutcome(val id: Long, val kind: MissionKind, val mandatory: Boolean, val completed: Boolean)

data class RuleOutcome(val mandatory: Boolean, val met: Boolean)

data class ExerciseDayInput(
    val day: LocalDate,
    val outcomes: List<MissionOutcome>,
    val restActive: Boolean,
    val currentTargets: DailyTargets,
)

/** [nutritionMandatory] null = nutrición desactivada. [evaluatedDuring] es el día de juego en que se evalúa (D+1). */
data class NutritionDayInput(
    val day: LocalDate,
    val evaluatedDuring: LocalDate,
    val nutritionMandatory: Boolean?,
    val nutritionMet: Boolean,
    val rules: List<RuleOutcome>,
    val mealsWithEntries: Int,
    val restActive: Boolean,
    /** El día en que se evalúa (D+1) está en descanso: no se inicia penalización. */
    val evaluatedDuringRest: Boolean = false,
)

data class CloseResult(val state: PlayerState, val events: List<SystemEvent>)
