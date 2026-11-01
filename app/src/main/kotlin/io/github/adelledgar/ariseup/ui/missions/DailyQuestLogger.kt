package io.github.adelledgar.ariseup.ui.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.adelledgar.ariseup.data.db.DailyProgress
import io.github.adelledgar.ariseup.data.db.of
import io.github.adelledgar.ariseup.domain.model.DailyExercise
import io.github.adelledgar.ariseup.domain.model.DailyTargets
import io.github.adelledgar.ariseup.ui.components.StatBar
import io.github.adelledgar.ariseup.ui.components.SystemButton
import io.github.adelledgar.ariseup.ui.components.SystemTextField
import io.github.adelledgar.ariseup.ui.components.toDecimalOrNull
import io.github.adelledgar.ariseup.ui.theme.SystemColors
import java.util.Locale

fun fmtKm(v: Double) = "%.1f".format(Locale("es"), v)

/** Registro de la misión diaria (o de penalización): cuatro filas con su progreso. */
@Composable
fun DailyQuestLogger(targets: DailyTargets, progress: DailyProgress, completed: Boolean, onAdd: (DailyExercise, Double, Double) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DailyExercise.entries.forEach { e ->
            val done = progress.of(e)
            val target = targets.of(e)
            val text = if (e == DailyExercise.RUN) "${fmtKm(done)}/${fmtKm(target)} km" else "${done.toInt()}/${target.toInt()}"
            StatBar(e.label, done.toFloat(), target.toFloat(), valueText = text, color = if (done >= target) SystemColors.success else SystemColors.accent)
            if (!completed && done < target) {
                if (e == DailyExercise.RUN) RunInput { km, min -> onAdd(e, km, min) } else RepButtons { onAdd(e, it.toDouble(), 0.0) }
            }
        }
    }
}

@Composable
fun RepButtons(onAdd: (Int) -> Unit) {
    var custom by remember { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(1, 5, 10).forEach { n -> SystemButton("+$n", { onAdd(n) }) }
        SystemTextField(custom, { custom = it }, "Otra", numeric = true, modifier = Modifier.width(90.dp))
        SystemButton("OK", {
            custom.toDecimalOrNull()?.toInt()?.takeIf { it != 0 }?.let(onAdd)
            custom = ""
        })
    }
}

@Composable
fun RunInput(onAdd: (Double, Double) -> Unit) {
    var km by remember { mutableStateOf("") }
    var min by remember { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        SystemTextField(km, { km = it }, "km", numeric = true, modifier = Modifier.weight(1f))
        SystemTextField(min, { min = it }, "minutos", numeric = true, modifier = Modifier.weight(1f))
        SystemButton("Añadir", {
            km.toDecimalOrNull()?.takeIf { it > 0 }?.let { onAdd(it, min.toDecimalOrNull() ?: 0.0) }
            km = ""
            min = ""
        })
    }
    Spacer(Modifier.height(2.dp))
    Text("Las carreras de Health Connect se suman solas.", color = SystemColors.muted, style = MaterialTheme.typography.bodySmall)
}
