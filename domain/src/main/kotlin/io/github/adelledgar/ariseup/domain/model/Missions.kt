package io.github.adelledgar.ariseup.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MissionKind { DAILY, ROUTINE, NUTRITION, DIET_RULE, URGENT, PENALTY, REEVALUATION, WEIGH_IN }

@Serializable
enum class RoutineCategory(val stat: Stat, val label: String) {
    STRENGTH(Stat.FUE, "Fuerza"),
    CARDIO(Stat.AGI, "Cardio"),
    MOBILITY(Stat.SEN, "Movilidad"),
}

@Serializable
enum class DailyExercise(val label: String) {
    PUSH_UPS("Flexiones"),
    SIT_UPS("Abdominales"),
    SQUATS("Sentadillas"),
    RUN("Correr"),
}

data class Reward(val xp: Long, val stats: Map<Stat, Double>)
