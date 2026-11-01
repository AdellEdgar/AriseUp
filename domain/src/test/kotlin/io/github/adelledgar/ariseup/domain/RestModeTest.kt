package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.RestPeriod
import io.github.adelledgar.ariseup.domain.model.RestReason
import io.github.adelledgar.ariseup.domain.model.RestValidation
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RestModeTest {
    private val today = LocalDate.of(2026, 11, 1)

    private fun period(reason: RestReason, start: LocalDate, end: LocalDate?, lastConfirmed: LocalDate = start, endedEarly: LocalDate? = null) =
        RestPeriod(0, reason, start, end, lastConfirmed, endedEarly)

    @Test
    fun noRetroactive() {
        assertEquals(
            RestValidation.Rejected("[No se puede iniciar un descanso en el pasado]"),
            RestMode.validateStart(RestReason.VACATION, today.minusDays(1), today.plusDays(3), today, false, emptyList()),
        )
    }

    @Test
    fun todayAllowed() {
        assertEquals(RestValidation.Ok, RestMode.validateStart(RestReason.VACATION, today, today.plusDays(3), today, false, emptyList()))
    }

    @Test
    fun notInPenalty() {
        assertEquals(
            RestValidation.Rejected("[No disponible durante la Zona de Penalización]"),
            RestMode.validateStart(RestReason.ILLNESS, today, null, today, true, emptyList()),
        )
    }

    @Test
    fun vacationNeedsEnd() {
        assertEquals(
            RestValidation.Rejected("[Las vacaciones necesitan fecha de fin]"),
            RestMode.validateStart(RestReason.VACATION, today, null, today, false, emptyList()),
        )
    }

    @Test
    fun vacationCap() {
        val used = listOf(period(RestReason.VACATION, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 26)))
        assertEquals(26, RestMode.vacationDaysUsed(2026, used))
        assertEquals(
            RestValidation.Rejected("[Límite de 30 días de vacaciones por año alcanzado]"),
            RestMode.validateStart(RestReason.VACATION, today, today.plusDays(4), today, false, used),
        )
        assertEquals(RestValidation.Ok, RestMode.validateStart(RestReason.VACATION, today, today.plusDays(3), today, false, used))
    }

    @Test
    fun vacationCapCountsOnlyDaysInThatYear() {
        val p = listOf(period(RestReason.VACATION, LocalDate.of(2025, 12, 28), LocalDate.of(2026, 1, 3)))
        assertEquals(3, RestMode.vacationDaysUsed(2026, p))
        assertEquals(4, RestMode.vacationDaysUsed(2025, p))
    }

    @Test
    fun endedEarlyCountsOnlyUsedDays() {
        val p = listOf(period(RestReason.VACATION, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 20), endedEarly = LocalDate.of(2026, 8, 5)))
        assertEquals(5, RestMode.vacationDaysUsed(2026, p))
    }

    @Test
    fun illnessUnlimited() {
        val p = listOf(period(RestReason.ILLNESS, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1)))
        assertEquals(RestValidation.Ok, RestMode.validateStart(RestReason.INJURY, today, null, today, false, p))
    }

    @Test
    fun overlappingRestRejected() {
        val p = listOf(period(RestReason.ILLNESS, today.minusDays(2), null))
        assertIs<RestValidation.Rejected>(RestMode.validateStart(RestReason.VACATION, today, today.plusDays(2), today, false, p))
    }

    @Test
    fun illnessConfirmEvery7Days() {
        assertTrue(RestMode.needsConfirmation(period(RestReason.ILLNESS, today.minusDays(10), null, lastConfirmed = today.minusDays(7)), today))
        assertFalse(RestMode.needsConfirmation(period(RestReason.ILLNESS, today.minusDays(10), null, lastConfirmed = today.minusDays(6)), today))
        assertFalse(RestMode.needsConfirmation(period(RestReason.VACATION, today.minusDays(10), today.plusDays(1), lastConfirmed = today.minusDays(9)), today))
    }

    @Test
    fun expiredUnconfirmed() {
        val p = period(RestReason.ILLNESS, today.minusDays(10), null)
        val prompt = LocalDateTime.of(2026, 11, 1, 9, 0)
        assertFalse(RestMode.expiredUnconfirmed(p, prompt.plusHours(23), prompt))
        assertTrue(RestMode.expiredUnconfirmed(p, prompt.plusHours(24), prompt))
        assertFalse(RestMode.expiredUnconfirmed(p, prompt.plusHours(48), null))
    }

    @Test
    fun returnWeek() {
        val endedYesterday = listOf(period(RestReason.VACATION, today.minusDays(5), today.minusDays(1)))
        assertTrue(RestMode.isReturnWeek(today, endedYesterday))
        val endedLongAgo = listOf(period(RestReason.VACATION, today.minusDays(15), today.minusDays(8)))
        assertFalse(RestMode.isReturnWeek(today, endedLongAgo))
        assertFalse(RestMode.isReturnWeek(today.minusDays(3), endedYesterday))
    }

    @Test
    fun longRest() {
        assertTrue(RestMode.suggestsReevaluation(period(RestReason.ILLNESS, today, today.plusDays(21))))
        assertFalse(RestMode.suggestsReevaluation(period(RestReason.ILLNESS, today, today.plusDays(20))))
    }

    @Test
    fun covers() {
        val p = period(RestReason.VACATION, today, today.plusDays(2))
        assertTrue(p.covers(today.plusDays(2)))
        assertFalse(p.covers(today.plusDays(3)))
        assertTrue(period(RestReason.ILLNESS, today, null).covers(today.plusDays(100)))
    }
}
