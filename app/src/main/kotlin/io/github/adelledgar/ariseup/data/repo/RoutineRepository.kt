package io.github.adelledgar.ariseup.data.repo

import androidx.room.withTransaction
import io.github.adelledgar.ariseup.data.db.AriseUpDatabase
import io.github.adelledgar.ariseup.data.db.ExerciseEntity
import io.github.adelledgar.ariseup.data.db.ExerciseType
import io.github.adelledgar.ariseup.data.db.RoutineEntity
import io.github.adelledgar.ariseup.data.db.RoutineExerciseEntity
import io.github.adelledgar.ariseup.data.db.SetLogEntity
import io.github.adelledgar.ariseup.data.seed.BuiltInExercises
import java.time.LocalDate

data class RoutineExercise(val link: RoutineExerciseEntity, val exercise: ExerciseEntity)

data class RoutineWithExercises(val routine: RoutineEntity, val exercises: List<RoutineExercise>)

/** ¿Alcanza esta serie el objetivo según el tipo de ejercicio? */
fun SetLogEntity.meets(target: Double, type: ExerciseType): Boolean = when (type) {
    ExerciseType.REPS, ExerciseType.REPS_WEIGHT -> (reps ?: 0) >= target
    ExerciseType.TIME -> (seconds ?: 0) >= target
    ExerciseType.DISTANCE -> (meters ?: 0.0) >= target - 1e-9
}

class RoutineRepository(private val db: AriseUpDatabase) {
    private val dao get() = db.routineDao()

    suspend fun seedExercisesIfEmpty() {
        if (dao.exerciseCount() == 0) dao.insertExerciseDefs(BuiltInExercises.all)
    }

    suspend fun active() = dao.active()

    fun observeActive() = dao.observeActive()

    suspend fun exercises() = dao.exercises()

    fun observeExercises() = dao.observeExercises()

    suspend fun addExercise(name: String, type: ExerciseType): Long = dao.insertExerciseDef(ExerciseEntity(name = name.trim(), type = type, builtIn = false))

    suspend fun withExercises(id: Long): RoutineWithExercises? {
        val r = dao.get(id) ?: return null
        val links = dao.exercisesOf(id).mapNotNull { link -> dao.exercise(link.exerciseId)?.let { RoutineExercise(link, it) } }
        return RoutineWithExercises(r, links)
    }

    /** Rutinas activas que tocan ese día de la semana. */
    suspend fun activeFor(day: LocalDate): List<RoutineWithExercises> {
        val bit = 1 shl (day.dayOfWeek.value - 1)
        return dao.active().filter { it.daysOfWeekMask and bit != 0 }.mapNotNull { withExercises(it.id) }
    }

    suspend fun save(routine: RoutineEntity, exercises: List<RoutineExerciseEntity>): Long = db.withTransaction {
        val id = if (routine.id == 0L) dao.insert(routine) else { dao.upsert(routine); routine.id }
        dao.clearExercises(id)
        exercises.forEachIndexed { i, e -> dao.insertExercise(e.copy(id = 0, routineId = id, position = i)) }
        id
    }

    suspend fun archive(id: Long) {
        dao.get(id)?.let { dao.upsert(it.copy(archived = true)) }
    }

    suspend fun setsOf(missionId: Long) = dao.setsOf(missionId)

    fun observeSets(missionId: Long) = dao.observeSets(missionId)
}
