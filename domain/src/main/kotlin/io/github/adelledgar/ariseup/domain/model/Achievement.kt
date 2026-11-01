package io.github.adelledgar.ariseup.domain.model

/** Misiones ocultas: solo se revelan al conseguirlas (spec §2.5). */
enum class Achievement(val title: String?, val label: String) {
    FIRST_DAY(null, "Completar tu primer día"),
    STREAK_7(null, "7 días de racha"),
    STREAK_30("Disciplina de hierro", "30 días de racha"),
    STREAK_100(null, "100 días de racha"),
    STREAK_365("El que nunca falla", "365 días de racha"),
    PENALTY_SURVIVOR("El que superó la penalización", "Superar una penalización"),
    PENALTY_SAME_DAY(null, "Superar una penalización en su primer día"),
    RANK_D(null, "Alcanzar el rango D"),
    RANK_C(null, "Alcanzar el rango C"),
    RANK_B(null, "Alcanzar el rango B"),
    RANK_A(null, "Alcanzar el rango A"),
    RANK_S("Cazador de rango S", "Alcanzar el rango S"),
    PUSH_1K(null, "1.000 flexiones acumuladas"),
    PUSH_10K(null, "10.000 flexiones acumuladas"),
    RUN_100("Corredor incansable", "100 km corridos"),
    NUTRITION_30(null, "30 días con la nutrición cumplida"),
    FIRST_RECIPE(null, "Crear tu primera receta"),
    ALL_STATS_UP(null, "Mejorar todas las stats en una reevaluación"),
    URGENT_FAST(null, "Completar una misión urgente en menos de una hora"),
}

data class AchievementCounters(
    val firstDayCompleted: Boolean,
    val maxStreak: Int,
    val penaltiesCleared: Int,
    val penaltyClearedOnFirstDay: Boolean,
    val ranksReached: Set<Rank>,
    val totalPushUps: Int,
    val totalRunKm: Double,
    val nutritionDaysMet: Int,
    val recipesCreated: Int,
    val allStatsImprovedInReeval: Boolean,
    val urgentUnderOneHour: Boolean,
)
