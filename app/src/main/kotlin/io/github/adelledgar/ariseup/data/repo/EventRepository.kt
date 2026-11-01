package io.github.adelledgar.ariseup.data.repo

import io.github.adelledgar.ariseup.data.db.AppJson
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.EventEntity
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

class EventRepository(private val db: AriseUpDatabase, private val clock: Clock) {
    suspend fun append(events: List<SystemEvent>, day: LocalDate? = null) {
        if (events.isEmpty()) return
        val at = clock.instant()
        db.eventDao().insertAll(events.map { EventEntity(at = at, day = day, type = typeOf(it), payload = encode(it)) })
    }

    fun recent(limit: Int): Flow<List<Pair<EventEntity, SystemEvent>>> =
        db.eventDao().observeRecent(limit).map { list -> list.mapNotNull { e -> decode(e)?.let { e to it } } }

    companion object {
        fun typeOf(e: SystemEvent): String = e::class.simpleName ?: "Unknown"

        fun encode(e: SystemEvent): String = AppJson.encodeToString(SystemEvent.serializer(), e)

        fun decode(e: EventEntity): SystemEvent? = runCatching { AppJson.decodeFromString(SystemEvent.serializer(), e.payload) }.getOrNull()
    }
}
