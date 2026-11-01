package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.PlayerProgress
import io.github.adelledgar.ariseup.domain.model.StatBlock

fun progress(level: Int = 1, xp: Long = 0, freePoints: Int = 0, streak: Int = 0, stats: StatBlock = StatBlock.initial()) =
    PlayerProgress(level = level, xp = xp, freePoints = freePoints, stats = stats, streak = streak)

fun test(
    push: Int? = null,
    sit: Int? = null,
    squat: Int? = null,
    cooper: Int? = null,
    reach: Double? = null,
    weight: Double? = null,
    fat: Double? = null,
    date: java.time.LocalDate = java.time.LocalDate.of(2026, 1, 1),
) = io.github.adelledgar.ariseup.domain.model.FitnessTest(
    date = date, pushUps = push, sitUps = sit, squats = squat, cooperMeters = cooper,
    sitAndReachCm = reach, weightKg = weight, waistCm = null, bodyFatPct = fat,
)
