package io.github.adelledgar.ariseup.data.repo

import androidx.room.withTransaction
import io.github.adelledgar.ariseup.data.GameClock
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.RestPeriodEntity
import io.github.adelledgar.ariseup.domain.Balance
import io.github.adelledgar.ariseup.domain.RestMode
import io.github.adelledgar.ariseup.domain.model.RestReason
import io.github.adelledgar.ariseup.domain.model.RestValidation
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import java.time.LocalDate

/** Estado de descanso: iniciar, confirmar y terminar (spec §2.9). */
class RestService(private val db: AriseUpDatabase, private val events: EventRepository, private val gameClock: GameClock) {
    fun observe() = db.restDao().observeAll()

    suspend fun vacationUsage(): String {
        val used = RestMode.vacationDaysUsed(gameClock.today().year, db.restDao().all().map { it.toDomain() })
        return "$used/${Balance.VACATION_DAYS_PER_YEAR}"
    }

    suspend fun start(reason: RestReason, start: LocalDate, end: LocalDate?): RestValidation {
        val today = gameClock.today()
        val result = RestMode.validateStart(reason, start, end, today, db.penaltyDao().get() != null, db.restDao().all().map { it.toDomain() })
        if (result != RestValidation.Ok) return result
        db.withTransaction {
            db.restDao().insert(RestPeriodEntity(reason = reason, start = start, end = end, lastConfirmed = today))
            // Si empieza hoy, las misiones de hoy dejan de ser obligatorias.
            if (start == today) {
                db.dayDao().get(today)?.let { db.dayDao().upsert(it.copy(restActive = true)) }
                db.missionDao().forDate(today).filter { it.mandatory }.forEach { db.missionDao().upsert(it.copy(mandatory = false)) }
            }
            events.append(listOf(SystemEvent.RestStarted(reason.label)), today)
        }
        return result
    }

    suspend fun confirm(id: Long) {
        db.restDao().all().firstOrNull { it.id == id }?.let {
            db.restDao().upsert(it.copy(lastConfirmed = gameClock.today(), confirmPromptAt = null))
        }
    }

    /** Termina hoy (hoy sigue siendo día de descanso). */
    suspend fun endNow(id: Long) {
        val today = gameClock.today()
        db.restDao().all().firstOrNull { it.id == id }?.let {
            db.restDao().upsert(it.copy(endedEarly = maxOf(it.start, today)))
            events.append(listOf(SystemEvent.RestEnded), today)
        }
    }
}
