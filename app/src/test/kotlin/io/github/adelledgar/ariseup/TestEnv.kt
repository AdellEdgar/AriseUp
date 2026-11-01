package io.github.adelledgar.ariseup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.adelledgar.ariseup.data.GameClock
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.PlayerEntity
import io.github.adelledgar.ariseup.data.prefs.SettingsStore
import io.github.adelledgar.ariseup.data.repo.BodyRepository
import io.github.adelledgar.ariseup.data.repo.EventRepository
import io.github.adelledgar.ariseup.data.repo.FoodRepository
import io.github.adelledgar.ariseup.data.repo.MissionRepository
import io.github.adelledgar.ariseup.data.repo.PlayerRepository
import io.github.adelledgar.ariseup.data.repo.RoutineRepository
import io.github.adelledgar.ariseup.domain.model.ActivityLevel
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.Goal
import io.github.adelledgar.ariseup.domain.model.Sex
import io.github.adelledgar.ariseup.domain.model.StatBlock
import kotlinx.coroutines.runBlocking
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

/** Reloj que los tests pueden avanzar. */
class MutableClock(var now: Instant, private val zone: ZoneId = ZoneId.of("Europe/Madrid")) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)
    override fun instant(): Instant = now

    fun set(local: String) {
        now = LocalDateTime.parse(local).atZone(zone).toInstant()
    }
}

/** BD en memoria y repositorios reales para tests con Robolectric. */
class TestEnv(start: String = "2026-11-02T10:00") {
    val context: Context = ApplicationProvider.getApplicationContext()
    val clock = MutableClock(Instant.EPOCH).also { it.set(start) }
    val db: AriseUpDatabase = Room.inMemoryDatabaseBuilder(context, AriseUpDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val settings = SettingsStore(context, "test-${UUID.randomUUID()}")
    val gameClock = GameClock(clock, settings)
    val events = EventRepository(db, clock)
    val players = PlayerRepository(db, events)
    val routines = RoutineRepository(db)
    val missions = MissionRepository(db, players, events, gameClock, clock)
    val foods = FoodRepository(db, clock)
    val body = BodyRepository(db)

    val today: LocalDate get() = runBlocking { gameClock.today() }

    fun seedPlayer(level: Int = 1, xp: Long = 0, baseline: DailyTargets? = DailyTargets(10, 10, 10, 1.0), createdOn: LocalDate? = null) = runBlocking {
        db.playerDao().upsert(
            PlayerEntity(
                name = "Jugador", sex = Sex.MALE, birthDate = LocalDate.of(1996, 1, 1), heightCm = 180.0,
                activity = ActivityLevel.MODERATE, goal = Goal.LOSE_FAT, level = level, xp = xp, freePoints = 0,
                stats = StatBlock.initial(), streak = 0, titleId = null, baseline = baseline, createdOn = createdOn ?: today.minusDays(30),
            ),
        )
    }

    @Suppress("unused")
    private val utc = ZoneOffset.UTC
}
