package io.github.adelledgar.ariseup.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.adelledgar.ariseup.domain.model.DailyExercise
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.MissionKind
import io.github.adelledgar.ariseup.domain.model.RoutineCategory
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate

/**
 * Una misión de un día. [target] y [progress] son JSON cuyo formato depende de [kind]:
 * DAILY/PENALTY → [DailyTargets]/[DailyProgress]; URGENT → [UrgentTarget]/[UrgentProgress].
 * [refId] apunta a la rutina (ROUTINE) o a la regla (DIET_RULE).
 */
@Serializable
@Entity(tableName = "mission", indices = [Index("date")])
data class MissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Serializable(LocalDateSerializer::class) val date: LocalDate,
    val kind: MissionKind,
    val mandatory: Boolean,
    val refId: Long? = null,
    val title: String,
    val target: String? = null,
    val progress: String? = null,
    @Serializable(InstantSerializer::class) val completedAt: Instant? = null,
    @Serializable(InstantSerializer::class) val appearAt: Instant? = null,
    @Serializable(InstantSerializer::class) val deadline: Instant? = null,
)

@Serializable
data class DailyProgress(
    val pushUps: Int = 0,
    val sitUps: Int = 0,
    val squats: Int = 0,
    val runKm: Double = 0.0,
    val runMinutes: Double = 0.0,
) {
    fun add(exercise: DailyExercise, amount: Double): DailyProgress = when (exercise) {
        DailyExercise.PUSH_UPS -> copy(pushUps = maxOf(0, pushUps + amount.toInt()))
        DailyExercise.SIT_UPS -> copy(sitUps = maxOf(0, sitUps + amount.toInt()))
        DailyExercise.SQUATS -> copy(squats = maxOf(0, squats + amount.toInt()))
        DailyExercise.RUN -> copy(runKm = maxOf(0.0, runKm + amount))
    }

    fun of(exercise: DailyExercise): Double = when (exercise) {
        DailyExercise.PUSH_UPS -> pushUps.toDouble()
        DailyExercise.SIT_UPS -> sitUps.toDouble()
        DailyExercise.SQUATS -> squats.toDouble()
        DailyExercise.RUN -> runKm
    }

    fun meets(t: DailyTargets): Boolean =
        pushUps >= t.pushUps && sitUps >= t.sitUps && squats >= t.squats && runKm >= t.runKm - 1e-9
}

fun DailyTargets.of(exercise: DailyExercise): Double = when (exercise) {
    DailyExercise.PUSH_UPS -> pushUps.toDouble()
    DailyExercise.SIT_UPS -> sitUps.toDouble()
    DailyExercise.SQUATS -> squats.toDouble()
    DailyExercise.RUN -> runKm
}

@Serializable
data class UrgentTarget(val exercise: DailyExercise, val amount: Double)

@Serializable
data class UrgentProgress(val done: Double = 0.0)

enum class ExerciseType { REPS, REPS_WEIGHT, TIME, DISTANCE }

@Serializable
@Entity(tableName = "exercise")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: ExerciseType,
    val builtIn: Boolean,
)

@Serializable
@Entity(tableName = "routine")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Bit 0 = lunes … bit 6 = domingo. */
    val daysOfWeekMask: Int,
    val category: RoutineCategory,
    val mandatory: Boolean,
    val archived: Boolean = false,
)

@Serializable
@Entity(tableName = "routine_exercise", indices = [Index("routineId")])
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long,
    val sets: Int,
    /** Repeticiones, segundos o metros por serie, según el tipo de ejercicio. */
    val targetPerSet: Double,
    val position: Int,
)

@Serializable
@Entity(tableName = "set_log", indices = [Index("missionId")])
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val missionId: Long,
    val routineExerciseId: Long,
    val setIndex: Int,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val seconds: Int? = null,
    val meters: Double? = null,
)
