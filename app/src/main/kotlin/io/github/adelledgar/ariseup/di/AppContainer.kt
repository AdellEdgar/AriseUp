package io.github.adelledgar.ariseup.di

import android.content.Context
import androidx.room.Room
import io.github.adelledgar.ariseup.data.GameClock
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.prefs.SettingsStore
import io.github.adelledgar.ariseup.data.repo.BodyRepository
import io.github.adelledgar.ariseup.data.repo.EventRepository
import io.github.adelledgar.ariseup.data.repo.FoodRepository
import io.github.adelledgar.ariseup.data.repo.MissionRepository
import io.github.adelledgar.ariseup.data.repo.PlayerRepository
import io.github.adelledgar.ariseup.data.repo.RoutineRepository
import io.github.adelledgar.ariseup.system.DayCloseCoordinator
import io.github.adelledgar.ariseup.system.MissionPlanner
import io.github.adelledgar.ariseup.system.Notifier
import io.github.adelledgar.ariseup.system.Scheduler
import java.time.Clock

/** Inyección de dependencias manual: un único contenedor por proceso. */
class AppContainer(
    val context: Context,
    val clock: Clock = Clock.systemDefaultZone(),
    val db: AriseUpDatabase = Room.databaseBuilder(context, AriseUpDatabase::class.java, AriseUpDatabase.NAME).build(),
    val settings: SettingsStore = SettingsStore(context),
) {
    val gameClock = GameClock(clock, settings)
    val events = EventRepository(db, clock)
    val players = PlayerRepository(db, events)
    val routines = RoutineRepository(db)
    val missions = MissionRepository(db, players, events, gameClock, clock)
    val foods = FoodRepository(db, clock)
    val body = BodyRepository(db)
    val notifier = Notifier(context)
    val scheduler = Scheduler(context, clock, gameClock, settings)
    val planner = MissionPlanner(db, players, missions, foods, settings, gameClock).also {
        it.onUrgentCreated = { id, at -> scheduler.scheduleUrgent(id, at) }
    }
    val coordinator = DayCloseCoordinator(db, players, foods, settings, gameClock, planner)
}
