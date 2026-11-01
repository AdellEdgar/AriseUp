package io.github.adelledgar.ariseup.data.repo

import io.github.adelledgar.ariseup.data.db.AchievementEntity
import io.github.adelledgar.ariseup.data.db.AppJson
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.DailyProgress
import io.github.adelledgar.ariseup.data.db.UrgentProgress
import io.github.adelledgar.ariseup.data.db.UrgentTarget
import io.github.adelledgar.ariseup.domain.Achievements
import io.github.adelledgar.ariseup.domain.Leveling
import io.github.adelledgar.ariseup.domain.model.Achievement
import io.github.adelledgar.ariseup.domain.model.AchievementCounters
import io.github.adelledgar.ariseup.domain.model.DailyExercise
import io.github.adelledgar.ariseup.domain.model.MissionKind
import io.github.adelledgar.ariseup.domain.model.Rank
import io.github.adelledgar.ariseup.domain.model.Stat
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import java.time.Duration
import java.time.Instant

/** Misiones ocultas: calcula los contadores desde la BD y desbloquea las nuevas. */
class AchievementService(private val db: AriseUpDatabase, private val events: EventRepository) {
    suspend fun counters(): AchievementCounters {
        val missions = db.missionDao().all()
        val log = db.eventDao().all().mapNotNull { e -> EventRepository.decode(e) }
        val player = db.playerDao().get()

        val physical = missions.filter { it.kind == MissionKind.DAILY || it.kind == MissionKind.PENALTY }
            .mapNotNull { m -> m.progress?.let { AppJson.decodeFromString(DailyProgress.serializer(), it) } }
        val urgent = missions.filter { it.kind == MissionKind.URGENT }.mapNotNull { m ->
            val t = m.target?.let { AppJson.decodeFromString(UrgentTarget.serializer(), it) } ?: return@mapNotNull null
            val p = m.progress?.let { AppJson.decodeFromString(UrgentProgress.serializer(), it) } ?: return@mapNotNull null
            Triple(m, t, p)
        }
        val cleared = log.filterIsInstance<SystemEvent.PenaltyCleared>()
        val ranks = log.filterIsInstance<SystemEvent.RankChanged>().map { it.to }.toSet() + Rank.E +
            listOfNotNull(player?.level?.let(Leveling::rankOf))

        return AchievementCounters(
            firstDayCompleted = missions.any { it.kind == MissionKind.DAILY && it.completedAt != null },
            maxStreak = maxOf(player?.streak ?: 0, log.filterIsInstance<SystemEvent.StreakChanged>().maxOfOrNull { it.value } ?: 0),
            penaltiesCleared = cleared.size,
            penaltyClearedOnFirstDay = cleared.any { it.onFirstDay },
            ranksReached = ranks,
            totalPushUps = physical.sumOf { it.pushUps } +
                urgent.filter { it.second.exercise == DailyExercise.PUSH_UPS }.sumOf { it.third.done.toInt() },
            totalRunKm = physical.sumOf { it.runKm } + urgent.filter { it.second.exercise == DailyExercise.RUN }.sumOf { it.third.done },
            nutritionDaysMet = missions.count { it.kind == MissionKind.NUTRITION && it.completedAt != null },
            recipesCreated = db.foodDao().recipeCount(),
            allStatsImprovedInReeval = log.filterIsInstance<SystemEvent.StatsChanged>().any { e ->
                e.reason == ReevaluationService.REASON && Stat.entries.all { (e.delta[it] ?: 0.0) > 0 }
            },
            urgentUnderOneHour = urgent.any { (m, _, _) ->
                m.completedAt != null && m.appearAt != null && Duration.between(m.appearAt, m.completedAt) < Duration.ofHours(1)
            },
        )
    }

    /** Desbloquea las misiones ocultas nuevas y devuelve sus eventos. */
    suspend fun check(): List<SystemEvent> {
        val already = db.achievementDao().all().mapNotNull { a -> Achievement.entries.firstOrNull { it.name == a.id } }.toSet()
        val fresh = Achievements.newlyUnlocked(counters(), already)
        if (fresh.isEmpty()) return emptyList()
        val now = Instant.now()
        fresh.forEach { db.achievementDao().insert(AchievementEntity(it.name, now)) }
        val out = fresh.map { SystemEvent.AchievementUnlocked(it.name) }
        events.append(out)
        return out
    }
}
