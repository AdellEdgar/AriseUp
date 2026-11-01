package io.github.adelledgar.ariseup.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.adelledgar.ariseup.di.AppContainer
import io.github.adelledgar.ariseup.domain.model.ActivityLevel
import io.github.adelledgar.ariseup.domain.model.Goal
import io.github.adelledgar.ariseup.domain.model.Profile
import io.github.adelledgar.ariseup.domain.model.Sex
import io.github.adelledgar.ariseup.ui.ScreenColumn
import io.github.adelledgar.ariseup.ui.components.ChoiceChips
import io.github.adelledgar.ariseup.ui.components.SystemButton
import io.github.adelledgar.ariseup.ui.components.SystemSounds
import io.github.adelledgar.ariseup.ui.components.SystemTextField
import io.github.adelledgar.ariseup.ui.components.SystemWindow
import io.github.adelledgar.ariseup.ui.components.TypewriterText
import io.github.adelledgar.ariseup.ui.components.toDecimalOrNull
import io.github.adelledgar.ariseup.ui.containerViewModel
import io.github.adelledgar.ariseup.ui.missions.FitnessTestForm
import io.github.adelledgar.ariseup.ui.missions.rememberFitnessTestDraft
import io.github.adelledgar.ariseup.ui.theme.SystemColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private enum class Step { MEDICAL, ACCEPT, PROFILE, TEST }

/** Primer arranque (spec §4.2). Al terminar, la raíz detecta el Jugador y muestra las pestañas. */
@Composable
fun OnboardingRoute(container: AppContainer) {
    val vm = containerViewModel { OnboardingViewModel(container.db, container.body, container.planner, container.gameClock) }
    var step by rememberSaveable { mutableStateOf(Step.MEDICAL) }
    var refusals by rememberSaveable { mutableStateOf(0) }

    var name by rememberSaveable { mutableStateOf("") }
    var sex by rememberSaveable { mutableStateOf(Sex.MALE) }
    var birth by rememberSaveable { mutableStateOf<String?>(null) }
    var height by rememberSaveable { mutableStateOf("") }
    var activity by rememberSaveable { mutableStateOf(ActivityLevel.MODERATE) }
    var goal by rememberSaveable { mutableStateOf(Goal.MAINTAIN) }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }
    val draft = rememberFitnessTestDraft()
    val scope = rememberCoroutineScope()

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    ScreenColumn {
        when (step) {
            Step.MEDICAL -> item {
                SystemWindow("Aviso") {
                    TypewriterText("AriseUp no es consejo médico. Consulta a un profesional antes de empezar un plan de ejercicio o dieta.")
                    Spacer(Modifier.height(16.dp))
                    SystemButton("Entendido", { step = Step.ACCEPT }, Modifier.fillMaxWidth())
                }
            }
            Step.ACCEPT -> item {
                SystemWindow("Notificación") {
                    TypewriterText(
                        if (refusals == 0) "Has cumplido los requisitos para convertirte en Jugador. ¿Aceptas?"
                        else "Has cumplido los requisitos para convertirte en Jugador. ¿Aceptas? (×$refusals)",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SystemButton("Sí", {
                            SystemSounds.ding()
                            step = Step.PROFILE
                        }, Modifier.weight(1f))
                        SystemButton("No", { refusals++ }, Modifier.weight(1f), color = SystemColors.muted)
                    }
                }
            }
            Step.PROFILE -> item {
                SystemWindow("Registro del Jugador") {
                    SystemTextField(name, { name = it }, "Nombre de Jugador", error = errors["name"])
                    Text("Sexo", color = SystemColors.muted)
                    ChoiceChips(Sex.entries, sex, { it.label }, { sex = it })
                    BirthDateField(birth, errors["birthDate"]) { birth = it }
                    SystemTextField(height, { height = it }, "Altura", numeric = true, suffix = "cm", error = errors["height"])
                    Text("Nivel de actividad", color = SystemColors.muted)
                    ChoiceChips(ActivityLevel.entries, activity, { it.label }, { activity = it })
                    Text("Objetivo", color = SystemColors.muted)
                    ChoiceChips(Goal.entries, goal, { it.label }, { goal = it })
                    Spacer(Modifier.height(12.dp))
                    SystemButton("Continuar", {
                        val today = LocalDate.now()
                        errors = validateProfile(height.toDecimalOrNull(), 70.0, birth?.let(LocalDate::parse), today, name)
                        if (errors.isEmpty()) step = Step.TEST
                    }, Modifier.fillMaxWidth())
                }
            }
            Step.TEST -> item {
                SystemWindow("Test de evaluación inicial") {
                    Text(
                        "Con estos datos el Sistema calibra tu misión diaria. Puedes dejar huecos y completarlos otro día.",
                        color = SystemColors.muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    FitnessTestForm(draft, weightError = errors["weight"])
                    Spacer(Modifier.height(12.dp))
                    SystemButton("Iniciar el Sistema", {
                        scope.launch {
                            val profile = Profile(name, sex, LocalDate.parse(birth!!), height.toDecimalOrNull()!!, activity, goal)
                            when (val r = vm.finish(profile, draft.toTest(LocalDate.now()))) {
                                is OnboardingResult.Invalid -> errors = r.errors
                                is OnboardingResult.Done -> {
                                    SystemSounds.levelUp()
                                    if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        }
                    }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDateField(value: String?, error: String?, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = value?.let { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) } ?: "Elegir"
    Row(Modifier.fillMaxWidth()) {
        Text("Fecha de nacimiento", color = SystemColors.muted, modifier = Modifier.weight(1f).align(androidx.compose.ui.Alignment.CenterVertically))
        TextButton(onClick = { open = true }) { Text(label) }
    }
    if (error != null) Text(error, color = SystemColors.danger, style = MaterialTheme.typography.bodySmall)
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (value?.let(LocalDate::parse) ?: LocalDate.now().minusYears(25))
                .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                    open = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
        ) { DatePicker(state) }
    }
}
