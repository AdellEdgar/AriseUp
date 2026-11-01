package io.github.adelledgar.ariseup.system

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.adelledgar.ariseup.AriseUpApp
import io.github.adelledgar.ariseup.data.db.UrgentTarget
import io.github.adelledgar.ariseup.data.db.AppJson
import io.github.adelledgar.ariseup.domain.RestMode
import io.github.adelledgar.ariseup.domain.model.DailyExercise
import io.github.adelledgar.ariseup.domain.model.MissionKind
import java.time.LocalDateTime

private val Context.container get() = (applicationContext as AriseUpApp).container

/** Cierre de día: al reinicio y como respaldo periódico. */
class DayCloseWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val atReset = inputData.getBoolean(KEY_AT_RESET, false)
        try {
            val events = c.coordinator.catchUp()
            c.notifier.postCloseEvents(events)
            if (atReset && c.db.missionDao().forDateAndKind(c.gameClock.today(), MissionKind.DAILY).isNotEmpty()) {
                c.notifier.post(NotificationKind.DAILY, "[Misión diaria disponible: Preparación para volverse fuerte]")
            }
        } finally {
            // La cadena de reinicios no se corta aunque el cierre falle.
            if (atReset) c.scheduler.scheduleNextReset()
        }
        return Result.success()
    }

    companion object {
        const val KEY_AT_RESET = "at_reset"
    }
}

/** Avisos programados: plazo, pesaje y confirmación de descanso. */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        if (c.players.current() == null) return Result.success()
        val today = c.gameClock.today()
        when (inputData.getString(KEY_KIND)) {
            KIND_DEADLINE -> {
                val pending = c.db.missionDao().forDate(today).any {
                    it.mandatory && it.completedAt == null && it.kind in setOf(MissionKind.DAILY, MissionKind.ROUTINE)
                }
                if (pending) c.notifier.post(NotificationKind.DEADLINE, "[Advertencia: quedan 2 horas para completar las misiones obligatorias]")
                c.scheduler.scheduleDeadline()
            }
            KIND_WEIGH_IN -> {
                val due = c.db.missionDao().forDateAndKind(today, MissionKind.WEIGH_IN).any { it.completedAt == null }
                if (due && c.body.on(today) == null) c.notifier.post(NotificationKind.WEIGH_IN, "[Es hora de registrar tu condición física]")
                c.scheduler.scheduleWeighIn()
            }
            KIND_REST -> {
                val now = LocalDateTime.ofInstant(c.clock.instant(), c.clock.zone)
                c.db.restDao().all().filter { it.confirmPromptAt == null && RestMode.needsConfirmation(it.toDomain(), today) }.forEach {
                    c.db.restDao().upsert(it.copy(confirmPromptAt = now))
                    c.notifier.post(NotificationKind.REST, "[Confirma que sigues en Estado de descanso. Si no lo confirmas en 24 h, terminará.]")
                }
            }
        }
        return Result.success()
    }

    companion object {
        const val KEY_KIND = "kind"
        const val KIND_DEADLINE = "deadline"
        const val KIND_WEIGH_IN = "weigh_in"
        const val KIND_REST = "rest"
    }
}

/** Aviso de misión urgente al aparecer. */
class UrgentAppearWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val m = c.db.missionDao().get(inputData.getLong(KEY_MISSION, -1)) ?: return Result.success()
        if (m.completedAt != null) return Result.success()
        val t = AppJson.decodeFromString(UrgentTarget.serializer(), m.target!!)
        val amount = if (t.exercise == DailyExercise.RUN) "${t.amount} km" else "${t.amount.toInt()}"
        val hours = m.deadline?.let { java.time.Duration.between(c.clock.instant(), it).toHours() } ?: 0
        c.notifier.post(NotificationKind.URGENT, "[Misión urgente: $amount ${t.exercise.label.lowercase()} en menos de $hours h. Recompensa: XP doble.]")
        return Result.success()
    }

    companion object {
        const val KEY_MISSION = "mission"
    }
}
