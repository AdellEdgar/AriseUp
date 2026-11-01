package io.github.adelledgar.ariseup.ui.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import io.github.adelledgar.ariseup.domain.model.FitnessTest
import io.github.adelledgar.ariseup.ui.components.SystemTextField
import io.github.adelledgar.ariseup.ui.components.toDecimalOrNull
import io.github.adelledgar.ariseup.ui.theme.SystemColors
import java.time.LocalDate

/** Borrador editable del test de evaluación. */
class FitnessTestDraft {
    var pushUps by mutableStateOf("")
    var sitUps by mutableStateOf("")
    var squats by mutableStateOf("")
    var cooper by mutableStateOf("")
    var reach by mutableStateOf("")
    var weight by mutableStateOf("")
    var waist by mutableStateOf("")
    var fat by mutableStateOf("")

    fun toTest(date: LocalDate) = FitnessTest(
        date = date,
        pushUps = pushUps.toDecimalOrNull()?.toInt(),
        sitUps = sitUps.toDecimalOrNull()?.toInt(),
        squats = squats.toDecimalOrNull()?.toInt(),
        cooperMeters = cooper.toDecimalOrNull()?.toInt(),
        sitAndReachCm = reach.toDecimalOrNull(),
        weightKg = weight.toDecimalOrNull(),
        waistCm = waist.toDecimalOrNull(),
        bodyFatPct = fat.toDecimalOrNull(),
    )
}

@Composable
fun rememberFitnessTestDraft(): FitnessTestDraft = rememberSaveable(saver = androidx.compose.runtime.saveable.Saver(
    save = { listOf(it.pushUps, it.sitUps, it.squats, it.cooper, it.reach, it.weight, it.waist, it.fat) },
    restore = { l -> FitnessTestDraft().apply { pushUps = l[0]; sitUps = l[1]; squats = l[2]; cooper = l[3]; reach = l[4]; weight = l[5]; waist = l[6]; fat = l[7] } },
)) { FitnessTestDraft() }

/** Formulario del test inicial y de la reevaluación (spec §2.7). Todo es opcional: se puede rellenar por partes. */
@Composable
fun FitnessTestForm(draft: FitnessTestDraft, weightError: String? = null, showBody: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Haz cada ejercicio hasta el fallo, con buena técnica.", color = SystemColors.muted, style = MaterialTheme.typography.bodySmall)
        SystemTextField(draft.pushUps, { draft.pushUps = it }, "Máximo de flexiones", numeric = true, suffix = "reps")
        SystemTextField(draft.sitUps, { draft.sitUps = it }, "Máximo de abdominales", numeric = true, suffix = "reps")
        SystemTextField(draft.squats, { draft.squats = it }, "Máximo de sentadillas", numeric = true, suffix = "reps")
        SystemTextField(draft.cooper, { draft.cooper = it }, "Test de Cooper (distancia en 12 min)", numeric = true, suffix = "m")
        SystemTextField(draft.reach, { draft.reach = it }, "Flexibilidad sentado (sit-and-reach)", numeric = true, suffix = "cm")
        if (showBody) {
            SystemTextField(draft.weight, { draft.weight = it }, "Peso", numeric = true, suffix = "kg", error = weightError)
            SystemTextField(draft.waist, { draft.waist = it }, "Cintura (opcional)", numeric = true, suffix = "cm")
            SystemTextField(draft.fat, { draft.fat = it }, "% de grasa (opcional)", numeric = true, suffix = "%")
        }
    }
}
