package io.github.adelledgar.ariseup.system

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.adelledgar.ariseup.MainActivity
import io.github.adelledgar.ariseup.R
import io.github.adelledgar.ariseup.domain.model.SystemEvent

enum class NotificationKind(val channelId: String, val channelName: String, val important: Boolean) {
    DAILY("missions", "Misiones", false),
    DEADLINE("deadline", "Plazos", true),
    URGENT("urgent", "Misiones urgentes", true),
    PENALTY("penalty", "Penalización", true),
    LEVEL("level", "Nivel y rango", false),
    REEVALUATION("reevaluation", "Reevaluación", false),
    REST("rest", "Estado de descanso", false),
    WEIGH_IN("weigh_in", "Pesaje", false),
}

/** Notificaciones del Sistema (spec §4.4). */
class Notifier(private val context: Context) {
    fun createChannels() {
        val nm = context.getSystemService(NotificationManager::class.java)
        NotificationKind.entries.forEach {
            val importance = if (it.important) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            nm.createNotificationChannel(NotificationChannel(it.channelId, it.channelName, importance))
        }
    }

    fun canPost(): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun post(kind: NotificationKind, text: String, title: String = "[Notificación del Sistema]") {
        if (!canPost()) return
        val intent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, kind.channelId)
            .setSmallIcon(R.drawable.ic_stat_system)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(0xFF3FD8FF.toInt())
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(kind.ordinal, n)
        } catch (_: SecurityException) {
            // El usuario retiró el permiso entre la comprobación y el aviso.
        }
    }

    /** Avisa de lo importante que haya pasado en un cierre de día. */
    fun postCloseEvents(events: List<SystemEvent>) {
        when {
            events.any { it is SystemEvent.LevelDown } -> {
                val lvl = events.filterIsInstance<SystemEvent.LevelDown>().last().newLevel
                post(NotificationKind.PENALTY, "[Has bajado al nivel $lvl. Completa la misión de penalización para detener la degradación.]")
            }
            events.any { it is SystemEvent.PenaltyDegraded } ->
                post(NotificationKind.PENALTY, "[Degradación aplicada: has perdido XP y stats. Completa la misión de penalización.]")
            events.any { it is SystemEvent.PenaltyStarted } ->
                post(NotificationKind.PENALTY, "[Has entrado en la Zona de Penalización]")
        }
    }
}
