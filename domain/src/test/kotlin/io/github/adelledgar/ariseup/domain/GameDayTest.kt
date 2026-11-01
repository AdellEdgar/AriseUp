package io.github.adelledgar.ariseup.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class GameDayTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun at(text: String) = LocalDateTime.parse(text).atZone(madrid).toInstant()

    @Test
    fun midnightReset() {
        assertEquals(LocalDate.of(2026, 3, 10), GameDay.of(at("2026-03-10T00:00"), madrid, LocalTime.MIDNIGHT))
        assertEquals(LocalDate.of(2026, 3, 9), GameDay.of(at("2026-03-09T23:59"), madrid, LocalTime.MIDNIGHT))
    }

    @Test
    fun beforeResetBelongsToPreviousDay() {
        assertEquals(LocalDate.of(2026, 3, 9), GameDay.of(at("2026-03-10T03:59"), madrid, LocalTime.of(4, 0)))
        assertEquals(LocalDate.of(2026, 3, 10), GameDay.of(at("2026-03-10T04:00"), madrid, LocalTime.of(4, 0)))
    }

    @Test
    fun startOfRoundTrips() {
        val start = GameDay.startOf(LocalDate.of(2026, 7, 1), madrid, LocalTime.of(4, 0))
        assertEquals(at("2026-07-01T04:00"), start)
        assertEquals(LocalDate.of(2026, 7, 1), GameDay.of(start, madrid, LocalTime.of(4, 0)))
    }

    @Test
    fun dstSpringForward() {
        // En Madrid, el 2026-03-29 se salta de 02:00 a 03:00.
        val reset = LocalTime.of(2, 30)
        assertEquals(LocalDate.of(2026, 3, 29), GameDay.of(at("2026-03-29T03:10"), madrid, reset))
        val start = GameDay.startOf(LocalDate.of(2026, 3, 29), madrid, reset)
        assertEquals(LocalDate.of(2026, 3, 29), GameDay.of(start, madrid, reset))
    }
}
