package io.github.adelledgar.ariseup.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.adelledgar.ariseup.data.GameClock
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.DayStatus
import io.github.adelledgar.ariseup.data.db.FitnessTestEntity
import io.github.adelledgar.ariseup.data.repo.BodyRepository
import io.github.adelledgar.ariseup.data.repo.EventRepository
import io.github.adelledgar.ariseup.domain.model.Achievement
import io.github.adelledgar.ariseup.domain.model.Stat
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

enum class DayMark { DONE, FAILED, PENALTY, REST, TODAY, OPEN }

data class CalendarDay(val date: LocalDate, val mark: DayMark)

data class LogEntry(val at: Instant, val text: String)

data class UnlockedAchievement(val achievement: Achievement, val at: Instant)

data class ProgressUi(
    val calendar: List<CalendarDay>,
    val weight: List<Pair<LocalDate, Double>>,
    val weightAvg: List<Double>,
    val stats: Map<Stat, List<Pair<LocalDate, Double>>>,
    val tests: List<FitnessTestEntity>,
    val log: List<LogEntry>,
    val achievements: List<UnlockedAchievement>,
    val hiddenLeft: Int,
)

/** Texto legible de un evento del Sistema. */
fun describe(e: SystemEvent): String = when (e) {
    is SystemEvent.XpGained -> "[+${e.amount} XP] ${e.source}".trimEnd()
    is SystemEvent.XpLost -> "[−${e.amount} XP] ${e.reason}".trimEnd()
    is SystemEvent.LevelUp -> "[Ha subido de nivel: ${e.newLevel}]"
    is SystemEvent.LevelDown -> "[Has bajado al nivel ${e.newLevel}]"
    is SystemEvent.RankChanged -> "[Nuevo rango: ${e.to}]"
    is SystemEvent.StatsChanged -> "[Stats] " + e.delta.filterValues { it != 0.0 }.entries.joinToString(", ") { (s, v) ->
        "${s.name} ${if (v > 0) "+" else ""}${"%.2f".format(Locale("es"), v).trimEnd('0').trimEnd(',')}"
    } + if (e.reason.isNotBlank()) " · ${e.reason}" else ""
    is SystemEvent.PenaltyStarted -> "[Has entrado en la Zona de Penalización]"
    is SystemEvent.PenaltyDegraded -> "[Degradación nº ${e.closeNumber}]"
    is SystemEvent.PenaltyCleared -> "[Has superado la penalización]"
    is SystemEvent.StreakChanged -> if (e.value == 0) "[Racha perdida]" else "[Racha: ${e.value} días]"
    is SystemEvent.UrgentFailed -> "[Misión urgente fallida]"
    is SystemEvent.AchievementUnlocked -> "[Misión oculta: ${Achievement.entries.firstOrNull { it.name == e.id }?.label ?: e.id}]"
    is SystemEvent.RestStarted -> "[Estado de descanso: ${e.reason}]"
    SystemEvent.RestEnded -> "[Fin del Estado de descanso]"
}

/** Media móvil de los últimos [window] valores (incluido el actual). */
fun movingAverage(values: List<Double>, window: Int): List<Double> = values.indices.map { i ->
    val from = maxOf(0, i - window + 1)
    values.subList(from, i + 1).average()
}

class ProgressViewModel(
    private val db: AriseUpDatabase,
    private val body: BodyRepository,
    private val gameClock: GameClock,
) : ViewModel() {
    val uiFlow: Flow<ProgressUi> = combine(
        db.dayDao().observeAll(),
        body.observeAll(),
        body.observeTests(),
        db.eventDao().observeRecent(Int.MAX_VALUE),
        db.achievementDao().observeAll(),
    ) { days, entries, tests, eventRows, achievements ->
        val today = gameClock.today()
        val zone: ZoneId = gameClock.clock.zone
        val decoded = eventRows.sortedBy { it.id }.mapNotNull { row -> EventRepository.decode(row)?.let { row to it } }
        val failedDays = decoded.filter { it.second is SystemEvent.PenaltyStarted }.mapNotNull { it.first.day }.toSet()
        val penaltyDays = decoded.filter { it.second is SystemEvent.PenaltyDegraded }.mapNotNull { it.first.day }.toSet()
        val calendar = days.map { d ->
            CalendarDay(
                d.date,
                when {
                    d.date == today -> DayMark.TODAY
                    d.date in failedDays -> DayMark.FAILED
                    d.date in penaltyDays -> DayMark.PENALTY
                    d.restActive -> DayMark.REST
                    d.status == DayStatus.OPEN -> DayMark.OPEN
                    else -> DayMark.DONE
                },
            )
        }
        val weights = entries.filter { it.weightKg != null }.map { it.date to it.weightKg!! }

        var current = Stat.entries.associateWith { 10.0 }
        val statPoints = Stat.entries.associateWith { mutableListOf<Pair<LocalDate, Double>>() }
        decoded.forEach { (row, e) ->
            if (e is SystemEvent.StatsChanged) {
                current = current.mapValues { (s, v) -> maxOf(1.0, v + (e.delta[s] ?: 0.0)) }
                val date = row.day ?: row.at.atZone(zone).toLocalDate()
                Stat.entries.forEach { s -> statPoints.getValue(s) += date to current.getValue(s) }
            }
        }
        val unlocked = achievements.mapNotNull { a -> Achievement.entries.firstOrNull { it.name == a.id }?.let { UnlockedAchievement(it, a.unlockedAt) } }
            .sortedByDescending { it.at }
        ProgressUi(
            calendar = calendar,
            weight = weights,
            weightAvg = movingAverage(weights.map { it.second }, 7),
            stats = statPoints,
            tests = tests,
            log = decoded.reversed().take(200).map { LogEntry(it.first.at, describe(it.second)) },
            achievements = unlocked,
            hiddenLeft = Achievement.entries.size - unlocked.size,
        )
    }

    val ui: StateFlow<ProgressUi?> = uiFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

}
