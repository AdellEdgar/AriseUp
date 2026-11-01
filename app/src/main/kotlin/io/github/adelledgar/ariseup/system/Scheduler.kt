package io.github.adelledgar.ariseup.system

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.github.adelledgar.ariseup.data.GameClock
import io.github.adelledgar.ariseup.data.prefs.SettingsStore
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Programa los trabajos de WorkManager (horarios aproximados, sin alarmas exactas). */
class Scheduler(
    private val context: Context,
    private val clock: Clock,
    private val gameClock: GameClock,
    private val settings: SettingsStore,
) {
    private val wm get() = WorkManager.getInstance(context)

    suspend fun scheduleAll() {
        wm.enqueueUniquePeriodicWork(
            "day_close_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DayCloseWorker>(6, TimeUnit.HOURS).build(),
        )
        wm.enqueueUniquePeriodicWork(
            "rest_confirm",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS)
                .setInputData(workDataOf(ReminderWorker.KEY_KIND to ReminderWorker.KIND_REST)).build(),
        )
        scheduleNextReset()
        scheduleDeadline()
        scheduleWeighIn()
    }

    suspend fun scheduleNextReset() {
        val next = gameClock.startOf(gameClock.today().plusDays(1))
        enqueueAt<DayCloseWorker>("day_close_reset", next.plusSeconds(30), DayCloseWorker.KEY_AT_RESET to true)
    }

    suspend fun scheduleDeadline() {
        var at = gameClock.startOf(gameClock.today().plusDays(1)).minus(Duration.ofHours(2))
        if (at <= clock.instant()) at = at.plus(Duration.ofDays(1))
        enqueueAt<ReminderWorker>("deadline", at, ReminderWorker.KEY_KIND to ReminderWorker.KIND_DEADLINE)
    }

    suspend fun scheduleWeighIn() {
        val time = settings.current().weighInTime
        val now = LocalDateTime.ofInstant(clock.instant(), clock.zone)
        var at = now.toLocalDate().atTime(time)
        if (!at.isAfter(now)) at = at.plusDays(1)
        enqueueAt<ReminderWorker>("weigh_in", at.atZone(clock.zone).toInstant(), ReminderWorker.KEY_KIND to ReminderWorker.KIND_WEIGH_IN)
    }

    fun scheduleUrgent(missionId: Long, appearAt: Instant) {
        val delay = Duration.between(clock.instant(), appearAt).toMillis().coerceAtLeast(0)
        wm.enqueueUniqueWork(
            "urgent_$missionId",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<UrgentAppearWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(UrgentAppearWorker.KEY_MISSION to missionId))
                .build(),
        )
    }

    private inline fun <reified W : androidx.work.ListenableWorker> enqueueAt(name: String, at: Instant, vararg data: Pair<String, Any?>) {
        val delay = Duration.between(clock.instant(), at).toMillis().coerceAtLeast(0)
        wm.enqueueUniqueWork(
            name,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<W>().setInitialDelay(delay, TimeUnit.MILLISECONDS).setInputData(workDataOf(*data)).build(),
        )
    }
}
