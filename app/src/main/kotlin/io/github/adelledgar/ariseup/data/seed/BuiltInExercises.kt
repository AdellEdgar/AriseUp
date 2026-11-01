package io.github.adelledgar.ariseup.data.seed

import io.github.adelledgar.ariseup.data.db.ExerciseEntity
import io.github.adelledgar.ariseup.data.db.ExerciseType

/** Biblioteca inicial de ejercicios habituales. */
object BuiltInExercises {
    private fun e(name: String, type: ExerciseType) = ExerciseEntity(name = name, type = type, builtIn = true)

    val all: List<ExerciseEntity> = listOf(
        e("Flexiones", ExerciseType.REPS),
        e("Flexiones diamante", ExerciseType.REPS),
        e("Flexiones declinadas", ExerciseType.REPS),
        e("Dominadas", ExerciseType.REPS),
        e("Dominadas con lastre", ExerciseType.REPS_WEIGHT),
        e("Fondos en paralelas", ExerciseType.REPS),
        e("Fondos en banco", ExerciseType.REPS),
        e("Remo invertido", ExerciseType.REPS),
        e("Press de banca", ExerciseType.REPS_WEIGHT),
        e("Press militar", ExerciseType.REPS_WEIGHT),
        e("Press con mancuernas", ExerciseType.REPS_WEIGHT),
        e("Remo con barra", ExerciseType.REPS_WEIGHT),
        e("Remo con mancuerna", ExerciseType.REPS_WEIGHT),
        e("Jalón al pecho", ExerciseType.REPS_WEIGHT),
        e("Curl de bíceps", ExerciseType.REPS_WEIGHT),
        e("Extensión de tríceps", ExerciseType.REPS_WEIGHT),
        e("Elevaciones laterales", ExerciseType.REPS_WEIGHT),
        e("Sentadillas", ExerciseType.REPS),
        e("Sentadilla con barra", ExerciseType.REPS_WEIGHT),
        e("Sentadilla búlgara", ExerciseType.REPS_WEIGHT),
        e("Peso muerto", ExerciseType.REPS_WEIGHT),
        e("Peso muerto rumano", ExerciseType.REPS_WEIGHT),
        e("Zancadas", ExerciseType.REPS),
        e("Hip thrust", ExerciseType.REPS_WEIGHT),
        e("Prensa de piernas", ExerciseType.REPS_WEIGHT),
        e("Elevación de gemelos", ExerciseType.REPS),
        e("Abdominales", ExerciseType.REPS),
        e("Elevación de piernas", ExerciseType.REPS),
        e("Plancha", ExerciseType.TIME),
        e("Plancha lateral", ExerciseType.TIME),
        e("Burpees", ExerciseType.REPS),
        e("Saltos de tijera", ExerciseType.REPS),
        e("Comba", ExerciseType.TIME),
        e("Correr", ExerciseType.DISTANCE),
        e("Bicicleta", ExerciseType.DISTANCE),
        e("Natación", ExerciseType.DISTANCE),
        e("Remo (máquina)", ExerciseType.DISTANCE),
        e("Caminar", ExerciseType.DISTANCE),
        e("Estiramientos", ExerciseType.TIME),
        e("Yoga", ExerciseType.TIME),
        e("Movilidad de cadera", ExerciseType.TIME),
    )
}
