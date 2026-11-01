package io.github.adelledgar.ariseup.ui.status

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.adelledgar.ariseup.di.AppContainer
import io.github.adelledgar.ariseup.domain.model.Stat
import io.github.adelledgar.ariseup.ui.ScreenColumn
import io.github.adelledgar.ariseup.ui.components.AvatarMode
import io.github.adelledgar.ariseup.ui.components.InfoRow
import io.github.adelledgar.ariseup.ui.components.ShadowAvatar
import io.github.adelledgar.ariseup.ui.components.StatBar
import io.github.adelledgar.ariseup.ui.components.SystemWindow
import io.github.adelledgar.ariseup.ui.containerViewModel
import io.github.adelledgar.ariseup.ui.theme.LocalSystemPalette
import io.github.adelledgar.ariseup.ui.theme.SystemColors
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.util.Locale

@Composable
fun StatusRoute(container: AppContainer, onOpenSettings: () -> Unit) {
    val vm = containerViewModel { StatusViewModel(container.db, container.players, container.body, container.gameClock) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val s = ui ?: return
    var detail by remember { mutableStateOf(false) }
    val accent = LocalSystemPalette.current.accent

    ScreenColumn {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("[Estado]", style = MaterialTheme.typography.headlineMedium, color = accent, modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = accent) }
            }
        }
        if (s.penalty) item { PenaltyBanner(s.penaltyDeadline) }
        if (s.rest) item {
            SystemWindow("Estado de descanso", accent = SystemColors.muted) {
                Text("No hay misiones obligatorias ni penalizaciones. La racha está congelada.", color = SystemColors.text)
            }
        }
        item {
            Box(Modifier.fillMaxWidth().height(330.dp).clickable { detail = true }) {
                ShadowAvatar(
                    shape = s.shape,
                    mode = when {
                        s.penalty -> AvatarMode.PENALTY
                        s.rest -> AvatarMode.REST
                        else -> AvatarMode.NORMAL
                    },
                    rank = s.rank,
                    modifier = Modifier.fillMaxWidth().height(330.dp),
                )
            }
        }
        item {
            SystemWindow("Jugador") {
                Text(s.name, style = MaterialTheme.typography.headlineLarge, color = SystemColors.text)
                TitlePicker(s.title, s.titles, vm::chooseTitle)
                Spacer(Modifier.height(8.dp))
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("NIVEL", color = SystemColors.muted, style = MaterialTheme.typography.labelLarge)
                        Text("${s.level}", style = MaterialTheme.typography.headlineLarge, color = accent)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("RANGO", color = SystemColors.muted, style = MaterialTheme.typography.labelLarge)
                        Text(s.rank.name, style = MaterialTheme.typography.headlineLarge, color = accent)
                    }
                }
                Spacer(Modifier.height(8.dp))
                StatBar("XP", s.xp.toFloat(), s.xpToNext.toFloat())
                Spacer(Modifier.height(8.dp))
                InfoRow("Racha", "${s.streak} días")
            }
        }
        item {
            SystemWindow("Atributos") {
                if (s.freePoints > 0) {
                    Text("Puntos disponibles: ${s.freePoints}", color = SystemColors.gold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                }
                Stat.entries.forEach { stat ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stat.name, color = accent, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(48.dp))
                        Text(stat.label, color = SystemColors.muted, modifier = Modifier.weight(1f))
                        Text("${s.stats.displayed(stat)}", style = MaterialTheme.typography.titleLarge, color = SystemColors.text)
                        if (s.freePoints > 0) {
                            IconButton(onClick = { vm.spend(stat) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Sumar a ${stat.label}", tint = SystemColors.gold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (detail) {
        Dialog(onDismissRequest = { detail = false }) { BodyDetailWindow(s) { detail = false } }
    }
}

@Composable
private fun TitlePicker(current: String?, titles: List<String>, onChoose: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, enabled = titles.isNotEmpty()) {
            Text(current?.let { "«$it»" } ?: if (titles.isEmpty()) "Sin título" else "Elegir título", color = SystemColors.gold)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Sin título") }, onClick = { onChoose(null); open = false })
            titles.forEach { t -> DropdownMenuItem(text = { Text(t) }, onClick = { onChoose(t); open = false }) }
        }
    }
}

@Composable
private fun PenaltyBanner(deadline: Instant?) {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = Instant.now()
        }
    }
    SystemWindow("Zona de Penalización", accent = SystemColors.danger) {
        Text(
            "Completa la misión de penalización. Si no lo haces, en cada cierre perderás XP y stats, y desde el tercero, niveles.",
            color = SystemColors.text,
        )
        deadline?.let {
            val left = Duration.between(now, it).coerceAtLeast(Duration.ZERO)
            Spacer(Modifier.height(8.dp))
            Text(
                "Próxima degradación en %02d:%02d:%02d".format(left.seconds / 3600, left.seconds % 3600 / 60, left.seconds % 60),
                color = SystemColors.danger,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BodyDetailWindow(s: StatusUi, onClose: () -> Unit) {
    SystemWindow("Condición física", onClose = onClose) {
        Box(Modifier.fillMaxWidth().height(260.dp)) {
            ShadowAvatar(s.shape, AvatarMode.NORMAL, s.rank, Modifier.fillMaxWidth().height(260.dp), ghost = s.ghost)
        }
        if (s.ghost != null) Text("Línea discontinua: tu silueta del primer registro.", color = SystemColors.muted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        InfoRow("Peso", s.weightKg?.let { "%.1f kg".format(Locale("es"), it) } ?: "—")
        InfoRow("IMC", s.bmi?.let { "%.1f".format(Locale("es"), it) } ?: "—")
        InfoRow(if (s.bodyFatEstimated) "% de grasa (estimado)" else "% de grasa", s.bodyFatPct?.let { "%.1f %%".format(Locale("es"), it) } ?: "—")
        InfoRow(
            "Desde el primer registro",
            s.weightDelta?.let { "%+.1f kg".format(Locale("es"), it) } ?: "—",
            valueColor = when {
                s.weightDelta == null -> SystemColors.text
                s.weightDelta < 0 -> SystemColors.success
                else -> SystemColors.text
            },
        )
        Spacer(Modifier.width(4.dp))
    }
}
