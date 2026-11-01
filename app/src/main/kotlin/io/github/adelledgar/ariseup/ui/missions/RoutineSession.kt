package io.github.adelledgar.ariseup.ui.missions

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.adelledgar.ariseup.data.db.ExerciseType
import io.github.adelledgar.ariseup.data.db.SetLogEntity
import io.github.adelledgar.ariseup.data.repo.RoutineWithExercises
import io.github.adelledgar.ariseup.data.repo.meets
import io.github.adelledgar.ariseup.ui.components.SystemButton
import io.github.adelledgar.ariseup.ui.components.SystemSounds
import io.github.adelledgar.ariseup.ui.components.SystemTextField
import io.github.adelledgar.ariseup.ui.components.toDecimalOrNull
import io.github.adelledgar.ariseup.ui.theme.LocalSystemPalette
import io.github.adelledgar.ariseup.ui.theme.SystemColors
import kotlinx.coroutines.delay

fun unitOf(type: ExerciseType) = when (type) {
    ExerciseType.REPS, ExerciseType.REPS_WEIGHT -> "reps"
    ExerciseType.TIME -> "s"
    ExerciseType.DISTANCE -> "m"
}

/** Registro serie a serie de una rutina, con temporizador de descanso. */
@Composable
fun RoutineSession(
    routine: RoutineWithExercises,
    sets: List<SetLogEntity>,
    completed: Boolean,
    restSeconds: Int,
    vibrateOnRestEnd: Boolean = true,
    onLog: (reId: Long, setIndex: Int, reps: Int?, weight: Double?, seconds: Int?, meters: Double?) -> Unit,
) {
    var restKey by remember { mutableIntStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        routine.exercises.forEach { re ->
            Text(re.exercise.name, style = MaterialTheme.typography.titleMedium, color = LocalSystemPalette.current.accent)
            Text("${re.link.sets} × ${re.link.targetPerSet.toInt()} ${unitOf(re.exercise.type)}", color = SystemColors.muted)
            (0 until re.link.sets).forEach { i ->
                val logged = sets.firstOrNull { it.routineExerciseId == re.link.id && it.setIndex == i }
                SetRow(i, re.exercise.type, re.link.targetPerSet, logged, enabled = !completed) { reps, w, s, m ->
                    onLog(re.link.id, i, reps, w, s, m)
                    restKey++
                }
            }
        }
        if (restKey > 0 && !completed) RestTimer(restSeconds, restKey, vibrateOnRestEnd)
    }
}

@Composable
private fun SetRow(index: Int, type: ExerciseType, target: Double, logged: SetLogEntity?, enabled: Boolean, onSave: (Int?, Double?, Int?, Double?) -> Unit) {
    var value by remember(logged) {
        mutableStateOf(
            when (type) {
                ExerciseType.REPS, ExerciseType.REPS_WEIGHT -> logged?.reps?.toString()
                ExerciseType.TIME -> logged?.seconds?.toString()
                ExerciseType.DISTANCE -> logged?.meters?.toString()
            } ?: "",
        )
    }
    var weight by remember(logged) { mutableStateOf(logged?.weightKg?.toString() ?: "") }
    val ok = logged?.meets(target, type) == true
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("S${index + 1}", color = SystemColors.muted, modifier = Modifier.width(28.dp))
        SystemTextField(value, { value = it }, unitOf(type), numeric = true, modifier = Modifier.weight(1f))
        if (type == ExerciseType.REPS_WEIGHT) SystemTextField(weight, { weight = it }, "kg", numeric = true, modifier = Modifier.weight(1f))
        if (ok) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Serie cumplida", tint = SystemColors.success)
        } else {
            SystemButton("✓", {
                val v = value.toDecimalOrNull() ?: return@SystemButton
                when (type) {
                    ExerciseType.REPS -> onSave(v.toInt(), null, null, null)
                    ExerciseType.REPS_WEIGHT -> onSave(v.toInt(), weight.toDecimalOrNull(), null, null)
                    ExerciseType.TIME -> onSave(null, null, v.toInt(), null)
                    ExerciseType.DISTANCE -> onSave(null, null, null, v)
                }
            }, enabled = enabled)
        }
    }
}

/** Cuenta atrás de descanso entre series; vibra y suena al terminar. */
@Composable
fun RestTimer(seconds: Int, key: Int, vibrate: Boolean = true) {
    val context = LocalContext.current
    var left by remember(key) { mutableIntStateOf(seconds) }
    LaunchedEffect(key) {
        while (left > 0) {
            delay(1000)
            left--
        }
        if (vibrate) vibrate(context)
        SystemSounds.ding()
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("[Descanso]", color = LocalSystemPalette.current.accent, modifier = Modifier.weight(1f))
        Text(if (left > 0) "%d:%02d".format(left / 60, left % 60) else "¡Siguiente serie!", style = MaterialTheme.typography.titleLarge, color = SystemColors.text)
    }
}

fun vibrate(context: Context, ms: Long = 400) {
    val vibrator = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
}
