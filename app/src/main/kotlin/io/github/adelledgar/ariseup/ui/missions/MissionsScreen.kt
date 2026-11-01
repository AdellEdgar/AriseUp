package io.github.adelledgar.ariseup.ui.missions

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.adelledgar.ariseup.data.db.AppJson
import io.github.adelledgar.ariseup.data.db.DailyProgress
import io.github.adelledgar.ariseup.data.db.MissionEntity
import io.github.adelledgar.ariseup.data.db.UrgentProgress
import io.github.adelledgar.ariseup.data.db.UrgentTarget
import io.github.adelledgar.ariseup.data.prefs.Settings
import io.github.adelledgar.ariseup.di.AppContainer
import io.github.adelledgar.ariseup.domain.model.DailyExercise
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.domain.model.MissionKind
import io.github.adelledgar.ariseup.ui.ScreenColumn
import io.github.adelledgar.ariseup.ui.components.StatBar
import io.github.adelledgar.ariseup.ui.components.SystemButton
import io.github.adelledgar.ariseup.ui.components.SystemMessageHost
import io.github.adelledgar.ariseup.ui.components.SystemTextField
import io.github.adelledgar.ariseup.ui.components.SystemWindow
import io.github.adelledgar.ariseup.ui.components.toDecimalOrNull
import io.github.adelledgar.ariseup.ui.containerViewModel
import io.github.adelledgar.ariseup.ui.theme.LocalSystemPalette
import io.github.adelledgar.ariseup.ui.theme.SystemColors
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

@Composable
fun MissionsRoute(container: AppContainer, onManageRoutines: () -> Unit) {
    val vm = containerViewModel {
        MissionsViewModel(container.db, container.missions, container.routines, container.body, container.reevaluation, container.gameClock)
    }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle(Settings())
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = Instant.now()
        }
    }
    val s = ui ?: return
    val accent = LocalSystemPalette.current.accent

    ScreenColumn {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("[Misiones]", style = MaterialTheme.typography.headlineMedium, color = accent, modifier = Modifier.weight(1f))
                IconButton(onClick = onManageRoutines) { Icon(Icons.Default.Tune, contentDescription = "Gestionar rutinas", tint = accent) }
            }
        }
        s.penalty?.let { p ->
            item {
                SystemWindow("Misión de penalización: supervivencia", accent = SystemColors.danger) {
                    Text("Completa este entreno para salir de la Zona de Penalización.", color = SystemColors.text)
                    Spacer(Modifier.height(8.dp))
                    DailyQuestLogger(targetsOf(p), progressOf(p), p.completedAt != null) { e, a, m -> vm.addDaily(p.id, e, a, m) }
                }
            }
        }
        val list = s.missions.filter { it.kind != MissionKind.URGENT || (it.appearAt ?: Instant.MIN) <= now }
        if (list.isEmpty()) item {
            SystemWindow("Sin misiones") { Text("Completa el test de evaluación para recibir la misión diaria.", color = SystemColors.text) }
        }
        list.forEach { m ->
            item(key = m.id) { MissionCard(m, s, vm, settings, now) }
        }
    }
    SystemMessageHost(vm.messages)
}

private fun targetsOf(m: MissionEntity) = AppJson.decodeFromString(DailyTargets.serializer(), m.target!!)
private fun progressOf(m: MissionEntity) = AppJson.decodeFromString(DailyProgress.serializer(), m.progress!!)

@Composable
private fun MissionCard(m: MissionEntity, s: MissionsUi, vm: MissionsViewModel, settings: Settings, now: Instant) {
    val done = m.completedAt != null
    val title = (if (done) "✓ " else "") + m.title + if (!m.mandatory && m.kind in setOf(MissionKind.DAILY, MissionKind.ROUTINE, MissionKind.NUTRITION, MissionKind.DIET_RULE)) " (opcional)" else ""
    val accent = when {
        done -> SystemColors.success
        m.kind == MissionKind.URGENT -> SystemColors.gold
        else -> LocalSystemPalette.current.accent
    }
    SystemWindow(title, accent = accent) {
        when (m.kind) {
            MissionKind.DAILY -> DailyQuestLogger(targetsOf(m), progressOf(m), done) { e, a, min -> vm.addDaily(m.id, e, a, min) }
            MissionKind.URGENT -> UrgentBody(m, vm, now)
            MissionKind.ROUTINE -> {
                val routine = s.routines[m.refId]
                if (routine == null) {
                    Text("La rutina ya no existe.", color = SystemColors.muted)
                } else {
                    val sets by vm.sets(m.id).collectAsStateWithLifecycle(emptyList())
                    RoutineSession(routine, sets, done, settings.restTimerSeconds, settings.vibration) { re, i, r, w, sec, met -> vm.logSet(m.id, re, i, r, w, sec, met) }
                }
            }
            MissionKind.NUTRITION -> Text(
                if (done) "Objetivos de nutrición cumplidos." else "Calorías en ±10 %, proteína ≥90 % y al menos 3 comidas registradas. Se evalúa a las 12:00 del día siguiente.",
                color = SystemColors.text,
            )
            MissionKind.DIET_RULE -> Text("Márcala en la pestaña Nutrición. Se evalúa con la dieta del día.", color = SystemColors.text)
            MissionKind.WEIGH_IN -> if (done) Text("Peso registrado.", color = SystemColors.text) else WeighInBody { w -> vm.weighIn(m.id, w, null, null) }
            MissionKind.REEVALUATION -> if (done) Text("Reevaluación completada.", color = SystemColors.text) else ReevaluationBody { vm.reevaluate(m.id, it.toTest(s.today)) }
            MissionKind.PENALTY -> Unit
        }
    }
}

@Composable
private fun UrgentBody(m: MissionEntity, vm: MissionsViewModel, now: Instant) {
    val t = AppJson.decodeFromString(UrgentTarget.serializer(), m.target!!)
    val p = AppJson.decodeFromString(UrgentProgress.serializer(), m.progress!!)
    val left = m.deadline?.let { Duration.between(now, it) } ?: Duration.ZERO
    val expired = left.isNegative || left.isZero
    val unit = if (t.exercise == DailyExercise.RUN) "km" else "reps"
    Text(
        if (expired) "[Tiempo agotado]" else "Tiempo restante: %d:%02d:%02d".format(left.seconds / 3600, left.seconds % 3600 / 60, left.seconds % 60),
        color = if (expired) SystemColors.danger else SystemColors.gold,
        style = MaterialTheme.typography.titleMedium,
    )
    StatBar(t.exercise.label, p.done.toFloat(), t.amount.toFloat(), valueText = "${fmtKm(p.done)}/${fmtKm(t.amount)} $unit")
    if (m.completedAt == null && !expired) {
        if (t.exercise == DailyExercise.RUN) RunInput { km, _ -> vm.addDaily(m.id, t.exercise, km) } else RepButtons { vm.addDaily(m.id, t.exercise, it.toDouble()) }
    }
}

@Composable
private fun WeighInBody(onSave: (Double) -> Unit) {
    var w by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        SystemTextField(w, { w = it }, "Peso", numeric = true, suffix = "kg", modifier = Modifier.weight(1f))
        SystemButton("Registrar", { w.toDecimalOrNull()?.takeIf { it in 30.0..300.0 }?.let(onSave) })
    }
}

@Composable
private fun ReevaluationBody(onSave: (FitnessTestDraft) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Text("Repite el test para recalibrar tus stats y tu misión diaria.", color = SystemColors.text)
    Spacer(Modifier.height(8.dp))
    SystemButton("Hacer la reevaluación", { open = true }, Modifier.fillMaxWidth())
    if (open) {
        val draft = rememberFitnessTestDraft()
        Dialog(onDismissRequest = { open = false }) {
            androidx.compose.foundation.lazy.LazyColumn {
                item {
                    SystemWindow("Reevaluación del Jugador", onClose = { open = false }) {
                        FitnessTestForm(draft)
                        Spacer(Modifier.height(8.dp))
                        SystemButton("Enviar al Sistema", { onSave(draft); open = false }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
