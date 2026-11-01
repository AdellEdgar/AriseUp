package io.github.adelledgar.ariseup.data.repo

import io.github.adelledgar.ariseup.data.db.AriseUpDatabase

class RoutineRepository(private val db: AriseUpDatabase) {
    suspend fun active() = db.routineDao().active()
}
