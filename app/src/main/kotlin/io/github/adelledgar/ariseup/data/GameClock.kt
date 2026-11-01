package io.github.adelledgar.ariseup.data

import io.github.adelledgar.ariseup.data.prefs.SettingsStore
import io.github.adelledgar.ariseup.domain.GameDay
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** Traduce el reloj real a días de juego según la hora de reinicio. */
class GameClock(val clock: Clock, private val settings: SettingsStore) {
    suspend fun today(): LocalDate = dayOf(clock.instant())

    suspend fun dayOf(instant: Instant): LocalDate = GameDay.of(instant, clock.zone, settings.current().resetTime)

    suspend fun startOf(day: LocalDate): Instant = GameDay.startOf(day, clock.zone, settings.current().resetTime)

    fun now(): Instant = clock.instant()
}
