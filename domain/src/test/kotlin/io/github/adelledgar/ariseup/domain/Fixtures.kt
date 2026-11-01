package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.PlayerProgress
import io.github.adelledgar.ariseup.domain.model.StatBlock

fun progress(level: Int = 1, xp: Long = 0, freePoints: Int = 0, streak: Int = 0, stats: StatBlock = StatBlock.initial()) =
    PlayerProgress(level = level, xp = xp, freePoints = freePoints, stats = stats, streak = streak)
