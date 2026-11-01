package io.github.adelledgar.ariseup.data.repo

import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.BodyEntryEntity
import io.github.adelledgar.ariseup.data.db.FitnessTestEntity
import io.github.adelledgar.ariseup.domain.model.FitnessTest
import java.time.LocalDate

class BodyRepository(private val db: AriseUpDatabase) {
    suspend fun latest() = db.bodyDao().latest()

    fun observeLatest() = db.bodyDao().observeLatest()

    suspend fun first() = db.bodyDao().first()

    suspend fun on(date: LocalDate) = db.bodyDao().on(date)

    fun observeAll() = db.bodyDao().observeAll()

    suspend fun add(entry: BodyEntryEntity): Long = db.bodyDao().insert(entry)

    suspend fun saveTest(test: FitnessTest, isInitial: Boolean): Long = db.bodyDao().insertTest(FitnessTestEntity.from(test, isInitial))

    suspend fun latestTest(): FitnessTest? = db.bodyDao().latestTest()?.toDomain()

    fun observeTests() = db.bodyDao().observeTests()
}
