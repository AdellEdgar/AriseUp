package io.github.adelledgar.ariseup.data.repo

import androidx.room.withTransaction
import io.github.adelledgar.ariseup.data.GameClock
import io.github.adelledgar.ariseup.data.db.AppJson
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.DailyProgress
import io.github.adelledgar.ariseup.data.db.DayStatus
import io.github.adelledgar.ariseup.data.db.MissionEntity
import io.github.adelledgar.ariseup.data.db.UrgentProgress
import io.github.adelledgar.ariseup.data.db.UrgentTarget
import io.github.adelledgar.ariseup.domain.Leveling
import io.github.adelledgar.ariseup.domain.Penalty
import io.github.adelledgar.ariseup.domain.Rewards
import io.github.adelledgar.ariseup.domain.Stats
import io.github.adelledgar.ariseup.domain.model.DailyExercise
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.MissionKind
import io.github.adelledgar.ariseup.domain.model.SystemEvent
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** Misiones del día: creación, registro de progreso y compleción con recompensa (idempotente). */
class MissionRepository(
    private val db: AriseUpDatabase,
    private val players: PlayerRepository,
    private val events: EventRepository,
    private val gameClock: GameClock,
    private val clock: Clock,
) {
    /** Se llama tras cada compleción; la Tarea de logros lo usa para revisar misiones ocultas. */
    var afterCompletion: (suspend () -> List<SystemEvent>)? = null

    fun observe(date: LocalDate): Flow<List<MissionEntity>> = db.missionDao().observeForDate(date)

    suspend fun forDate(date: LocalDate) = db.missionDao().forDate(date)

    suspend fun get(id: Long) = db.missionDao().get(id)

    suspend fun createDaily(date: LocalDate, targets: DailyTargets, mandatory: Boolean = true): Long = db.missionDao().insert(
        MissionEntity(
            date = date, kind = MissionKind.DAILY, mandatory = mandatory, title = "Preparación para volverse fuerte",
            target = AppJson.encodeToString(DailyTargets.serializer(), targets),
            progress = AppJson.encodeToString(DailyProgress.serializer(), DailyProgress()),
        ),
    )

    suspend fun createPenalty(date: LocalDate, targets: DailyTargets): Long = db.missionDao().insert(
        MissionEntity(
            date = date, kind = MissionKind.PENALTY, mandatory = true, title = "Misión de penalización: supervivencia",
            target = AppJson.encodeToString(DailyTargets.serializer(), targets),
            progress = AppJson.encodeToString(DailyProgress.serializer(), DailyProgress()),
        ),
    )

    suspend fun createUrgent(date: LocalDate, exercise: DailyExercise, amount: Double, appearAt: Instant, deadline: Instant): Long =
        db.missionDao().insert(
            MissionEntity(
                date = date, kind = MissionKind.URGENT, mandatory = false, title = "Misión urgente",
                target = AppJson.encodeToString(UrgentTarget.serializer(), UrgentTarget(exercise, amount)),
                progress = AppJson.encodeToString(UrgentProgress.serializer(), UrgentProgress()),
                appearAt = appearAt, deadline = deadline,
            ),
        )

    suspend fun createSimple(date: LocalDate, kind: MissionKind, title: String, mandatory: Boolean, refId: Long? = null): Long =
        db.missionDao().insert(MissionEntity(date = date, kind = kind, mandatory = mandatory, refId = refId, title = title))

    /** Suma repeticiones o km a la misión diaria, de penalización o urgente; la completa al llegar al objetivo. */
    suspend fun addDailyProgress(missionId: Long, exercise: DailyExercise, amount: Double, minutes: Double = 0.0): List<SystemEvent> {
        val m = checkNotNull(db.missionDao().get(missionId)) { "Misión inexistente" }
        ensureOpen(m)
        if (m.completedAt != null) return emptyList()
        when (m.kind) {
            MissionKind.DAILY, MissionKind.PENALTY -> {
                val target = AppJson.decodeFromString(DailyTargets.serializer(), m.target!!)
                var progress = AppJson.decodeFromString(DailyProgress.serializer(), m.progress!!).add(exercise, amount)
                if (exercise == DailyExercise.RUN) progress = progress.copy(runMinutes = progress.runMinutes + minutes)
                db.missionDao().setProgress(m.id, AppJson.encodeToString(DailyProgress.serializer(), progress))
                return if (progress.meets(target)) complete(m.id) else emptyList()
            }
            MissionKind.URGENT -> {
                val target = AppJson.decodeFromString(UrgentTarget.serializer(), m.target!!)
                if (target.exercise != exercise) return emptyList()
                if (m.deadline != null && clock.instant() > m.deadline) throw IllegalStateException("[Tiempo agotado]")
                val progress = AppJson.decodeFromString(UrgentProgress.serializer(), m.progress!!).let { it.copy(done = it.done + amount) }
                db.missionDao().setProgress(m.id, AppJson.encodeToString(UrgentProgress.serializer(), progress))
                return if (progress.done >= target.amount - 1e-9) complete(m.id) else emptyList()
            }
            else -> error("La misión ${m.kind} no admite este registro")
        }
    }

    /** Completa una misión y concede su recompensa una sola vez. */
    suspend fun complete(missionId: Long): List<SystemEvent> {
        val result = db.withTransaction {
            val m = db.missionDao().get(missionId) ?: return@withTransaction emptyList()
            if (m.completedAt != null) return@withTransaction emptyList()
            if (m.kind != MissionKind.PENALTY) ensureOpen(m)
            if (db.missionDao().markCompleted(m.id, clock.instant()) == 0) return@withTransaction emptyList()
            val state = players.current() ?: return@withTransaction emptyList()
            val out = mutableListOf<SystemEvent>()
            if (m.kind == MissionKind.PENALTY) {
                val penalty = state.penalty
                if (penalty != null) {
                    out += Penalty.clear(penalty, gameClock.today())
                    players.saveState(state.copy(penalty = null), out)
                }
                return@withTransaction out
            }
            val reward = Rewards.forCompletion(
                kind = m.kind,
                level = state.progress.level,
                shareCount = shareCount(m),
                category = if (m.kind == MissionKind.ROUTINE) m.refId?.let { db.routineDao().get(it) }?.category else null,
                urgentExercise = if (m.kind == MissionKind.URGENT) AppJson.decodeFromString(UrgentTarget.serializer(), m.target!!).exercise else null,
            )
            val (afterXp, xpEvents) = Leveling.applyXp(state.progress, reward.xp, m.title)
            out += xpEvents
            val progress = afterXp.copy(stats = Stats.applyGains(afterXp.stats, reward.stats))
            if (reward.stats.isNotEmpty()) out += SystemEvent.StatsChanged(reward.stats, m.title)
            players.saveState(state.copy(progress = progress), out, m.date)
            out
        }
        return result + (afterCompletion?.invoke() ?: emptyList())
    }

    private suspend fun shareCount(m: MissionEntity): Int = when (m.kind) {
        MissionKind.ROUTINE -> db.missionDao().forDateAndKind(m.date, MissionKind.ROUTINE).size
        else -> 1
    }

    /** Las misiones de un día cerrado ya no se pueden tocar (salvo la penalización, que no caduca). */
    private suspend fun ensureOpen(m: MissionEntity) {
        if (m.kind == MissionKind.PENALTY) return
        val day = db.dayDao().get(m.date)
        if (day != null && day.status != DayStatus.OPEN) throw IllegalStateException("[Este día ya está cerrado]")
    }
}
