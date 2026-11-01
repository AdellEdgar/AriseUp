package io.github.adelledgar.ariseup.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.adelledgar.ariseup.di.AppContainer
import io.github.adelledgar.ariseup.domain.model.Stat
import io.github.adelledgar.ariseup.ui.ScreenColumn
import io.github.adelledgar.ariseup.ui.components.ChoiceChips
import io.github.adelledgar.ariseup.ui.components.SystemWindow
import io.github.adelledgar.ariseup.ui.containerViewModel
import io.github.adelledgar.ariseup.ui.theme.LocalSystemPalette
import io.github.adelledgar.ariseup.ui.theme.SystemColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFmt = DateTimeFormatter.ofPattern("d MMM", Locale("es"))
private val timeFmt = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale("es"))

@Composable
fun ProgressRoute(container: AppContainer) {
    val vm = containerViewModel { ProgressViewModel(container.db, container.body, container.gameClock) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val s = ui ?: return
    val accent = LocalSystemPalette.current.accent
    var stat by remember { mutableStateOf(Stat.FUE) }
    var testMetric by remember { mutableStateOf(0) }

    ScreenColumn {
        item { Text("[Progreso]", style = MaterialTheme.typography.headlineMedium, color = accent) }
        item {
            SystemWindow("Peso") {
                if (s.weight.size < 2) {
                    Text("Registra tu peso al menos dos días para ver la gráfica.", color = SystemColors.muted)
                } else {
                    LineChart(s.weight.map { it.second }, accent, secondary = s.weightAvg)
                    Row {
                        Text(s.weight.first().first.format(dayFmt), color = SystemColors.muted, modifier = Modifier.weight(1f))
                        Text("${s.weight.last().second} kg", color = SystemColors.text)
                    }
                    Text("Línea discontinua: media de 7 registros.", color = SystemColors.muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            SystemWindow("Atributos") {
                ChoiceChips(Stat.entries, stat, { it.name }, { stat = it })
                val points = s.stats.getValue(stat)
                if (points.size < 2) Text("Aún no hay suficiente historial.", color = SystemColors.muted)
                else LineChart(points.map { it.second }, accent)
            }
        }
        item {
            SystemWindow("Marcas de los tests") {
                val metrics = listOf<Pair<String, (io.github.adelledgar.ariseup.data.db.FitnessTestEntity) -> Double?>>(
                    "Flexiones" to { it.pushUps?.toDouble() },
                    "Sentadillas" to { it.squats?.toDouble() },
                    "Abdominales" to { it.sitUps?.toDouble() },
                    "Cooper (m)" to { it.cooperMeters?.toDouble() },
                    "Flexibilidad (cm)" to { it.sitAndReachCm },
                )
                ChoiceChips(metrics.indices.toList(), testMetric, { metrics[it].first }, { testMetric = it })
                val values = s.tests.mapNotNull { metrics[testMetric].second(it) }
                when {
                    values.isEmpty() -> Text("Sin datos.", color = SystemColors.muted)
                    values.size == 1 -> Text("Primera marca: ${values.single().toInt()}", color = SystemColors.text)
                    else -> LineChart(values, SystemColors.gold)
                }
            }
        }
        item {
            SystemWindow("Historial") {
                Calendar(s.calendar)
                Legend()
            }
        }
        item {
            SystemWindow("Misiones ocultas") {
                s.achievements.forEach { a ->
                    Text("✦ ${a.achievement.label}" + (a.achievement.title?.let { " — título «$it»" } ?: ""), color = SystemColors.gold)
                    Text(a.at.atZone(ZoneId.systemDefault()).format(timeFmt), color = SystemColors.muted, style = MaterialTheme.typography.bodySmall)
                }
                repeat(minOf(s.hiddenLeft, 3)) { Text("[???]", color = SystemColors.muted) }
                if (s.hiddenLeft > 0) Text("${s.hiddenLeft} misiones ocultas por descubrir", color = SystemColors.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            SystemWindow("Registro del Sistema") {
                if (s.log.isEmpty()) Text("Todavía no hay eventos.", color = SystemColors.muted)
                s.log.take(60).forEach { e ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text(e.at.atZone(ZoneId.systemDefault()).format(timeFmt), color = SystemColors.muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.35f))
                        Text(e.text, color = SystemColors.text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.65f))
                    }
                }
            }
        }
    }
}

/** Gráfica de líneas sencilla con el estilo del Sistema. */
@Composable
fun LineChart(values: List<Double>, color: Color, secondary: List<Double>? = null) {
    Canvas(Modifier.fillMaxWidth().height(150.dp).padding(vertical = 8.dp)) {
        val all = values + secondary.orEmpty()
        val min = all.min()
        val max = all.max().let { if (it == min) min + 1 else it }
        fun pt(i: Int, v: Double, n: Int) = Offset(
            if (n == 1) size.width / 2 else size.width * i / (n - 1),
            (size.height * (1 - (v - min) / (max - min))).toFloat(),
        )
        repeat(4) { k ->
            val y = size.height * k / 3
            drawLine(color.copy(alpha = 0.12f), Offset(0f, y), Offset(size.width, y))
        }
        fun path(vs: List<Double>) = Path().apply { vs.forEachIndexed { i, v -> pt(i, v, vs.size).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } } }
        secondary?.let { drawPath(path(it), SystemColors.muted, style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))) }
        drawPath(path(values), color, style = Stroke(4f))
        values.forEachIndexed { i, v -> drawCircle(color, 5f, pt(i, v, values.size)) }
    }
}

private fun markColor(m: DayMark) = when (m) {
    DayMark.DONE -> SystemColors.accent
    DayMark.FAILED -> SystemColors.danger
    DayMark.PENALTY -> SystemColors.danger.copy(alpha = 0.5f)
    DayMark.REST -> SystemColors.muted
    DayMark.TODAY -> SystemColors.gold
    DayMark.OPEN -> Color(0x33FFFFFF)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Calendar(days: List<CalendarDay>) {
    if (days.isEmpty()) {
        Text("Sin días registrados todavía.", color = SystemColors.muted)
        return
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        days.takeLast(70).forEach { d ->
            Box(
                Modifier.size(30.dp).background(markColor(d.mark).copy(alpha = 0.25f)).border(1.dp, markColor(d.mark)),
                contentAlignment = Alignment.Center,
            ) { Text("${d.date.dayOfMonth}", color = SystemColors.text, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center) }
        }
    }
    Text("Desde ${days.first().date.format(dayFmt)}", color = SystemColors.muted, style = MaterialTheme.typography.bodySmall)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend() {
    FlowRow(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(DayMark.DONE to "Cumplido", DayMark.FAILED to "Fallado", DayMark.PENALTY to "Penalización", DayMark.REST to "Descanso", DayMark.TODAY to "Hoy")
            .forEach { (m, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(markColor(m)))
                    Text(" $label", color = SystemColors.muted, style = MaterialTheme.typography.bodySmall)
                }
            }
    }
}
