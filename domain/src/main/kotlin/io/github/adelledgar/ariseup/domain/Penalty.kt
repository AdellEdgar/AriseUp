package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.PenaltyState
import io.github.adelledgar.ariseup.domain.model.PlayerProgress
import io.github.adelledgar.ariseup.domain.model.Stat
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import java.time.LocalDate
import kotlin.math.roundToLong

/** Zona de Penalización (spec §2.6). */
object Penalty {
    fun start(today: LocalDate, firstDegradeOnCloseOf: LocalDate, current: DailyTargets): PenaltyState =
        PenaltyState(today, firstDegradeOnCloseOf, current.scaled(Balance.PENALTY_FACTOR), degradeCount = 0)

    fun degradeOnClose(
        p: PlayerProgress,
        s: PenaltyState,
        closedDay: LocalDate,
    ): Triple<PlayerProgress, PenaltyState, List<SystemEvent>> {
        if (closedDay.isBefore(s.firstDegradeOnCloseOf)) return Triple(p, s, emptyList())
        val count = s.degradeCount + 1
        val events = mutableListOf<SystemEvent>(SystemEvent.PenaltyDegraded(count))
        val loss = (Leveling.xpToNext(p.level) * Balance.PENALTY_XP_LOSS).roundToLong()
        val (afterXp, xpEvents) = Leveling.applyXp(p, -loss, "Zona de Penalización")
        events += xpEvents
        var progress = afterXp.copy(stats = Stats.degrade(afterXp.stats))
        events += SystemEvent.StatsChanged(Stat.entries.associateWith { -1.0 }, "Zona de Penalización")
        if (count >= Balance.PENALTY_LEVEL_LOSS_FROM_CLOSE) {
            val (down, downEvents) = Leveling.levelDown(progress)
            progress = down
            events += downEvents
        }
        return Triple(progress, s.copy(degradeCount = count), events)
    }

    fun clear(s: PenaltyState, today: LocalDate): SystemEvent.PenaltyCleared = SystemEvent.PenaltyCleared(onFirstDay = today == s.startedOn)
}
