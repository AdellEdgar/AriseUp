package io.github.adelledgar.ariseup.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayerProgress(
    val level: Int,
    val xp: Long,
    val freePoints: Int,
    val stats: StatBlock,
    val streak: Int,
)
