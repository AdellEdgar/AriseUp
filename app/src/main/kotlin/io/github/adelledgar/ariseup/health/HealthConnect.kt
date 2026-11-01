package io.github.adelledgar.ariseup.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.room.withTransaction
import io.github.adelledgar.ariseup.data.GameClock
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.DayStatus
import io.github.adelledgar.ariseup.data.db.HealthImportEntity
import io.github.adelledgar.ariseup.data.repo.MissionRepository
import io.github.adelledgar.ariseup.domain.model.DailyExercise
import io.github.adelledgar.ariseup.domain.model.MissionKind
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

data class HealthRun(val recordId: String, val start: Instant, val meters: Double, val minutes: Double)

/** Fuente de carreras (Health Connect en la app, falsa en los tests). */
interface RunSource {
    suspend fun available(): Boolean
    suspend fun runsBetween(start: Instant, end: Instant): List<HealthRun>
}

class HealthConnectGateway(private val context: Context) : RunSource {
    val permissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
    )

    fun sdkAvailable(): Boolean = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasPermissions(): Boolean =
        sdkAvailable() && HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions().containsAll(permissions)

    override suspend fun available(): Boolean = runCatching { hasPermissions() }.getOrDefault(false)

    override suspend fun runsBetween(start: Instant, end: Instant): List<HealthRun> {
        if (!available()) return emptyList()
        val client = HealthConnectClient.getOrCreate(context)
        val sessions = client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(start, end))).records
            .filter { it.exerciseType in RUN_TYPES }
        return sessions.map { s ->
            val meters = client.aggregate(AggregateRequest(setOf(DistanceRecord.DISTANCE_TOTAL), TimeRangeFilter.between(s.startTime, s.endTime)))[DistanceRecord.DISTANCE_TOTAL]
                ?.inMeters ?: 0.0
            HealthRun(s.metadata.id, s.startTime, meters, Duration.between(s.startTime, s.endTime).toMinutes().toDouble())
        }
    }

    private companion object {
        val RUN_TYPES = setOf(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL)
    }
}

/** Suma las carreras de un día a la fila "Correr" de la misión diaria y de la penalización, sin duplicar. */
class HealthImporter(
    private val db: AriseUpDatabase,
    private val missions: MissionRepository,
    private val gameClock: GameClock,
    private val source: RunSource,
) {
    suspend fun import(day: LocalDate) {
        if (!source.available()) return
        if (db.dayDao().get(day)?.status?.let { it != DayStatus.OPEN } == true) return
        val runs = source.runsBetween(gameClock.startOf(day), gameClock.startOf(day.plusDays(1))).distinctBy { it.recordId }
        val daily = db.missionDao().forDateAndKind(day, MissionKind.DAILY).firstOrNull()
        val penalty = db.missionDao().openOfKind(MissionKind.PENALTY)
        if (daily == null && penalty == null) return
        for (run in runs) {
            if (db.healthImportDao().exists(run.recordId) || run.meters <= 0) continue
            val km = run.meters / 1000
            // Todo o nada: si falla a medias, la carrera se reintenta entera la próxima vez.
            db.withTransaction {
                daily?.takeIf { it.completedAt == null }?.let { missions.addDailyProgress(it.id, DailyExercise.RUN, km, run.minutes) }
                penalty?.let { missions.addDailyProgress(it.id, DailyExercise.RUN, km, run.minutes) }
                db.healthImportDao().insert(HealthImportEntity(run.recordId, daily?.id ?: penalty!!.id))
            }
        }
    }

    /** Importa los días aún abiertos (incluido hoy). Se llama con la app en primer plano, que es cuando Health Connect deja leer. */
    suspend fun importOpenDays() {
        if (!source.available()) return
        db.dayDao().withStatus(DayStatus.OPEN).forEach { import(it.date) }
    }
}
