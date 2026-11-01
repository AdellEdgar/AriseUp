package io.github.adelledgar.ariseup.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** El día de juego D va de `D@resetTime` a `(D+1)@resetTime` en hora local. */
object GameDay {
    fun of(instant: Instant, zone: ZoneId, resetTime: LocalTime): LocalDate {
        val local = instant.atZone(zone)
        val date = local.toLocalDate()
        return if (instant < startOf(date, zone, resetTime)) date.minusDays(1) else date
    }

    /** Si la hora de reinicio cae en el hueco de un cambio de hora, el día empieza justo al terminar el hueco. */
    fun startOf(day: LocalDate, zone: ZoneId, resetTime: LocalTime): Instant {
        val local = LocalDateTime.of(day, resetTime)
        val transition = zone.rules.getTransition(local)
        if (transition != null && transition.isGap) return transition.instant
        return ZonedDateTime.of(local, zone).toInstant()
    }
}
