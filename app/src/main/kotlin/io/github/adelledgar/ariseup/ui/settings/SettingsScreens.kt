package io.github.adelledgar.ariseup.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.adelledgar.ariseup.BuildConfig
import io.github.adelledgar.ariseup.data.backup.ImportResult
import io.github.adelledgar.ariseup.data.prefs.NutritionMode
import io.github.adelledgar.ariseup.data.prefs.Settings
import io.github.adelledgar.ariseup.data.prefs.WeighInFrequency
import io.github.adelledgar.ariseup.di.AppContainer
import io.github.adelledgar.ariseup.domain.Nutrition
import io.github.adelledgar.ariseup.domain.model.ActivityLevel
import io.github.adelledgar.ariseup.domain.model.Goal
import io.github.adelledgar.ariseup.domain.model.RestReason
import io.github.adelledgar.ariseup.domain.model.RestValidation
import io.github.adelledgar.ariseup.domain.model.TargetOverrides
import io.github.adelledgar.ariseup.ui.ScreenColumn
import io.github.adelledgar.ariseup.ui.components.ChoiceChips
import io.github.adelledgar.ariseup.ui.components.InfoRow
import io.github.adelledgar.ariseup.ui.components.SystemButton
import io.github.adelledgar.ariseup.ui.components.SystemSounds
import io.github.adelledgar.ariseup.ui.components.SystemTextField
import io.github.adelledgar.ariseup.ui.components.SystemWindow
import io.github.adelledgar.ariseup.ui.components.toDecimalOrNull
import io.github.adelledgar.ariseup.ui.theme.SystemColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private fun hm(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

@Composable
fun SettingsScreen(container: AppContainer, onRest: () -> Unit, onBack: () -> Unit) {
    val settings by container.settings.settings.collectAsStateWithLifecycle(Settings())
    val player by container.players.entity().collectAsStateWithLifecycle(null)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    fun update(t: (Settings) -> Settings) = scope.launch {
        container.settings.update(t)
        SystemSounds.enabled = container.settings.current().sound
        container.scheduler.scheduleAll()
    }

    var hcAvailable by remember { mutableStateOf(false) }
    var hcGranted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hcAvailable = container.health.sdkAvailable()
        hcGranted = container.health.available()
    }
    val hcLauncher = rememberLauncherForActivityResult(container.health.permissionContract()) { granted ->
        hcGranted = granted.containsAll(container.health.permissions)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            context.contentResolver.openOutputStream(uri)?.use { container.backup.export(it) }
            Toast.makeText(context, "Copia guardada", Toast.LENGTH_SHORT).show()
        }
    }
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { pendingImport = it }

    ScreenColumn {
        item { SystemWindow("Ajustes", onClose = onBack) { Text("Los cambios se guardan al momento.", color = SystemColors.muted) } }
        player?.let { p ->
            item {
                SystemWindow("Perfil") {
                    var name by remember(p.name) { mutableStateOf(p.name) }
                    var height by remember(p.heightCm) { mutableStateOf(p.heightCm.toString()) }
                    SystemTextField(name, { name = it }, "Nombre de Jugador")
                    SystemTextField(height, { height = it }, "Altura", numeric = true, suffix = "cm")
                    Text("Actividad", color = SystemColors.muted)
                    ChoiceChips(ActivityLevel.entries, p.activity, { it.label }, { a -> scope.launch { container.players.saveEntity(p.copy(activity = a)) } })
                    Text("Objetivo", color = SystemColors.muted)
                    ChoiceChips(Goal.entries, p.goal, { it.label }, { g -> scope.launch { container.players.saveEntity(p.copy(goal = g)) } })
                    SystemButton("Guardar nombre y altura", {
                        val h = height.toDecimalOrNull()
                        if (name.isNotBlank() && h != null && h in 120.0..230.0) scope.launch { container.players.saveEntity(p.copy(name = name.trim(), heightCm = h)) }
                    }, Modifier.fillMaxWidth())
                }
            }
            item {
                SystemWindow("Objetivos de nutrición") {
                    var auto by remember { mutableStateOf<io.github.adelledgar.ariseup.domain.model.NutritionTargets?>(null) }
                    LaunchedEffect(p, settings) {
                        auto = Nutrition.targets(p.profile(), container.body.latest()?.weightKg ?: 70.0, LocalDate.now(), TargetOverrides.NONE)
                    }
                    val o = settings.targetOverrides
                    var kcal by remember(o) { mutableStateOf(o.kcal?.toString() ?: "") }
                    var prot by remember(o) { mutableStateOf(o.proteinG?.toString() ?: "") }
                    var fat by remember(o) { mutableStateOf(o.fatG?.toString() ?: "") }
                    var carbs by remember(o) { mutableStateOf(o.carbsG?.toString() ?: "") }
                    auto?.let { Text("Calculados: ${it.kcal} kcal · P ${it.proteinG} g · G ${it.fatG} g · H ${it.carbsG} g", color = SystemColors.muted) }
                    Text("Deja un campo vacío para que se calcule solo.", color = SystemColors.muted, style = MaterialTheme.typography.bodySmall)
                    SystemTextField(kcal, { kcal = it }, "Calorías", numeric = true, suffix = "kcal")
                    SystemTextField(prot, { prot = it }, "Proteína", numeric = true, suffix = "g")
                    SystemTextField(fat, { fat = it }, "Grasa", numeric = true, suffix = "g")
                    SystemTextField(carbs, { carbs = it }, "Hidratos", numeric = true, suffix = "g")
                    Row {
                        SystemButton("Guardar", {
                            update { s -> s.copy(targetOverrides = TargetOverrides(kcal.toDecimalOrNull()?.toInt(), prot.toDecimalOrNull()?.toInt(), fat.toDecimalOrNull()?.toInt(), carbs.toDecimalOrNull()?.toInt())) }
                        }, Modifier.weight(1f))
                        SystemButton("Volver a calcular", { update { it.copy(targetOverrides = TargetOverrides.NONE) } }, Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            SystemWindow("Sistema") {
                TimeRow("Hora de reinicio diario", settings.resetMinutes) { m -> update { it.copy(resetMinutes = m) } }
                Text("Cambiarla durante el día puede alargar o acortar el día en curso.", color = SystemColors.muted, style = MaterialTheme.typography.bodySmall)
                Text("Misión de nutrición", color = SystemColors.muted)
                ChoiceChips(NutritionMode.entries, settings.nutritionMode, { it.label }, { v -> update { it.copy(nutritionMode = v) } })
                Text("Recordatorio de pesaje", color = SystemColors.muted)
                ChoiceChips(WeighInFrequency.entries, settings.weighInFrequency, { it.label }, { v -> update { it.copy(weighInFrequency = v) } })
                TimeRow("Hora del pesaje", settings.weighInMinutes) { m -> update { it.copy(weighInMinutes = m) } }
                Text("Descanso entre series", color = SystemColors.muted)
                ChoiceChips(listOf(30, 60, 90, 120, 180), settings.restTimerSeconds, { "${it}s" }, { v -> update { it.copy(restTimerSeconds = v) } })
                ToggleRow("Sonido", settings.sound) { v -> update { it.copy(sound = v) } }
                ToggleRow("Vibración", settings.vibration) { v -> update { it.copy(vibration = v) } }
            }
        }
        item {
            SystemWindow("Estado de descanso") {
                Text("Vacaciones, enfermedad o lesión: sin misiones obligatorias ni penalizaciones.", color = SystemColors.text)
                SystemButton("Gestionar descanso", onRest, Modifier.fillMaxWidth())
            }
        }
        item {
            SystemWindow("Health Connect") {
                when {
                    !hcAvailable -> Text("Health Connect no está disponible en este dispositivo. Puedes registrar las carreras a mano.", color = SystemColors.muted)
                    hcGranted -> Text("Conectado: tus carreras se importan solas a la misión diaria.", color = SystemColors.success)
                    else -> {
                        Text("Importa las carreras de tu reloj o de otras apps (solo lectura de ejercicio y distancia).", color = SystemColors.text)
                        SystemButton("Conceder permisos", { hcLauncher.launch(container.health.permissions) }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        item {
            SystemWindow("Copia de seguridad") {
                Text("Tus datos solo están en este móvil. Guarda una copia de vez en cuando.", color = SystemColors.text)
                Row {
                    SystemButton("Exportar", { exportLauncher.launch("ariseup-${LocalDate.now()}.json") }, Modifier.weight(1f))
                    SystemButton("Importar", { importLauncher.launch(arrayOf("application/json", "*/*")) }, Modifier.weight(1f))
                }
            }
        }
        item { AboutWindow() }
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("[Importar copia]") },
            text = { Text("[Esto sustituirá todos tus datos. ¿Continuar?]") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        val r = context.contentResolver.openInputStream(uri)?.use { container.backup.import(it) } ?: ImportResult.InvalidFile
                        val msg = when (r) {
                            ImportResult.Ok -> "Copia importada"
                            ImportResult.InvalidFile -> "Archivo no válido: no se ha cambiado nada"
                            is ImportResult.NewerSchema -> "La copia es de una versión más nueva de AriseUp"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }) { Text("Sustituir", color = SystemColors.danger) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun AboutWindow() {
    val uri = LocalUriHandler.current
    SystemWindow("Acerca de") {
        InfoRow("Versión", BuildConfig.VERSION_NAME)
        InfoRow("Licencia", "MIT")
        Text(
            "Proyecto de fan sin afiliación con Solo Leveling ni sus titulares. No es consejo médico. " +
                "Datos de alimentos: Open Food Facts (ODbL) y USDA FoodData Central (dominio público).",
            color = SystemColors.muted, style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = { uri.openUri("https://github.com/AdellEdgar/AriseUp") }) { Text("github.com/AdellEdgar/AriseUp") }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SystemColors.text, modifier = Modifier.weight(1f))
        Switch(value, onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRow(label: String, minutes: Int, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SystemColors.text, modifier = Modifier.weight(1f))
        TextButton(onClick = { open = true }) { Text(hm(minutes)) }
    }
    if (open) {
        val state = rememberTimePickerState(minutes / 60, minutes % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { onPick(state.hour * 60 + state.minute); open = false }) { Text("Aceptar") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
            text = { TimePicker(state) },
        )
    }
}

private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun RestModeScreen(container: AppContainer, onBack: () -> Unit) {
    val periods by container.rest.observe().collectAsStateWithLifecycle(emptyList())
    var usage by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf(RestReason.VACATION) }
    var days by remember { mutableStateOf("7") }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var today by remember { mutableStateOf<LocalDate?>(null) }
    LaunchedEffect(periods) {
        usage = container.rest.vacationUsage()
        today = container.gameClock.today()
    }
    val t = today ?: return
    val active = periods.map { it to it.toDomain() }.filter { (_, d) -> d.covers(t) || d.start > t }

    ScreenColumn {
        item {
            SystemWindow("Estado de descanso", onClose = onBack) {
                InfoRow("Vacaciones usadas este año", usage)
                active.forEach { (e, d) ->
                    Spacer(Modifier.height(8.dp))
                    Text("${d.reason.label}: desde ${d.start.format(dateFmt)}" + (d.effectiveEnd?.let { " hasta ${it.format(dateFmt)}" } ?: " (sin fecha de fin)"), color = SystemColors.text)
                    Row {
                        if (d.reason != RestReason.VACATION) TextButton(onClick = { scope.launch { container.rest.confirm(e.id) } }) { Text("Confirmar que sigo") }
                        TextButton(onClick = { scope.launch { container.rest.endNow(e.id) } }) { Text("Terminar hoy", color = SystemColors.danger) }
                    }
                }
                if (active.isEmpty()) Text("No hay ningún descanso activo.", color = SystemColors.muted)
            }
        }
        item {
            SystemWindow("Iniciar descanso (desde hoy)") {
                ChoiceChips(RestReason.entries, reason, { it.label }, { reason = it })
                SystemTextField(
                    days, { days = it },
                    if (reason == RestReason.VACATION) "Días (incluido hoy)" else "Días (vacío = hasta que lo termines)",
                    numeric = true,
                )
                message?.let { Text(it, color = if (it.startsWith("[")) SystemColors.danger else SystemColors.success) }
                SystemButton("Activar", {
                    val n = days.toDecimalOrNull()?.toInt()?.takeIf { it > 0 }
                    scope.launch {
                        val end = n?.let { t.plusDays(it - 1L) }
                        message = when (val r = container.rest.start(reason, t, end)) {
                            RestValidation.Ok -> "Estado de descanso activado."
                            is RestValidation.Rejected -> r.reason
                        }
                    }
                }, Modifier.fillMaxWidth())
                Text(
                    "No se puede activar con efecto retroactivo ni durante la Zona de Penalización. Por enfermedad o lesión, el Sistema te pedirá confirmarlo cada 7 días.",
                    color = SystemColors.muted, style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
