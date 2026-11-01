package io.github.adelledgar.ariseup.data.repo

import androidx.room.withTransaction
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.PenaltyEntity
import io.github.adelledgar.ariseup.data.db.PlayerEntity
import io.github.adelledgar.ariseup.domain.Stats
import io.github.adelledgar.ariseup.domain.model.PlayerState
import io.github.adelledgar.ariseup.domain.model.Stat
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class PlayerRepository(private val db: AriseUpDatabase, private val events: EventRepository) {
    /** null hasta terminar el primer arranque. */
    fun state(): Flow<PlayerState?> = combine(db.playerDao().observe(), db.penaltyDao().observe()) { p, pen ->
        p?.let { PlayerState(it.progress(), pen?.toDomain(), it.baseline) }
    }

    fun entity(): Flow<PlayerEntity?> = db.playerDao().observe()

    suspend fun current(): PlayerState? {
        val p = db.playerDao().get() ?: return null
        return PlayerState(p.progress(), db.penaltyDao().get()?.toDomain(), p.baseline)
    }

    suspend fun currentEntity(): PlayerEntity? = db.playerDao().get()

    suspend fun saveState(state: PlayerState, newEvents: List<SystemEvent>, day: LocalDate? = null) = db.withTransaction {
        val p = checkNotNull(db.playerDao().get()) { "No hay Jugador" }
        db.playerDao().upsert(p.withProgress(state.progress).copy(baseline = state.baseline))
        if (state.penalty == null) db.penaltyDao().clear() else db.penaltyDao().upsert(PenaltyEntity.from(state.penalty!!))
        events.append(newEvents, day)
    }

    suspend fun saveEntity(p: PlayerEntity) = db.playerDao().upsert(p)

    suspend fun spendFreePoint(stat: Stat) = db.withTransaction {
        val p = checkNotNull(db.playerDao().get())
        val progress = Stats.spendFreePoint(p.progress(), stat)
        db.playerDao().upsert(p.withProgress(progress))
        events.append(listOf(SystemEvent.StatsChanged(mapOf(stat to 1.0), "Punto libre")))
    }
}
