package io.github.adelledgar.ariseup.ui.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.adelledgar.ariseup.data.db.ExerciseEntity
import io.github.adelledgar.ariseup.data.db.ExerciseType
import io.github.adelledgar.ariseup.data.db.RoutineEntity
import io.github.adelledgar.ariseup.data.db.RoutineExerciseEntity
import io.github.adelledgar.ariseup.di.AppContainer
import io.github.adelledgar.ariseup.domain.model.RoutineCategory
import io.github.adelledgar.ariseup.ui.ScreenColumn
import io.github.adelledgar.ariseup.ui.components.ChoiceChips
import io.github.adelledgar.ariseup.ui.components.InfoRow
import io.github.adelledgar.ariseup.ui.components.SystemButton
import io.github.adelledgar.ariseup.ui.components.SystemTextField
import io.github.adelledgar.ariseup.ui.components.SystemWindow
import io.github.adelledgar.ariseup.ui.components.toDecimalOrNull
import io.github.adelledgar.ariseup.ui.theme.LocalSystemPalette
import io.github.adelledgar.ariseup.ui.theme.SystemColors
import kotlinx.coroutines.launch

private val DAYS = listOf("L", "M", "X", "J", "V", "S", "D")

fun daysLabel(mask: Int) = DAYS.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.joinToString(" ").ifEmpty { "Ningún día" }

@Composable
fun RoutineListRoute(container: AppContainer, onEdit: (Long) -> Unit, onBack: () -> Unit) {
    val routines by container.routines.observeActive().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    ScreenColumn {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = LocalSystemPalette.current.accent) }
                Text("[Rutinas]", style = MaterialTheme.typography.headlineMedium, color = LocalSystemPalette.current.accent)
            }
        }
        item { SystemButton("Nueva rutina", { onEdit(0) }, Modifier.fillMaxWidth()) }
        routines.forEach { r ->
            item(key = r.id) {
                SystemWindow(r.name) {
                    InfoRow("Días", daysLabel(r.daysOfWeekMask))
                    InfoRow("Categoría", r.category.label)
                    InfoRow("Tipo", if (r.mandatory) "Obligatoria" else "Opcional")
                    Row {
                        TextButton(onClick = { onEdit(r.id) }) { Text("Editar") }
                        TextButton(onClick = { scope.launch { container.routines.archive(r.id) } }) { Text("Archivar", color = SystemColors.danger) }
                    }
                }
            }
        }
    }
}

private class DraftExercise(val exercise: ExerciseEntity, sets: Int, target: Double) {
    var sets by mutableStateOf(sets.toString())
    var target by mutableStateOf(target.toInt().toString())
}

@Composable
fun RoutineEditorRoute(container: AppContainer, id: Long, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val library by container.routines.observeExercises().collectAsStateWithLifecycle(emptyList())
    var name by remember { mutableStateOf("") }
    var mask by remember { mutableStateOf(0) }
    var category by remember { mutableStateOf(RoutineCategory.STRENGTH) }
    var mandatory by remember { mutableStateOf(true) }
    val items = remember { mutableStateListOf<DraftExercise>() }
    var error by remember { mutableStateOf<String?>(null) }
    var newExercise by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf(ExerciseType.REPS) }

    LaunchedEffect(id) {
        if (id != 0L) container.routines.withExercises(id)?.let { r ->
            name = r.routine.name
            mask = r.routine.daysOfWeekMask
            category = r.routine.category
            mandatory = r.routine.mandatory
            items.clear()
            r.exercises.forEach { items += DraftExercise(it.exercise, it.link.sets, it.link.targetPerSet) }
        }
    }

    ScreenColumn {
        item {
            SystemWindow(if (id == 0L) "Nueva rutina" else "Editar rutina", onClose = onDone) {
                SystemTextField(name, { name = it }, "Nombre")
                Text("Días", color = SystemColors.muted)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DAYS.forEachIndexed { i, d ->
                        val on = mask and (1 shl i) != 0
                        TextButton(onClick = { mask = mask xor (1 shl i) }) {
                            Text(d, color = if (on) LocalSystemPalette.current.accent else SystemColors.muted, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                Text("Categoría", color = SystemColors.muted)
                ChoiceChips(RoutineCategory.entries, category, { it.label }, { category = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Obligatoria (si la fallas, penalización)", color = SystemColors.text, modifier = Modifier.weight(1f))
                    Switch(mandatory, { mandatory = it }, colors = SwitchDefaults.colors(checkedTrackColor = LocalSystemPalette.current.accent))
                }
            }
        }
        item {
            SystemWindow("Ejercicios") {
                items.forEachIndexed { i, item ->
                    Text(item.exercise.name, color = LocalSystemPalette.current.accent, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SystemTextField(item.sets, { item.sets = it }, "Series", numeric = true, modifier = Modifier.weight(1f))
                        SystemTextField(item.target, { item.target = it }, unitOf(item.exercise.type) + "/serie", numeric = true, modifier = Modifier.weight(1f))
                        IconButton(onClick = { items.removeAt(i) }) { Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = SystemColors.danger) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                ExercisePicker(library) { items += DraftExercise(it, 3, if (it.type == ExerciseType.TIME) 30.0 else if (it.type == ExerciseType.DISTANCE) 1000.0 else 10.0) }
                Spacer(Modifier.height(8.dp))
                Text("¿No está en la lista?", color = SystemColors.muted)
                SystemTextField(newExercise, { newExercise = it }, "Nuevo ejercicio")
                ChoiceChips(ExerciseType.entries, newType, { typeLabel(it) }, { newType = it })
                SystemButton("Crear ejercicio", {
                    if (newExercise.isNotBlank()) scope.launch {
                        val newId = container.routines.addExercise(newExercise, newType)
                        items += DraftExercise(ExerciseEntity(newId, newExercise.trim(), newType, false), 3, 10.0)
                        newExercise = ""
                    }
                })
            }
        }
        item {
            error?.let { Text(it, color = SystemColors.danger) }
            SystemButton("Guardar rutina", {
                val links = items.map {
                    RoutineExerciseEntity(routineId = id, exerciseId = it.exercise.id, sets = it.sets.toDecimalOrNull()?.toInt() ?: 0, targetPerSet = it.target.toDecimalOrNull() ?: 0.0, position = 0)
                }
                error = when {
                    name.isBlank() -> "Ponle un nombre"
                    mask == 0 -> "Elige al menos un día"
                    links.isEmpty() -> "Añade al menos un ejercicio"
                    links.any { it.sets <= 0 || it.targetPerSet <= 0 } -> "Series y objetivo deben ser mayores que 0"
                    else -> null
                }
                if (error == null) scope.launch {
                    container.routines.save(RoutineEntity(id = id, name = name.trim(), daysOfWeekMask = mask, category = category, mandatory = mandatory), links)
                    onDone()
                }
            }, Modifier.fillMaxWidth())
            Spacer(Modifier.width(1.dp))
            Text("Los cambios se aplican a partir de las misiones de mañana.", color = SystemColors.muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun typeLabel(t: ExerciseType) = when (t) {
    ExerciseType.REPS -> "Repeticiones"
    ExerciseType.REPS_WEIGHT -> "Reps + peso"
    ExerciseType.TIME -> "Tiempo"
    ExerciseType.DISTANCE -> "Distancia"
}

@Composable
private fun ExercisePicker(library: List<ExerciseEntity>, onPick: (ExerciseEntity) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        SystemButton("Añadir ejercicio", { open = true }, Modifier.fillMaxWidth())
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            library.forEach { e ->
                DropdownMenuItem(text = { Text("${e.name} · ${typeLabel(e.type)}") }, onClick = { onPick(e); open = false })
            }
        }
    }
}
