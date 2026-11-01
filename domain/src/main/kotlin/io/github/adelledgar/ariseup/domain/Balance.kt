package io.github.adelledgar.ariseup.domain

/** Todas las constantes de equilibrio del juego (spec §2). */
object Balance {
    // Nivel y XP
    const val XP_BASE = 100.0
    const val XP_EXP = 1.5
    const val POOL_BASE = 100.0
    const val POOL_EXP = 0.9
    const val FREE_POINTS_PER_LEVEL = 3

    // Reparto del día perfecto
    const val SHARE_DAILY = 0.45
    const val SHARE_ROUTINES = 0.20
    const val SHARE_NUTRITION = 0.25
    const val SHARE_RULES = 0.10
    const val SHARE_WEIGH_IN = 0.03
    const val SHARE_URGENT = 0.50

    // Rangos: nivel mínimo de cada uno
    const val RANK_D = 10
    const val RANK_C = 25
    const val RANK_B = 45
    const val RANK_A = 70
    const val RANK_S = 100

    // Stats
    const val STAT_INITIAL = 10.0
    const val STAT_MIN = 1.0

    // Misión diaria
    const val BASELINE_FRACTION = 0.5
    const val MIN_REPS = 5
    const val MIN_RUN_KM = 1.0
    const val GOAL_REPS = 100
    const val GOAL_KM = 10.0
    const val GOAL_LEVEL = 45
    const val POST_GOAL_REPS_PER_2LVL = 1
    const val POST_GOAL_KM_PER_2LVL = 0.1
    const val REST_RETURN_FACTOR = 0.7

    // Penalización
    const val PENALTY_FACTOR = 1.5
    const val PENALTY_XP_LOSS = 0.20
    const val PENALTY_LEVEL_LOSS_FROM_CLOSE = 3

    // Urgentes
    const val URGENT_PROBABILITY = 0.2
    const val URGENT_MAX_PER_WEEK = 2
    const val URGENT_FAIL_XP_LOSS = 0.10

    // Descanso
    const val VACATION_DAYS_PER_YEAR = 30
    const val ILLNESS_CONFIRM_EVERY_DAYS = 7
    const val ILLNESS_CONFIRM_GRACE_HOURS = 24
    const val REST_RETURN_DAYS = 7
    const val LONG_REST_DAYS = 21

    // Nutrición
    const val NUTRITION_KCAL_TOLERANCE = 0.10
    const val NUTRITION_PROTEIN_MIN = 0.90
    const val NUTRITION_MIN_MEALS = 3
    const val NUTRITION_MARGIN_HOURS = 12L

    // Reevaluación
    const val REEVALUATION_EVERY_DAYS = 28
}
