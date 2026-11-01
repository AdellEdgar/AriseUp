package io.github.adelledgar.ariseup.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Lo que el Sistema registra en el historial. */
@Serializable
sealed interface SystemEvent {
    @Serializable @SerialName("xp_gained")
    data class XpGained(val amount: Long, val source: String) : SystemEvent

    @Serializable @SerialName("xp_lost")
    data class XpLost(val amount: Long, val reason: String) : SystemEvent

    @Serializable @SerialName("level_up")
    data class LevelUp(val newLevel: Int) : SystemEvent

    @Serializable @SerialName("level_down")
    data class LevelDown(val newLevel: Int) : SystemEvent

    @Serializable @SerialName("rank_changed")
    data class RankChanged(val from: Rank, val to: Rank) : SystemEvent

    @Serializable @SerialName("stats_changed")
    data class StatsChanged(val delta: Map<Stat, Double>, val reason: String) : SystemEvent

    @Serializable @SerialName("penalty_started")
    data class PenaltyStarted(val day: String) : SystemEvent

    @Serializable @SerialName("penalty_degraded")
    data class PenaltyDegraded(val closeNumber: Int) : SystemEvent

    @Serializable @SerialName("penalty_cleared")
    data class PenaltyCleared(val onFirstDay: Boolean) : SystemEvent

    @Serializable @SerialName("streak_changed")
    data class StreakChanged(val value: Int) : SystemEvent

    @Serializable @SerialName("urgent_failed")
    data class UrgentFailed(val id: Long) : SystemEvent

    @Serializable @SerialName("achievement_unlocked")
    data class AchievementUnlocked(val id: String) : SystemEvent

    @Serializable @SerialName("rest_started")
    data class RestStarted(val reason: String) : SystemEvent

    @Serializable @SerialName("rest_ended")
    data object RestEnded : SystemEvent
}
