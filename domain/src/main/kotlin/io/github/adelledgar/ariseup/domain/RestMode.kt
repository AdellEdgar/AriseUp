package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.RestPeriod
import io.github.adelledgar.ariseup.domain.model.RestReason
import io.github.adelledgar.ariseup.domain.model.RestValidation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Estado de descanso (spec §2.9). */
object RestMode {
    fun validateStart(
        reason: RestReason,
        start: LocalDate,
        end: LocalDate?,
        today: LocalDate,
        inPenalty: Boolean,
        history: List<RestPeriod>,
    ): RestValidation {
        if (start.isBefore(today)) return RestValidation.Rejected("[No se puede iniciar un descanso en el pasado]")
        if (inPenalty) return RestValidation.Rejected("[No disponible durante la Zona de Penalización]")
        if (reason == RestReason.VACATION && end == null) return RestValidation.Rejected("[Las vacaciones necesitan fecha de fin]")
        if (end != null && end.isBefore(start)) return RestValidation.Rejected("[La fecha de fin es anterior al inicio]")
        if (history.any { overlaps(it, start, end) }) return RestValidation.Rejected("[Ya hay un descanso en esas fechas]")
        if (reason == RestReason.VACATION) {
            val requested = RestPeriod(0, reason, start, end, start, null)
            for (year in start.year..end!!.year) {
                if (vacationDaysUsed(year, history) + daysInYear(requested, year) > Balance.VACATION_DAYS_PER_YEAR) {
                    return RestValidation.Rejected("[Límite de 30 días de vacaciones por año alcanzado]")
                }
            }
        }
        return RestValidation.Ok
    }

    fun vacationDaysUsed(year: Int, history: List<RestPeriod>): Int =
        history.filter { it.reason == RestReason.VACATION }.sumOf { daysInYear(it, year) }

    fun needsConfirmation(p: RestPeriod, today: LocalDate): Boolean =
        p.reason != RestReason.VACATION && p.covers(today) &&
            ChronoUnit.DAYS.between(p.lastConfirmed, today) >= Balance.ILLNESS_CONFIRM_EVERY_DAYS

    fun expiredUnconfirmed(p: RestPeriod, now: LocalDateTime, promptAt: LocalDateTime?): Boolean =
        p.reason != RestReason.VACATION && promptAt != null &&
            !now.isBefore(promptAt.plusHours(Balance.ILLNESS_CONFIRM_GRACE_HOURS.toLong()))

    /** Los 7 días siguientes al fin de un descanso. */
    fun isReturnWeek(day: LocalDate, history: List<RestPeriod>): Boolean = history.any { p ->
        val end = p.effectiveEnd ?: return@any false
        day.isAfter(end) && !day.isAfter(end.plusDays(Balance.REST_RETURN_DAYS.toLong()))
    }

    fun suggestsReevaluation(p: RestPeriod): Boolean {
        val end = p.effectiveEnd ?: return false
        return ChronoUnit.DAYS.between(p.start, end) + 1 > Balance.LONG_REST_DAYS
    }

    private fun daysInYear(p: RestPeriod, year: Int): Int {
        val end = p.effectiveEnd ?: return 0
        val from = maxOf(p.start, LocalDate.of(year, 1, 1))
        val to = minOf(end, LocalDate.of(year, 12, 31))
        return if (to.isBefore(from)) 0 else (ChronoUnit.DAYS.between(from, to) + 1).toInt()
    }

    private fun overlaps(p: RestPeriod, start: LocalDate, end: LocalDate?): Boolean {
        val pEnd = p.effectiveEnd
        val startsBeforeOtherEnds = pEnd == null || !start.isAfter(pEnd)
        val otherStartsBeforeEnd = end == null || !p.start.isAfter(end)
        return startsBeforeOtherEnds && otherStartsBeforeEnd
    }
}
